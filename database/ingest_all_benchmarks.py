import asyncio
import datetime
import hashlib
import io
import json
import math
import sqlite3
import httpx

# Try parquet readers
try:
    import duckdb
except ImportError:
    duckdb = None

try:
    import pyarrow.parquet as pq
except ImportError:
    pq = None

from inferra_data import (
    InferraConfig,
    MasterDatabase,
    IdentityResolutionEngine,
    BenchmarkEntity,
    BenchmarkVersionEntity,
    BenchmarkResultEntity,
    ProvenanceRecord,
    MeasurementType,
    VerificationState,
    NormalizationEngine,
    OrganizationEntity,
    ModelFamilyEntity,
    CanonicalModelEntity,
    ModelType,
)

async def main():
    cfg = InferraConfig.from_env()
    db = MasterDatabase(cfg.master_db_path)
    
    print('=== INFERRA COMPLETE BENCHMARK DUMP INGESTION ===')

    # 1. Register benchmark definitions
    benchmarks_def = [
        ('mmlu_pro', 'MMLU-Pro', 'REASONING', 'Accuracy', 0.0, 100.0),
        ('gpqa', 'GPQA Diamond', 'SCIENCE_REASONING', 'Accuracy', 0.0, 100.0),
        ('math_l5', 'MATH Level 5', 'MATHEMATICS', 'Accuracy', 0.0, 100.0),
        ('ifeval', 'IFEval', 'INSTRUCTION_FOLLOWING', 'Strict Accuracy', 0.0, 100.0),
        ('musr', 'MuSR', 'NARRATIVE_REASONING', 'Accuracy', 0.0, 100.0),
        ('bbh', 'BIG-Bench Hard', 'REASONING', 'Accuracy', 0.0, 100.0),
    ]
    for b_id, b_name, b_dom, b_met, min_s, max_s in benchmarks_def:
        db.upsert_benchmark(
            BenchmarkEntity(b_id, b_name, b_dom, f'Standard benchmark {b_name}'),
            BenchmarkVersionEntity(f'{b_id}_v1', b_id, 'v1.0', b_met, min_s, max_s)
        )

    rows = []
    
    async with httpx.AsyncClient(timeout=60.0, follow_redirects=True) as client:
        # 2. Dynamically discover the full Parquet dump file in open-llm-leaderboard/contents
        print('Discovering full Open LLM Leaderboard v2 dump file from repository tree...')
        tree_url = 'https://huggingface.co/api/datasets/open-llm-leaderboard/contents/tree/main/data'
        parquet_filename = None
        try:
            tree_resp = await client.get(tree_url)
            if tree_resp.status_code == 200:
                for item in tree_resp.json():
                    if item.get('path', '').endswith('.parquet'):
                        parquet_filename = item['path']
                        break
        except Exception as e:
            print(f'Tree discovery error: {e}')

        if parquet_filename:
            parquet_download_url = f'https://huggingface.co/datasets/open-llm-leaderboard/contents/resolve/main/{parquet_filename}'
            print(f'Downloading entire leaderboard dataset: {parquet_download_url} (~1.1 MB)...')
            parquet_cache = cfg.cache_dir / 'full_open_llm_leaderboard.parquet'
            
            resp = await client.get(parquet_download_url)
            if resp.status_code == 200:
                with open(parquet_cache, 'wb') as f:
                    f.write(resp.content)
                print(f'Download complete. Parsing parquet using analytical engine...')

                if duckdb is not None:
                    con = duckdb.connect()
                    df = con.execute('SELECT * FROM parquet_scan(?)', [str(parquet_cache)]).df()
                    rows = df.to_dict(orient='records')
                elif pq is not None:
                    table = pq.read_table(str(parquet_cache))
                    rows = table.to_pylist()
                print(f'Successfully loaded {len(rows)} models from Parquet!')

        # 3. Fallback to API paginated collection if parquet parsers are not installed
        if not rows:
            print('Falling back to high-throughput pagination via Hugging Face API...')
            api_url = 'https://datasets-server.huggingface.co/rows'
            offset = 0
            limit = 100
            while True:
                params = {'dataset': 'open-llm-leaderboard/contents', 'config': 'default', 'split': 'train', 'offset': str(offset), 'limit': str(limit)}
                resp = await client.get(api_url, params=params)
                if resp.status_code != 200:
                    break
                batch = resp.json().get('rows', [])
                if not batch:
                    break
                rows.extend([r['row'] for r in batch])
                print(f'Ingested {len(rows)} models from API...')
                offset += limit
                if offset >= 5000:
                    break

    now = datetime.datetime.now(datetime.timezone.utc).isoformat()
    inserted_results = 0

    print(f'Beginning database insertion of all benchmark records for {len(rows)} models...')

    # Use raw transactions for high-speed bulk inserts
    db.conn.execute('BEGIN TRANSACTION;')

    for r in rows:
        m_name = r.get('fullname') or r.get('eval_name') or r.get('Model')
        if not m_name or not isinstance(m_name, str):
            continue

        c_id, fam_id, m_type, _, _ = IdentityResolutionEngine.resolve_huggingface_id(m_name)
        org_id = c_id.split('/')[0]

        # Ensure model stub exists
        db.conn.execute(
            'INSERT OR IGNORE INTO organizations (org_id, name) VALUES (?, ?)',
            (org_id, org_id.capitalize())
        )
        db.conn.execute(
            'INSERT OR IGNORE INTO model_families (family_id, org_id, name) VALUES (?, ?, ?)',
            (fam_id, org_id, fam_id)
        )
        db.conn.execute(
            '''INSERT INTO canonical_models (canonical_id, family_id, org_id, display_name, model_type, is_open_weights, updated_at)
               VALUES (?, ?, ?, ?, ?, 1, ?)
               ON CONFLICT(canonical_id) DO UPDATE SET updated_at=excluded.updated_at''',
            (c_id, fam_id, org_id, m_name.split('/')[-1], m_type.value, now)
        )

        mapping = [
            ('mmlu_pro', r.get('MMLU-PRO')),
            ('gpqa', r.get('GPQA')),
            ('math_l5', r.get('MATH Lvl 5')),
            ('ifeval', r.get('IFEval')),
            ('musr', r.get('MUSR')),
            ('bbh', r.get('BBH')),
        ]

        for b_id, score_raw in mapping:
            if score_raw is None:
                continue
            try:
                score_f = float(score_raw)
                if math.isnan(score_f) or math.isinf(score_f):
                    continue
            except (ValueError, TypeError):
                continue

            p_hash = hashlib.sha256(f'open_llm_leaderboard:{m_name}:{b_id}'.encode()).hexdigest()
            
            db.conn.execute(
                '''INSERT OR IGNORE INTO provenance (
                    provenance_hash, source_id, publisher, source_uri, source_version, retrieved_at, raw_payload_checksum
                ) VALUES (?, ?, ?, ?, ?, ?, ?)''',
                (p_hash, 'open_llm_leaderboard', 'Hugging Face / EleutherAI', 'https://huggingface.co/open-llm-leaderboard', 'v2', now, p_hash[:16])
            )

            norm_s = NormalizationEngine.normalize_score(score_f)
            db.conn.execute(
                '''INSERT INTO benchmark_results (
                    result_id, canonical_id, version_id, raw_score, score_normalized,
                    measurement_type, source_id, retrieved_at, provenance_hash, verification_state
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(result_id) DO UPDATE SET
                    raw_score=excluded.raw_score,
                    score_normalized=excluded.score_normalized''',
                (
                    f'res_{p_hash[:16]}',
                    c_id,
                    f'{b_id}_v1',
                    score_f,
                    norm_s,
                    MeasurementType.LEADERBOARD_MEASURED.value,
                    'open_llm_leaderboard',
                    now,
                    p_hash,
                    VerificationState.VERIFIED.value
                )
            )
            inserted_results += 1

    db.conn.commit()
    print(f'COMPLETE: Committed {inserted_results} benchmark evaluations into inferra_master.sqlite3 across {len(rows)} models!')

if __name__ == '__main__':
    asyncio.run(main())
