from __future__ import annotations

import argparse
import datetime
import hashlib
import io
import json
import logging
import math
import os
import platform
import queue
import re
import shutil
import sqlite3
import sys
import threading
import time
import urllib.parse
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any, Dict, Generator, List, Optional, Tuple

try:
    import requests
    from requests.adapters import HTTPAdapter
    from urllib3.util.retry import Retry
except ImportError:
    requests = None

try:
    import yaml
except ImportError:
    yaml = None

try:
    from bs4 import BeautifulSoup
except ImportError:
    BeautifulSoup = None

try:
    from PIL import Image as PILImage
except ImportError:
    PILImage = None

try:
    import pytesseract
except ImportError:
    pytesseract = None

try:
    import pypdf
except ImportError:
    pypdf = None

DEFAULT_BASE_DIR = r"F:\InferraData"
FALLBACK_BASE_DIR = r".\InferraData"

SCHEMA_VERSION = 1
MAX_HTTP_RETRIES = 5
HTTP_TIMEOUT = 25
BACKOFF_FACTOR = 1.5

DISK_WARN_PERCENT = 20.0
DISK_AGGRESSIVE_WARN_PERCENT = 10.0
DISK_SAFE_MODE_PERCENT = 5.0
DISK_EMERGENCY_STOP_PERCENT = 1.5

BENCHMARK_CATALOG = {
    "mmlu": {
        "canonical": "MMLU",
        "domain": "General Knowledge",
        "default_metric": "accuracy",
        "patterns": [r"\bmmlu\b", r"massive multitask language understanding"],
    },
    "mmlu_pro": {
        "canonical": "MMLU-Pro",
        "domain": "Reasoning",
        "default_metric": "accuracy",
        "patterns": [r"\bmmlu[-_ ]pro\b"],
    },
    "gsm8k": {
        "canonical": "GSM8K",
        "domain": "Math",
        "default_metric": "accuracy",
        "patterns": [r"\bgsm8k\b", r"grade school math 8k"],
    },
    "math": {
        "canonical": "MATH",
        "domain": "Math",
        "default_metric": "accuracy",
        "patterns": [r"\bmath\b", r"hendrycks math"],
    },
    "humaneval": {
        "canonical": "HumanEval",
        "domain": "Code",
        "default_metric": "pass@1",
        "patterns": [r"\bhumaneval\b", r"human-eval"],
    },
    "humaneval_plus": {
        "canonical": "HumanEval+",
        "domain": "Code",
        "default_metric": "pass@1",
        "patterns": [r"\bhumaneval\+\b", r"humaneval[-_ ]plus"],
    },
    "mbpp": {
        "canonical": "MBPP",
        "domain": "Code",
        "default_metric": "pass@1",
        "patterns": [r"\bmbpp\b"],
    },
    "mbpp_plus": {
        "canonical": "MBPP+",
        "domain": "Code",
        "default_metric": "pass@1",
        "patterns": [r"\bmbpp\+\b", r"mbpp[-_ ]plus"],
    },
    "swe_bench": {
        "canonical": "SWE-bench",
        "domain": "Agentic Coding",
        "default_metric": "resolved_rate",
        "patterns": [r"\bswe[-_ ]bench\b(?!.*verified)"],
    },
    "swe_bench_verified": {
        "canonical": "SWE-bench Verified",
        "domain": "Agentic Coding",
        "default_metric": "resolved_rate",
        "patterns": [r"\bswe[-_ ]bench[-_ ]verified\b"],
    },
    "gpqa": {
        "canonical": "GPQA",
        "domain": "Graduate Reasoning",
        "default_metric": "accuracy",
        "patterns": [r"\bgpqa\b(?!.*diamond)"],
    },
    "gpqa_diamond": {
        "canonical": "GPQA Diamond",
        "domain": "Graduate Reasoning",
        "default_metric": "accuracy",
        "patterns": [r"\bgpqa[-_ ]diamond\b"],
    },
    "arc_challenge": {
        "canonical": "ARC Challenge",
        "domain": "Reasoning",
        "default_metric": "accuracy",
        "patterns": [r"\barc[-_ ]c\b", r"arc[-_ ]challenge"],
    },
    "hellaswag": {
        "canonical": "HellaSwag",
        "domain": "Common Sense",
        "default_metric": "accuracy",
        "patterns": [r"\bhellaswag\b"],
    },
    "winogrande": {
        "canonical": "WinoGrande",
        "domain": "Common Sense",
        "default_metric": "accuracy",
        "patterns": [r"\bwinogrande\b"],
    },
    "ifeval": {
        "canonical": "IFEval",
        "domain": "Instruction Following",
        "default_metric": "accuracy",
        "patterns": [r"\bifeval\b"],
    },
    "arena_elo": {
        "canonical": "Chatbot Arena Elo",
        "domain": "Human Preference",
        "default_metric": "elo",
        "patterns": [r"\barena[-_ ]elo\b", r"lmsys elo"],
    },
}

MODEL_FAMILY_RULES = [
    (r"\b(llama[-_ ]?4)\b", "Llama 4"),
    (r"\b(llama[-_ ]?3\.3)\b", "Llama 3.3"),
    (r"\b(llama[-_ ]?3\.2)\b", "Llama 3.2"),
    (r"\b(llama[-_ ]?3\.1)\b", "Llama 3.1"),
    (r"\b(llama[-_ ]?3)\b", "Llama 3"),
    (r"\b(llama[-_ ]?2)\b", "Llama 2"),
    (r"\b(llama)\b", "Llama"),
    (r"\b(qwen[-_ ]?3)\b", "Qwen3"),
    (r"\b(qwen[-_ ]?2\.5)\b", "Qwen2.5"),
    (r"\b(qwen[-_ ]?2)\b", "Qwen2"),
    (r"\b(qwen[-_ ]?1\.5)\b", "Qwen1.5"),
    (r"\b(qwen)\b", "Qwen"),
    (r"\b(deepseek[-_ ]?r1)\b", "DeepSeek R1"),
    (r"\b(deepseek[-_ ]?v3)\b", "DeepSeek V3"),
    (r"\b(deepseek[-_ ]?v2(?:\.5)?)\b", "DeepSeek V2"),
    (r"\b(deepseek)\b", "DeepSeek"),
    (r"\b(mistral)\b", "Mistral"),
    (r"\b(mixtral)\b", "Mixtral"),
    (r"\b(gemma[-_ ]?2)\b", "Gemma 2"),
    (r"\b(gemma)\b", "Gemma"),
    (r"\b(phi[-_ ]?4)\b", "Phi-4"),
    (r"\b(phi[-_ ]?3(?:\.5)?)\b", "Phi-3"),
    (r"\b(phi[-_ ]?[12])\b", "Phi"),
    (r"\b(claude[-_ ]?3\.5)\b", "Claude 3.5"),
    (r"\b(claude[-_ ]?3)\b", "Claude 3"),
    (r"\b(gpt[-_ ]?4o)\b", "GPT-4o"),
    (r"\b(gpt[-_ ]?4)\b", "GPT-4"),
]

@dataclass
class SourceRecord:
    source_type: str
    url: str
    repo_id: Optional[str] = None
    commit_sha: Optional[str] = None
    http_status: Optional[int] = None
    headers_json: Optional[str] = None
    fetched_at: str = field(default_factory=lambda: datetime.datetime.now(datetime.timezone.utc).isoformat())

@dataclass
class ArtifactRecord:
    rel_path: str
    file_type: str
    sha256: str
    size_bytes: int
    phash: Optional[str] = None
    created_at: str = field(default_factory=lambda: datetime.datetime.now(datetime.timezone.utc).isoformat())

@dataclass
class EvidenceRecord:
    extraction_method: str
    location_info: str
    raw_snippet: str
    confidence_score: float

@dataclass
class BenchmarkCandidate:
    raw_model_name: str
    raw_benchmark_name: str
    raw_value: str
    numeric_value: float
    normalized_value: float
    metric: str
    unit: str
    shots: Optional[int] = None
    split: Optional[str] = None
    subset: Optional[str] = None
    language: Optional[str] = None
    reported_date: Optional[str] = None
    confidence_tier: str = "F"
    extraction_method: str = "PROSE"
    location_info: str = ""
    raw_snippet: str = ""

class StorageModule:
    class DatabaseEngine:
        def __init__(self, db_path: Path, logger: logging.Logger):
            self.db_path = Path(db_path)
            self.logger = logger
            self.db_path.parent.mkdir(parents=True, exist_ok=True)
            self._init_db()

        def get_connection(self) -> sqlite3.Connection:
            conn = sqlite3.connect(str(self.db_path), timeout=60.0)
            conn.execute("PRAGMA journal_mode = WAL;")
            conn.execute("PRAGMA synchronous = NORMAL;")
            conn.execute("PRAGMA foreign_keys = ON;")
            conn.execute("PRAGMA busy_timeout = 60000;")
            conn.row_factory = sqlite3.Row
            return conn

        def _init_db(self):
            conn = self.get_connection()
            with conn:
                conn.executescript("""
                CREATE TABLE IF NOT EXISTS meta_schema (
                    version INTEGER PRIMARY KEY,
                    applied_at TEXT NOT NULL
                );

                CREATE TABLE IF NOT EXISTS crawl_state (
                    source_key TEXT PRIMARY KEY,
                    state TEXT NOT NULL,
                    cursor TEXT,
                    retry_count INTEGER DEFAULT 0,
                    last_error TEXT,
                    updated_at TEXT NOT NULL
                );

                CREATE TABLE IF NOT EXISTS sources (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    source_type TEXT NOT NULL,
                    url TEXT NOT NULL UNIQUE,
                    repo_id TEXT,
                    commit_sha TEXT,
                    http_status INTEGER,
                    headers_json TEXT,
                    fetched_at TEXT NOT NULL
                );

                CREATE TABLE IF NOT EXISTS artifacts (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    source_id INTEGER NOT NULL REFERENCES sources(id) ON DELETE CASCADE,
                    rel_path TEXT NOT NULL UNIQUE,
                    file_type TEXT NOT NULL,
                    sha256 TEXT NOT NULL,
                    phash TEXT,
                    size_bytes INTEGER NOT NULL,
                    created_at TEXT NOT NULL
                );

                CREATE TABLE IF NOT EXISTS model_families (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    canonical_name TEXT NOT NULL UNIQUE,
                    description TEXT
                );

                CREATE TABLE IF NOT EXISTS organizations (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL UNIQUE,
                    url TEXT
                );

                CREATE TABLE IF NOT EXISTS models (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    family_id INTEGER REFERENCES model_families(id),
                    org_id INTEGER REFERENCES organizations(id),
                    canonical_name TEXT NOT NULL UNIQUE,
                    raw_name TEXT NOT NULL,
                    param_size TEXT,
                    quantization TEXT,
                    variant TEXT,
                    created_at TEXT NOT NULL
                );

                CREATE TABLE IF NOT EXISTS model_aliases (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    model_id INTEGER NOT NULL REFERENCES models(id) ON DELETE CASCADE,
                    alias TEXT NOT NULL UNIQUE,
                    source TEXT
                );

                CREATE TABLE IF NOT EXISTS benchmarks (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    canonical_name TEXT NOT NULL UNIQUE,
                    domain TEXT,
                    default_metric TEXT
                );

                CREATE TABLE IF NOT EXISTS benchmark_variants (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    benchmark_id INTEGER NOT NULL REFERENCES benchmarks(id) ON DELETE CASCADE,
                    variant_name TEXT NOT NULL,
                    shots INTEGER,
                    split TEXT,
                    subset TEXT,
                    language TEXT,
                    UNIQUE(benchmark_id, variant_name, shots, split, subset, language)
                );

                CREATE TABLE IF NOT EXISTS evidence (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    artifact_id INTEGER NOT NULL REFERENCES artifacts(id) ON DELETE CASCADE,
                    extraction_method TEXT NOT NULL,
                    location_info TEXT,
                    raw_snippet TEXT,
                    confidence_score REAL NOT NULL
                );

                CREATE TABLE IF NOT EXISTS results (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    model_id INTEGER NOT NULL REFERENCES models(id),
                    benchmark_id INTEGER NOT NULL REFERENCES benchmarks(id),
                    benchmark_variant_id INTEGER NOT NULL REFERENCES benchmark_variants(id),
                    raw_value TEXT NOT NULL,
                    numeric_value REAL NOT NULL,
                    normalized_value REAL NOT NULL,
                    metric TEXT NOT NULL,
                    unit TEXT NOT NULL,
                    reported_date TEXT,
                    confidence_tier TEXT NOT NULL,
                    is_conflict INTEGER DEFAULT 0,
                    created_at TEXT NOT NULL
                );

                CREATE TABLE IF NOT EXISTS result_evidence (
                    result_id INTEGER NOT NULL REFERENCES results(id) ON DELETE CASCADE,
                    evidence_id INTEGER NOT NULL REFERENCES evidence(id) ON DELETE CASCADE,
                    PRIMARY KEY (result_id, evidence_id)
                );

                CREATE TABLE IF NOT EXISTS conflicts (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    model_id INTEGER NOT NULL REFERENCES models(id),
                    benchmark_variant_id INTEGER NOT NULL REFERENCES benchmark_variants(id),
                    existing_result_id INTEGER NOT NULL REFERENCES results(id),
                    conflicting_result_id INTEGER NOT NULL REFERENCES results(id),
                    difference_magnitude REAL NOT NULL,
                    detected_at TEXT NOT NULL
                );

                CREATE INDEX IF NOT EXISTS idx_artifacts_sha256 ON artifacts(sha256);
                CREATE INDEX IF NOT EXISTS idx_artifacts_phash ON artifacts(phash);
                CREATE INDEX IF NOT EXISTS idx_models_canonical ON models(canonical_name);
                CREATE INDEX IF NOT EXISTS idx_benchmarks_canonical ON benchmarks(canonical_name);
                CREATE INDEX IF NOT EXISTS idx_results_lookup ON results(model_id, benchmark_variant_id);
                CREATE INDEX IF NOT EXISTS idx_crawl_state_state ON crawl_state(state);
                """)

                res = conn.execute("SELECT version FROM meta_schema WHERE version = ?", (SCHEMA_VERSION,)).fetchone()
                if not res:
                    conn.execute(
                        "INSERT INTO meta_schema (version, applied_at) VALUES (?, ?)",
                        (SCHEMA_VERSION, datetime.datetime.now(datetime.timezone.utc).isoformat()),
                    )
            conn.close()

        def run_integrity_check(self) -> List[str]:
            conn = self.get_connection()
            issues = []
            try:
                cursor = conn.execute("PRAGMA integrity_check;")
                rows = cursor.fetchall()
                for r in rows:
                    if r[0] != "ok":
                        issues.append(f"SQLite Integrity: {r[0]}")

                cursor = conn.execute("PRAGMA foreign_key_check;")
                fk_rows = cursor.fetchall()
                for fk in fk_rows:
                    issues.append(f"Foreign Key Violation in table {fk[0]} rowid {fk[1]}")
            finally:
                conn.close()
            return issues

    class ArtifactManager:
        def __init__(self, base_dir: Path):
            self.base_dir = base_dir
            self.raw_dir = base_dir / "raw"
            self._ensure_layout()

        def _ensure_layout(self):
            for sub in ["huggingface", "web", "papers", "images", "html", "markdown", "pdf"]:
                (self.raw_dir / sub).mkdir(parents=True, exist_ok=True)

        def save_bytes(self, data: bytes, subfolder: str, filename: str) -> Tuple[str, str, int]:
            sha256 = hashlib.sha256(data).hexdigest()
            dest_folder = self.raw_dir / subfolder
            dest_folder.mkdir(parents=True, exist_ok=True)

            clean_fn = re.sub(r'[<>:"/\\|?*]', "_", filename)
            file_path = dest_folder / f"{sha256[:12]}_{clean_fn}"

            if not file_path.exists():
                file_path.write_bytes(data)

            rel_path = str(file_path.relative_to(self.base_dir))
            return rel_path, sha256, len(data)

        def compute_phash(self, image_data: bytes) -> Optional[str]:
            if PILImage is None:
                return None
            try:
                with PILImage.open(io.BytesIO(image_data)) as img:
                    img = img.convert("L").resize((8, 8), PILImage.Resampling.LANCZOS)
                    pixels = list(img.getdata())
                    avg = sum(pixels) / len(pixels)
                    bits = "".join("1" if p >= avg else "0" for p in pixels)
                    return f"{int(bits, 2):016x}"
            except Exception:
                return None

    class DiskMonitor:
        def __init__(self, target_path: str):
            self.target_path = Path(target_path)
            self.target_path.mkdir(parents=True, exist_ok=True)

        def get_free_space_gb(self) -> float:
            total, used, free = shutil.disk_usage(self.target_path)
            return free / (1024**3)

        def get_free_percent(self) -> float:
            total, used, free = shutil.disk_usage(self.target_path)
            if total == 0:
                return 0.0
            return (free / total) * 100.0

        def check_safety_threshold(self) -> str:
            free_pct = self.get_free_percent()
            if free_pct <= DISK_EMERGENCY_STOP_PERCENT:
                return "EMERGENCY_STOP"
            if free_pct <= DISK_SAFE_MODE_PERCENT:
                return "SAFE_MODE"
            if free_pct <= DISK_AGGRESSIVE_WARN_PERCENT:
                return "AGGRESSIVE_WARN"
            if free_pct <= DISK_WARN_PERCENT:
                return "WARN"
            return "OK"

    class ProvenanceConflictEngine:
        def __init__(self, db_engine: StorageModule.DatabaseEngine, logger: logging.Logger):
            self.db = db_engine
            self.logger = logger
            self.benchmark_cache: Dict[str, int] = {}
            self.variant_cache: Dict[Tuple, int] = {}
            self.family_cache: Dict[str, int] = {}
            self.org_cache: Dict[str, int] = {}
            self.models_cache: Dict[str, int] = {}

        def bootstrap_cache(self, conn: sqlite3.Connection):
            for meta in BENCHMARK_CATALOG.values():
                conn.execute(
                    "INSERT INTO benchmarks (canonical_name, domain, default_metric) VALUES (?, ?, ?) ON CONFLICT(canonical_name) DO NOTHING",
                    (meta["canonical"], meta["domain"], meta["default_metric"]),
                )
            conn.commit()

            for row in conn.execute("SELECT id, canonical_name FROM benchmarks"):
                self.benchmark_cache[row[1]] = row[0]

            for row in conn.execute(
                "SELECT id, benchmark_id, variant_name, shots, split, subset, language FROM benchmark_variants"
            ):
                self.variant_cache[(row[1], row[2], row[3], row[4], row[5], row[6])] = row[0]

            for row in conn.execute("SELECT id, canonical_name FROM model_families"):
                self.family_cache[row[1]] = row[0]

            for row in conn.execute("SELECT id, name FROM organizations"):
                self.org_cache[row[1]] = row[0]

            for row in conn.execute("SELECT id, canonical_name FROM models"):
                self.models_cache[row[1]] = row[0]

        def store_pipeline_batch(
            self,
            conn: sqlite3.Connection,
            source: SourceRecord,
            artifacts: List[Tuple[ArtifactRecord, List[Tuple[EvidenceRecord, BenchmarkCandidate]]]],
        ) -> Dict[str, int]:
            stats = {
                "models_new": 0,
                "families_new": 0,
                "benchmarks_new": 0,
                "results_new": 0,
                "evidence_new": 0,
                "conflicts_new": 0,
            }

            cursor = conn.execute(
                """
                INSERT INTO sources (source_type, url, repo_id, commit_sha, http_status, headers_json, fetched_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(url) DO UPDATE SET
                    commit_sha = excluded.commit_sha,
                    http_status = excluded.http_status,
                    fetched_at = excluded.fetched_at
                RETURNING id
            """,
                (
                    source.source_type,
                    source.url,
                    source.repo_id,
                    source.commit_sha,
                    source.http_status,
                    source.headers_json,
                    source.fetched_at,
                ),
            )
            source_id = cursor.fetchone()[0]

            for artifact_rec, eval_pairs in artifacts:
                cursor = conn.execute(
                    """
                    INSERT INTO artifacts (source_id, rel_path, file_type, sha256, phash, size_bytes, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT(rel_path) DO UPDATE SET
                        sha256 = excluded.sha256,
                        phash = excluded.phash
                    RETURNING id
                """,
                    (
                        source_id,
                        artifact_rec.rel_path,
                        artifact_rec.file_type,
                        artifact_rec.sha256,
                        artifact_rec.phash,
                        artifact_rec.size_bytes,
                        artifact_rec.created_at,
                    ),
                )
                artifact_id = cursor.fetchone()[0]

                for evidence_rec, candidate in eval_pairs:
                    raw_model = candidate.raw_model_name or source.repo_id or "Unknown"
                    model_meta = ExtractorsModule.NormalizationEngine.normalize_model_identity(raw_model)

                    fam_name = model_meta["family"]
                    if fam_name not in self.family_cache:
                        conn.execute(
                            "INSERT INTO model_families (canonical_name) VALUES (?) ON CONFLICT(canonical_name) DO NOTHING",
                            (fam_name,),
                        )
                        fam_row = conn.execute(
                            "SELECT id FROM model_families WHERE canonical_name = ?", (fam_name,)
                        ).fetchone()
                        self.family_cache[fam_name] = fam_row[0]
                        stats["families_new"] += 1
                    family_id = self.family_cache[fam_name]

                    org_name = model_meta["org"]
                    if org_name not in self.org_cache:
                        conn.execute(
                            "INSERT INTO organizations (name) VALUES (?) ON CONFLICT(name) DO NOTHING",
                            (org_name,),
                        )
                        org_row = conn.execute(
                            "SELECT id FROM organizations WHERE name = ?", (org_name,)
                        ).fetchone()
                        self.org_cache[org_name] = org_row[0]
                    org_id = self.org_cache[org_name]

                    can_model = model_meta["canonical_name"]
                    if can_model not in self.models_cache:
                        cursor = conn.execute(
                            """
                            INSERT INTO models (family_id, org_id, canonical_name, raw_name, param_size, quantization, variant, created_at)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                            ON CONFLICT(canonical_name) DO UPDATE SET
                                param_size = excluded.param_size,
                                quantization = excluded.quantization,
                                variant = excluded.variant
                            RETURNING id
                        """,
                            (
                                family_id,
                                org_id,
                                can_model,
                                model_meta["raw_name"],
                                model_meta["param_size"],
                                model_meta["quantization"],
                                model_meta["variant"],
                                datetime.datetime.now(datetime.timezone.utc).isoformat(),
                            ),
                        )
                        model_id = cursor.fetchone()[0]
                        self.models_cache[can_model] = model_id
                        stats["models_new"] += 1
                    else:
                        model_id = self.models_cache[can_model]

                    bench_match = ExtractorsModule.NormalizationEngine.match_benchmark(candidate.raw_benchmark_name)
                    canonical_bench_name = (
                        bench_match["canonical_name"] if bench_match else candidate.raw_benchmark_name
                    )
                    domain = bench_match["domain"] if bench_match else "General"
                    default_metric = bench_match["default_metric"] if bench_match else candidate.metric

                    if canonical_bench_name not in self.benchmark_cache:
                        conn.execute(
                            """
                            INSERT INTO benchmarks (canonical_name, domain, default_metric)
                            VALUES (?, ?, ?)
                            ON CONFLICT(canonical_name) DO NOTHING
                        """,
                            (canonical_bench_name, domain, default_metric),
                        )
                        b_row = conn.execute(
                            "SELECT id FROM benchmarks WHERE canonical_name = ?", (canonical_bench_name,)
                        ).fetchone()
                        self.benchmark_cache[canonical_bench_name] = b_row[0]
                        stats["benchmarks_new"] += 1
                    bench_id = self.benchmark_cache[canonical_bench_name]

                    variant_name = bench_match["variant_name"] if bench_match else "default"
                    v_key = (
                        bench_id,
                        variant_name,
                        candidate.shots,
                        candidate.split,
                        candidate.subset,
                        candidate.language,
                    )

                    if v_key not in self.variant_cache:
                        cursor = conn.execute(
                            """
                            INSERT INTO benchmark_variants (benchmark_id, variant_name, shots, split, subset, language)
                            VALUES (?, ?, ?, ?, ?, ?)
                            ON CONFLICT(benchmark_id, variant_name, shots, split, subset, language) DO NOTHING
                            RETURNING id
                        """,
                            (
                                bench_id,
                                variant_name,
                                candidate.shots,
                                candidate.split,
                                candidate.subset,
                                candidate.language,
                            ),
                        )
                        bv_row = cursor.fetchone()
                        if bv_row:
                            bv_id = bv_row[0]
                        else:
                            bv_row = conn.execute(
                                """
                                SELECT id FROM benchmark_variants
                                WHERE benchmark_id = ? AND variant_name = ?
                                  AND shots IS ? AND split IS ? AND subset IS ? AND language IS ?
                            """,
                                (
                                    bench_id,
                                    variant_name,
                                    candidate.shots,
                                    candidate.split,
                                    candidate.subset,
                                    candidate.language,
                                ),
                            ).fetchone()
                            bv_id = bv_row[0]
                        self.variant_cache[v_key] = bv_id
                    bv_id = self.variant_cache[v_key]

                    cursor = conn.execute(
                        """
                        INSERT INTO evidence (artifact_id, extraction_method, location_info, raw_snippet, confidence_score)
                        VALUES (?, ?, ?, ?, ?) RETURNING id
                    """,
                        (
                            artifact_id,
                            evidence_rec.extraction_method,
                            evidence_rec.location_info,
                            evidence_rec.raw_snippet,
                            evidence_rec.confidence_score,
                        ),
                    )
                    evidence_id = cursor.fetchone()[0]
                    stats["evidence_new"] += 1

                    existing_res = conn.execute(
                        """
                        SELECT id, numeric_value, normalized_value FROM results
                        WHERE model_id = ? AND benchmark_variant_id = ?
                    """,
                        (model_id, bv_id),
                    ).fetchall()

                    is_conflict = 0
                    diff_from_consensus = 0.0
                    representative_id = None

                    if existing_res:
                        historical_scores = [r["normalized_value"] for r in existing_res]
                        consensus_mean = sum(historical_scores) / len(historical_scores)
                        diff_from_consensus = abs(consensus_mean - candidate.normalized_value)
                        representative_id = existing_res[0]["id"]

                        if diff_from_consensus > 0.05:
                            is_conflict = 1
                            conn.execute(
                                """
                                UPDATE results SET is_conflict = 1 
                                WHERE model_id = ? AND benchmark_variant_id = ?
                            """,
                                (model_id, bv_id),
                            )

                    cursor = conn.execute(
                        """
                        INSERT INTO results (model_id, benchmark_id, benchmark_variant_id, raw_value, numeric_value,
                                             normalized_value, metric, unit, reported_date, confidence_tier, is_conflict, created_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id
                    """,
                        (
                            model_id,
                            bench_id,
                            bv_id,
                            candidate.raw_value,
                            candidate.numeric_value,
                            candidate.normalized_value,
                            candidate.metric,
                            candidate.unit,
                            candidate.reported_date,
                            candidate.confidence_tier,
                            is_conflict,
                            datetime.datetime.now(datetime.timezone.utc).isoformat(),
                        ),
                    )
                    new_result_id = cursor.fetchone()[0]
                    stats["results_new"] += 1

                    conn.execute(
                        """
                        INSERT INTO result_evidence (result_id, evidence_id) VALUES (?, ?)
                    """,
                        (new_result_id, evidence_id),
                    )

                    if is_conflict and representative_id:
                        conn.execute(
                            """
                            INSERT INTO conflicts (model_id, benchmark_variant_id, existing_result_id, conflicting_result_id,
                                                   difference_magnitude, detected_at)
                            VALUES (?, ?, ?, ?, ?, ?)
                        """,
                            (
                                model_id,
                                bv_id,
                                representative_id,
                                new_result_id,
                                diff_from_consensus,
                                datetime.datetime.now(datetime.timezone.utc).isoformat(),
                            ),
                        )
                        stats["conflicts_new"] += 1

            return stats

class ExtractorsModule:
    class NormalizationEngine:
        @staticmethod
        def normalize_model_identity(raw_name: str, org_hint: Optional[str] = None) -> Dict[str, Any]:
            cleaned = raw_name.strip()
            if "/" in cleaned:
                parts = cleaned.split("/", 1)
                org = parts[0]
                model_sub = parts[1]
            else:
                org = org_hint or "Unknown"
                model_sub = cleaned

            family = "Independent"
            for pattern, fam_name in MODEL_FAMILY_RULES:
                if re.search(pattern, model_sub, re.IGNORECASE):
                    family = fam_name
                    break

            param_size = "Unknown"
            param_match = re.search(r"(\d+(?:\.\d+)?)[bB]\b", model_sub)
            if param_match:
                param_size = f"{param_match.group(1).upper()}B"

            quant = "Original"
            for q_token in ["GGUF", "AWQ", "GPTQ", "FP16", "BF16", "Q4_K_M", "Q8_0", "Q5_K_M", "EXL2", "INT4", "INT8"]:
                if re.search(rf"\b{q_token}\b", model_sub, re.IGNORECASE):
                    quant = q_token.upper()
                    break

            variant = "Base"
            for v_token in ["Instruct", "Chat", "Coder", "Math", "Vision", "RL", "DPO", "SFT"]:
                if re.search(rf"\b{v_token}\b", model_sub, re.IGNORECASE):
                    variant = v_token.capitalize()
                    break

            norm_name = re.sub(r"[^a-zA-Z0-9\._]+", "-", model_sub).strip("-")
            canonical_name = f"{org}/{norm_name}" if org != "Unknown" else norm_name

            return {
                "org": org,
                "raw_name": raw_name,
                "canonical_name": canonical_name,
                "family": family,
                "param_size": param_size,
                "quantization": quant,
                "variant": variant,
            }

        @staticmethod
        def match_benchmark(raw_bench_name: str) -> Optional[Dict[str, Any]]:
            cleaned = raw_bench_name.strip().lower()
            for b_id, meta in BENCHMARK_CATALOG.items():
                for pat in meta["patterns"]:
                    if re.search(pat, cleaned):
                        shots = None
                        shot_m = re.search(r"(\d+)[-_ ]?shots?", cleaned)
                        if shot_m:
                            shots = int(shot_m.group(1))

                        variant_name = "default"
                        if "cot" in cleaned:
                            variant_name = "cot"
                        elif "diamond" in cleaned:
                            variant_name = "diamond"
                        elif "verified" in cleaned:
                            variant_name = "verified"
                        elif "pro" in cleaned:
                            variant_name = "pro"

                        return {
                            "catalog_id": b_id,
                            "canonical_name": meta["canonical"],
                            "domain": meta["domain"],
                            "default_metric": meta["default_metric"],
                            "shots": shots,
                            "variant_name": variant_name,
                        }
            return None

        @staticmethod
        def validate_and_normalize_score(raw_val: str, metric: str) -> Optional[Tuple[float, float, str]]:
            cleaned = raw_val.strip().replace("%", "").replace(",", "").replace('"', "").replace("'", "")
            try:
                num = float(cleaned)
            except ValueError:
                return None

            if math.isnan(num) or math.isinf(num):
                return None

            normalized = num
            unit = "percent"

            if "elo" in metric.lower():
                if num < 200 or num > 3000:
                    return None
                unit = "elo"
            else:
                if 0.0 <= num <= 1.0 and num > 0:
                    normalized = round(num * 100.0, 4)
                elif 0.0 <= num <= 100.0:
                    normalized = round(num, 4)
                else:
                    return None

            return num, normalized, unit

    class ModelIndexYAMLExtractor:
        @staticmethod
        def extract(content: str) -> List[BenchmarkCandidate]:
            candidates = []
            match = re.search(r"^---\s*\n(.*?)\n---", content, re.DOTALL | re.MULTILINE)
            if not match:
                return candidates

            raw_yaml = match.group(1)
            data = None

            if yaml:
                try:
                    data = yaml.safe_load(raw_yaml)
                except Exception:
                    data = None

            if not isinstance(data, dict):
                if "model-index" in raw_yaml:
                    sub_pattern = re.findall(
                        r"name:\s*([^\n]+).*?type:\s*([^\n]+).*?value:\s*([0-9\.]+)", raw_yaml, re.DOTALL
                    )
                    for bench_raw, metric_raw, val_raw in sub_pattern:
                        norm = ExtractorsModule.NormalizationEngine.validate_and_normalize_score(val_raw, metric_raw)
                        if norm:
                            num, norm_val, unit = norm
                            candidates.append(
                                BenchmarkCandidate(
                                    raw_model_name="",
                                    raw_benchmark_name=bench_raw.strip(),
                                    raw_value=val_raw.strip(),
                                    numeric_value=num,
                                    normalized_value=norm_val,
                                    metric=metric_raw.strip(),
                                    unit=unit,
                                    confidence_tier="A+",
                                    extraction_method="STRUCTURED_HF_EVAL",
                                    location_info="Frontmatter regex",
                                    raw_snippet=f"{bench_raw}: {val_raw}",
                                )
                            )
                return candidates

            model_indices = data.get("model-index", [])
            if isinstance(model_indices, dict):
                model_indices = [model_indices]
            if not isinstance(model_indices, list):
                return candidates

            for entry in model_indices:
                if not isinstance(entry, dict):
                    continue
                model_name = entry.get("name", "")
                results = entry.get("results", [])
                if not isinstance(results, list):
                    continue
                for res_item in results:
                    if not isinstance(res_item, dict):
                        continue
                    task_name = res_item.get("task", {}).get("name", "")
                    dataset_name = res_item.get("dataset", {}).get("name", "")
                    metrics = res_item.get("metrics", [])
                    if not isinstance(metrics, list):
                        continue
                    for m in metrics:
                        if not isinstance(m, dict):
                            continue
                        m_type = m.get("type", "accuracy")
                        m_val = str(m.get("value", ""))
                        if not m_val:
                            continue
                        norm = ExtractorsModule.NormalizationEngine.validate_and_normalize_score(m_val, m_type)
                        if norm:
                            num, norm_val, unit = norm
                            bench_name = dataset_name or task_name or m.get("name", "Unknown")
                            candidates.append(
                                BenchmarkCandidate(
                                    raw_model_name=str(model_name),
                                    raw_benchmark_name=str(bench_name),
                                    raw_value=m_val,
                                    numeric_value=num,
                                    normalized_value=norm_val,
                                    metric=str(m_type),
                                    unit=unit,
                                    confidence_tier="A+",
                                    extraction_method="STRUCTURED_HF_EVAL",
                                    location_info="model-index YAML metadata",
                                    raw_snippet=json.dumps(m),
                                )
                            )
            return candidates

    class MarkdownTableExtractor:
        @staticmethod
        def extract(content: str) -> List[BenchmarkCandidate]:
            candidates = []
            lines = content.splitlines()
            table_buffer: List[str] = []
            in_table = False

            def process_table_buffer(buf: List[str]) -> List[BenchmarkCandidate]:
                sub_candidates = []
                if len(buf) < 3:
                    return sub_candidates

                def parse_line(l: str) -> List[str]:
                    return [c.strip() for c in l.strip().strip("|").split("|")]

                header = parse_line(buf[0])
                sep = parse_line(buf[1])
                if not all(re.match(r"^:?-+:?$", s) for s in sep if s):
                    return sub_candidates

                rows = [parse_line(r) for r in buf[2:]]
                model_col_idx = -1
                benchmark_col_idx = -1
                score_col_indices: List[Tuple[int, str]] = []

                for idx, h in enumerate(header):
                    h_low = h.lower()
                    if any(k in h_low for k in ["model", "checkpoint", "system"]):
                        model_col_idx = idx
                    elif any(k in h_low for k in ["benchmark", "dataset", "eval", "metric", "task"]):
                        benchmark_col_idx = idx
                    else:
                        bench_match = ExtractorsModule.NormalizationEngine.match_benchmark(h)
                        if bench_match:
                            score_col_indices.append((idx, bench_match["canonical_name"]))

                for row_idx, r in enumerate(rows):
                    if not r or len(r) != len(header):
                        continue

                    row_model = r[model_col_idx] if model_col_idx != -1 and model_col_idx < len(r) else ""

                    if benchmark_col_idx != -1:
                        bench_name = r[benchmark_col_idx]
                        for c_idx, cell_val in enumerate(r):
                            if c_idx in (model_col_idx, benchmark_col_idx):
                                continue
                            norm = ExtractorsModule.NormalizationEngine.validate_and_normalize_score(
                                cell_val, header[c_idx]
                            )
                            if norm:
                                num, norm_val, unit = norm
                                sub_candidates.append(
                                    BenchmarkCandidate(
                                        raw_model_name=row_model,
                                        raw_benchmark_name=bench_name,
                                        raw_value=cell_val,
                                        numeric_value=num,
                                        normalized_value=norm_val,
                                        metric=header[c_idx],
                                        unit=unit,
                                        confidence_tier="A",
                                        extraction_method="MARKDOWN_TABLE",
                                        location_info=f"Table row {row_idx+1}, col {c_idx+1}",
                                        raw_snippet=" | ".join(r),
                                    )
                                )

                    for c_idx, bench_canonical in score_col_indices:
                        if c_idx < len(r):
                            val = r[c_idx]
                            norm = ExtractorsModule.NormalizationEngine.validate_and_normalize_score(val, "accuracy")
                            if norm:
                                num, norm_val, unit = norm
                                sub_candidates.append(
                                    BenchmarkCandidate(
                                        raw_model_name=row_model,
                                        raw_benchmark_name=bench_canonical,
                                        raw_value=val,
                                        numeric_value=num,
                                        normalized_value=norm_val,
                                        metric="accuracy",
                                        unit=unit,
                                        confidence_tier="A",
                                        extraction_method="MARKDOWN_TABLE",
                                        location_info=f"Table row {row_idx+1}, col {c_idx+1}",
                                        raw_snippet=" | ".join(r),
                                    )
                                )

                return sub_candidates

            for line in lines:
                stripped = line.strip()
                if stripped.startswith("|") and stripped.endswith("|"):
                    table_buffer.append(stripped)
                    in_table = True
                else:
                    if in_table:
                        candidates.extend(process_table_buffer(table_buffer))
                        table_buffer = []
                        in_table = False

            if table_buffer:
                candidates.extend(process_table_buffer(table_buffer))

            return candidates

    class ProseRegexExtractor:
        PROSE_PATTERNS = [
            re.compile(
                r"(?:achieves?|scores?|attains?|reaches?|evaluates? to|delivers?)\s+([0-9]{1,3}(?:\.[0-9]{1,2})?)\s*%?\s+(?:on|in|across)\s+([A-Za-z0-9_\-\+]+(?:\s+[A-Za-z0-9_\-\+]+){0,2})",
                re.IGNORECASE,
            ),
            re.compile(
                r"\b([A-Za-z0-9_\-\+]{2,15})\s*(?:score|result|accuracy)?\s*(?:of|:|=)\s*([0-9]{1,3}(?:\.[0-9]{1,2})?)\s*%?",
                re.IGNORECASE,
            ),
        ]

        @classmethod
        def extract(cls, text: str) -> List[BenchmarkCandidate]:
            candidates = []
            for line in text.splitlines():
                line_s = line.strip()
                if len(line_s) < 8 or len(line_s) > 300:
                    continue

                for m in cls.PROSE_PATTERNS[0].finditer(line_s):
                    val_str = m.group(1)
                    bench_str = m.group(2).strip()
                    matched = ExtractorsModule.NormalizationEngine.match_benchmark(bench_str)
                    if matched:
                        norm = ExtractorsModule.NormalizationEngine.validate_and_normalize_score(
                            val_str, matched["default_metric"]
                        )
                        if norm:
                            num, norm_val, unit = norm
                            candidates.append(
                                BenchmarkCandidate(
                                    raw_model_name="",
                                    raw_benchmark_name=matched["canonical_name"],
                                    raw_value=val_str,
                                    numeric_value=num,
                                    normalized_value=norm_val,
                                    metric=matched["default_metric"],
                                    unit=unit,
                                    confidence_tier="A-",
                                    extraction_method="PROSE",
                                    location_info="README Prose",
                                    raw_snippet=line_s,
                                )
                            )

                for m in cls.PROSE_PATTERNS[1].finditer(line_s):
                    bench_str = m.group(1).strip()
                    val_str = m.group(2)
                    matched = ExtractorsModule.NormalizationEngine.match_benchmark(bench_str)
                    if matched:
                        norm = ExtractorsModule.NormalizationEngine.validate_and_normalize_score(
                            val_str, matched["default_metric"]
                        )
                        if norm:
                            num, norm_val, unit = norm
                            candidates.append(
                                BenchmarkCandidate(
                                    raw_model_name="",
                                    raw_benchmark_name=matched["canonical_name"],
                                    raw_value=val_str,
                                    numeric_value=num,
                                    normalized_value=norm_val,
                                    metric=matched["default_metric"],
                                    unit=unit,
                                    confidence_tier="A-",
                                    extraction_method="PROSE",
                                    location_info="README Prose",
                                    raw_snippet=line_s,
                                )
                            )

            return candidates

    class LocalOCRExtractor:
        @staticmethod
        def extract_from_image(image_bytes: bytes) -> List[BenchmarkCandidate]:
            candidates = []
            if not pytesseract or not PILImage:
                return candidates

            try:
                with PILImage.open(io.BytesIO(image_bytes)) as img:
                    ocr_text = pytesseract.image_to_string(img)
                    prose_candidates = ExtractorsModule.ProseRegexExtractor.extract(ocr_text)
                    for c in prose_candidates:
                        c.confidence_tier = "D"
                        c.extraction_method = "OCR"
                        c.location_info = "Image Local OCR"
                        candidates.append(c)
            except Exception:
                pass
            return candidates

class AdaptersModule:
    class ResilientHTTPClient:
        def __init__(self, token: Optional[str] = None):
            self.session = requests.Session() if requests else None
            self.token = token
            if self.session:
                retries = Retry(
                    total=MAX_HTTP_RETRIES,
                    backoff_factor=BACKOFF_FACTOR,
                    status_forcelist=[429, 500, 502, 503, 504],
                    raise_on_status=False,
                )
                adapter = HTTPAdapter(max_retries=retries, pool_connections=32, pool_maxsize=32)
                self.session.mount("https://", adapter)
                self.session.mount("http://", adapter)

                headers = {
                    "User-Agent": "Inferra-Universal-Data-Crawler/2.0 (Open-Source Research Pipeline; Windows 11)",
                    "Accept-Encoding": "gzip, deflate",
                }
                if self.token:
                    headers["Authorization"] = f"Bearer {self.token}"
                self.session.headers.update(headers)

        def get(self, url: str) -> Tuple[Optional[bytes], int, Dict[str, str]]:
            if not self.session:
                return None, 0, {}

            try:
                resp = self.session.get(url, timeout=HTTP_TIMEOUT)
                if resp.status_code == 429:
                    retry_after = resp.headers.get("Retry-After")
                    wait_sec = float(retry_after) if retry_after and retry_after.isdigit() else 10.0
                    time.sleep(wait_sec)

                return resp.content, resp.status_code, dict(resp.headers)
            except Exception as e:
                return None, 599, {"error": str(e)}

    class BaseSourceAdapter:
        def __init__(
            self,
            http_client: AdaptersModule.ResilientHTTPClient,
            artifact_mgr: StorageModule.ArtifactManager,
            logger: logging.Logger,
        ):
            self.http = http_client
            self.artifacts = artifact_mgr
            self.logger = logger

        def process_item(self, item_key: str) -> Optional[Tuple[SourceRecord, List[Any]]]:
            raise NotImplementedError

    class HuggingFaceSourceAdapter(BaseSourceAdapter):
        def fetch_repo_details(self, repo_id: str) -> Optional[Dict[str, Any]]:
            url = f"https://huggingface.co/api/models/{repo_id}"
            body, status, _ = self.http.get(url)
            if status == 200 and body:
                try:
                    return json.loads(body.decode("utf-8"))
                except Exception:
                    return None
            return None

        def process_item(self, repo_id: str) -> Optional[Tuple[SourceRecord, List[Any]]]:
            repo_meta = self.fetch_repo_details(repo_id)
            commit_sha = repo_meta.get("sha") if repo_meta else None

            source_url = f"https://huggingface.co/{repo_id}"
            source_rec = SourceRecord(
                source_type="HUGGINGFACE",
                url=source_url,
                repo_id=repo_id,
                commit_sha=commit_sha,
                http_status=200 if repo_meta else 404,
            )

            extracted_pairs = []

            readme_url = f"https://huggingface.co/{repo_id}/raw/main/README.md"
            content_bytes, status, headers = self.http.get(readme_url)

            if status == 200 and content_bytes:
                rel_path, sha256, size_bytes = self.artifacts.save_bytes(
                    content_bytes, "markdown", f"{repo_id.replace('/', '__')}_README.md"
                )
                art_rec = ArtifactRecord(
                    rel_path=rel_path, file_type="MARKDOWN", sha256=sha256, size_bytes=size_bytes
                )

                try:
                    text_content = content_bytes.decode("utf-8", errors="replace")
                except Exception:
                    text_content = ""

                card_evals = []
                if text_content:
                    yaml_cands = ExtractorsModule.ModelIndexYAMLExtractor.extract(text_content)
                    for c in yaml_cands:
                        c.raw_model_name = repo_id
                        ev = EvidenceRecord(
                            extraction_method=c.extraction_method,
                            location_info=c.location_info,
                            raw_snippet=c.raw_snippet,
                            confidence_score=0.98,
                        )
                        card_evals.append((ev, c))

                    md_cands = ExtractorsModule.MarkdownTableExtractor.extract(text_content)
                    for c in md_cands:
                        if not c.raw_model_name:
                            c.raw_model_name = repo_id
                        ev = EvidenceRecord(
                            extraction_method=c.extraction_method,
                            location_info=c.location_info,
                            raw_snippet=c.raw_snippet,
                            confidence_score=0.88,
                        )
                        card_evals.append((ev, c))

                    prose_cands = ExtractorsModule.ProseRegexExtractor.extract(text_content)
                    for c in prose_cands:
                        if not c.raw_model_name:
                            c.raw_model_name = repo_id
                        ev = EvidenceRecord(
                            extraction_method=c.extraction_method,
                            location_info=c.location_info,
                            raw_snippet=c.raw_snippet,
                            confidence_score=0.75,
                        )
                        card_evals.append((ev, c))

                    img_urls = re.findall(
                        r'!\[.*?\]\((https?://[^\s\)]+|(?:[^\s\)]+\.(?:png|jpg|jpeg|webp)))\)', text_content
                    )
                    for i_url in img_urls[:5]:
                        is_benchmark_img = any(
                            k in i_url.lower() for k in ["eval", "bench", "result", "chart", "score", "vs"]
                        )
                        if is_benchmark_img:
                            if i_url.startswith("http"):
                                full_img_url = i_url
                                if "huggingface.co" in full_img_url and "/blob/" in full_img_url:
                                    full_img_url = full_img_url.replace("/blob/", "/resolve/")
                            else:
                                clean_rel = i_url.lstrip("/")
                                full_img_url = f"https://huggingface.co/{repo_id}/resolve/main/{clean_rel}"

                            img_bytes, img_status, _ = self.http.get(full_img_url)
                            if img_status == 200 and img_bytes:
                                phash = self.artifacts.compute_phash(img_bytes)
                                img_rel_path, img_sha, img_size = self.artifacts.save_bytes(
                                    img_bytes, "images", f"{repo_id.replace('/', '__')}_{Path(i_url).name}"
                                )
                                img_art = ArtifactRecord(
                                    rel_path=img_rel_path,
                                    file_type="IMAGE",
                                    sha256=img_sha,
                                    phash=phash,
                                    size_bytes=img_size,
                                )
                                ocr_cands = ExtractorsModule.LocalOCRExtractor.extract_from_image(img_bytes)
                                img_evals = []
                                for c in ocr_cands:
                                    c.raw_model_name = repo_id
                                    ev = EvidenceRecord(
                                        extraction_method="OCR",
                                        location_info=f"Image: {i_url}",
                                        raw_snippet=c.raw_snippet,
                                        confidence_score=0.55,
                                    )
                                    img_evals.append((ev, c))
                                extracted_pairs.append((img_art, img_evals))

                extracted_pairs.append((art_rec, card_evals))

            return source_rec, extracted_pairs

class CoreModule:
    class CapabilityDetector:
        @staticmethod
        def inspect() -> Dict[str, Any]:
            capabilities = {
                "requests": requests is not None,
                "sqlite": True,
                "pyyaml": yaml is not None,
                "beautifulsoup4": BeautifulSoup is not None,
                "pillow": PILImage is not None,
                "pytesseract": False,
                "pypdf": pypdf is not None,
                "tesseract_binary": False,
            }

            tess_bin = shutil.which("tesseract")
            if not tess_bin and platform.system() == "Windows":
                default_win_path = r"C:\Program Files\Tesseract-OCR\tesseract.exe"
                if os.path.exists(default_win_path):
                    tess_bin = default_win_path
                    if pytesseract:
                        pytesseract.pytesseract.tesseract_cmd = default_win_path

            if tess_bin:
                capabilities["tesseract_binary"] = True
                if pytesseract:
                    capabilities["pytesseract"] = True

            total_features = len(capabilities)
            active_features = sum(1 for v in capabilities.values() if v)
            capabilities["score_percent"] = round((active_features / total_features) * 100, 1)
            return capabilities

        @staticmethod
        def print_banner(caps: Dict[str, Any]):
            print("=" * 64)
            print("INFERRA UNIVERSAL ACQUISITION ENGINE - CAPABILITY CHECK")
            print("=" * 64)
            for k, v in caps.items():
                if k == "score_percent":
                    continue
                mark = "✓" if v else "✗"
                print(f"  {k.ljust(22)} : {mark}")
            print(f"  Total Capability Score : {caps['score_percent']}%")
            print("=" * 64)
            if not caps["requests"]:
                print("CRITICAL: 'requests' is missing. Network fetching will fail.")
            if not caps["pyyaml"]:
                print("WARN: 'pyyaml' missing. Structured model-index YAML will use regex fallback.")
            if not caps["pytesseract"]:
                print("WARN: Local OCR disabled. Image tables will be hashed but not OCR parsed.")
            print("=" * 64 + "\n")

    class JsonlLogFormatter(logging.Formatter):
        def format(self, record: logging.LogRecord) -> str:
            log_obj = {
                "timestamp": datetime.datetime.now(datetime.timezone.utc).isoformat(),
                "level": record.levelname,
                "name": record.name,
                "message": record.getMessage(),
            }
            if hasattr(record, "subsystem"):
                log_obj["subsystem"] = record.subsystem
            if hasattr(record, "extra_data"):
                log_obj["extra"] = record.extra_data
            if record.exc_info:
                log_obj["exception"] = self.formatException(record.exc_info)
            return json.dumps(log_obj)

    @staticmethod
    def setup_logger(base_dir: Path) -> logging.Logger:
        logs_dir = base_dir / "logs"
        logs_dir.mkdir(parents=True, exist_ok=True)

        logger = logging.getLogger("inferra")
        logger.setLevel(logging.DEBUG)
        logger.propagate = False

        if logger.handlers:
            return logger

        err_handler = logging.FileHandler(logs_dir / "errors.jsonl", encoding="utf-8")
        err_handler.setLevel(logging.WARNING)
        err_handler.setFormatter(CoreModule.JsonlLogFormatter())
        logger.addHandler(err_handler)

        crawl_handler = logging.FileHandler(logs_dir / "crawl.log", encoding="utf-8")
        crawl_handler.setLevel(logging.INFO)
        fmt = logging.Formatter("[%(asctime)s][%(levelname)s][%(name)s] %(message)s")
        crawl_handler.setFormatter(fmt)
        logger.addHandler(crawl_handler)

        return logger

    class CrawlTelemetry:
        def __init__(self, db: StorageModule.DatabaseEngine, disk_mon: StorageModule.DiskMonitor):
            self.db = db
            self.disk = disk_mon
            self.start_time = time.time()
            self.lock = threading.Lock()

            self.discovered_count = 0
            self.queued_count = 0
            self.processed_count = 0
            self.failed_count = 0
            self.downloaded_count = 0
            self.extracted_count = 0
            self.current_item = "Initializing..."
            self.last_write_timestamp = time.time()
            self.last_download_timestamp = time.time()
            self.last_error_msg = "None"
            self.last_error_time = 0.0

            self.models_count = 0
            self.families_count = 0
            self.benchmarks_count = 0
            self.results_count = 0
            self.evidence_count = 0
            self.conflicts_count = 0

        def init_counts(self):
            conn = self.db.get_connection()
            try:
                self.models_count = conn.execute("SELECT COUNT(*) FROM models").fetchone()[0]
                self.families_count = conn.execute("SELECT COUNT(*) FROM model_families").fetchone()[0]
                self.benchmarks_count = conn.execute("SELECT COUNT(*) FROM benchmarks").fetchone()[0]
                self.results_count = conn.execute("SELECT COUNT(*) FROM results").fetchone()[0]
                self.conflicts_count = conn.execute("SELECT COUNT(*) FROM conflicts").fetchone()[0]
                self.evidence_count = conn.execute("SELECT COUNT(*) FROM evidence").fetchone()[0]
            except Exception:
                pass
            finally:
                conn.close()

        def record_error(self, msg: str):
            with self.lock:
                self.last_error_msg = msg
                self.last_error_time = time.time()

        def record_progress(self, item: str, extracted_num: int, stats: Optional[Dict[str, int]] = None):
            with self.lock:
                self.processed_count += 1
                self.downloaded_count += 1
                self.extracted_count += extracted_num
                self.current_item = item
                self.last_write_timestamp = time.time()
                self.last_download_timestamp = time.time()
                if stats:
                    self.models_count += stats.get("models_new", 0)
                    self.families_count += stats.get("families_new", 0)
                    self.benchmarks_count += stats.get("benchmarks_new", 0)
                    self.results_count += stats.get("results_new", 0)
                    self.evidence_count += stats.get("evidence_new", 0)
                    self.conflicts_count += stats.get("conflicts_new", 0)

        def render_terminal(self):
            elapsed = int(time.time() - self.start_time)
            hrs, rem = divmod(elapsed, 3600)
            mins, secs = divmod(rem, 60)
            free_gb = self.disk.get_free_space_gb()

            with self.lock:
                m_count = self.models_count
                f_count = self.families_count
                b_count = self.benchmarks_count
                r_count = self.results_count
                e_count = self.evidence_count
                c_count = self.conflicts_count
                disc_count = self.discovered_count
                proc_count = self.processed_count
                down_count = self.downloaded_count
                fail_count = self.failed_count
                curr_item = self.current_item
                last_err = self.last_error_msg
                last_write = self.last_write_timestamp

            now = time.time()
            stall_warn = ""
            if now - last_write > 600:
                stall_warn = "\n  !!! PIPELINE STALL DETECTED: No writes for >10 mins !!!"

            dash = f"""
================================================================
INFERRA DATA ACQUISITION ENGINE (PRODUCTION MONITOR)
Runtime: {hrs:02d}:{mins:02d}:{secs:02d} | Free Disk: {free_gb:.1f} GB
----------------------------------------------------------------
Repositories Discovered : {disc_count:,}
Processed / Downloaded  : {proc_count:,} / {down_count:,}
Failures / Errors       : {fail_count:,}
Current Processing      : {curr_item[:45]}
----------------------------------------------------------------
DATABASE STORED STATE:
  Models                : {m_count:,}
  Families              : {f_count:,}
  Benchmarks Catalog    : {b_count:,}
  Benchmark Results     : {r_count:,}
  Verified Evidence     : {e_count:,}
  Recorded Conflicts    : {c_count:,}
----------------------------------------------------------------
Last Error Recorded     : {last_err[:50]}
Pipeline Status         : RUNNING NORMAL{stall_warn}
================================================================
"""
            if platform.system() == "Windows":
                os.system("cls")
            else:
                sys.stdout.write("\033[2J\033[H")
            sys.stdout.write(dash)
            sys.stdout.flush()

class InferraCrawler:
    def __init__(self, args: argparse.Namespace):
        self.args = args
        self.base_dir = Path(args.base_dir)

        if not self.base_dir.drive and not self.base_dir.exists():
            try:
                self.base_dir.mkdir(parents=True, exist_ok=True)
            except Exception:
                self.base_dir = Path(FALLBACK_BASE_DIR)
                self.base_dir.mkdir(parents=True, exist_ok=True)
        elif not os.path.exists(self.base_dir.anchor):
            self.base_dir = Path(FALLBACK_BASE_DIR)
            self.base_dir.mkdir(parents=True, exist_ok=True)

        self.logger = CoreModule.setup_logger(self.base_dir)
        self.disk_monitor = StorageModule.DiskMonitor(str(self.base_dir))
        self.db = StorageModule.DatabaseEngine(self.base_dir / "database" / "inferra_master.db", self.logger)
        self.artifacts = StorageModule.ArtifactManager(self.base_dir)
        self.http = AdaptersModule.ResilientHTTPClient(token=args.hf_token)
        self.provenance_engine = StorageModule.ProvenanceConflictEngine(self.db, self.logger)
        self.telemetry = CoreModule.CrawlTelemetry(self.db, self.disk_monitor)
        self.hf_adapter = AdaptersModule.HuggingFaceSourceAdapter(self.http, self.artifacts, self.logger)
        self.stop_event = threading.Event()

        self.num_workers = max(1, min(32, getattr(args, "workers", 8)))
        self.task_queue: queue.Queue = queue.Queue(maxsize=self.num_workers * 10)
        self.write_queue: queue.Queue = queue.Queue(maxsize=500)

    def discover_huggingface_repos(self, limit: int = 10000) -> Generator[str, None, None]:
        base_url = "https://huggingface.co/api/models"
        params = {
            "sort": "downloads",
            "direction": "-1",
            "limit": "100",
            "full": "false",
            "config": "false",
        }
        url = f"{base_url}?{urllib.parse.urlencode(params)}"
        yielded = 0

        while url and not self.stop_event.is_set():
            body, status, headers = self.http.get(url)
            if status != 200 or not body:
                self.telemetry.record_error(f"HF Discovery HTTP {status}")
                time.sleep(5)
                continue

            try:
                models_list = json.loads(body.decode("utf-8"))
            except Exception as e:
                self.telemetry.record_error(f"HF Discovery JSON parse fail: {e}")
                break

            if not models_list:
                break

            for m in models_list:
                m_id = m.get("id")
                if m_id:
                    yield m_id
                    yielded += 1
                    if yielded >= limit:
                        return

            link_header = headers.get("link") or headers.get("Link")
            url = None
            if link_header:
                links = link_header.split(",")
                for l in links:
                    parts = l.split(";")
                    if len(parts) >= 2 and 'rel="next"' in parts[1]:
                        url = parts[0].strip().strip("<>")
                        break

            time.sleep(0.5)

    def _worker_loop(self):
        while not self.stop_event.is_set():
            try:
                repo_id = self.task_queue.get(timeout=0.5)
            except queue.Empty:
                continue

            if repo_id is None:
                self.task_queue.task_done()
                break

            try:
                res = self.hf_adapter.process_item(repo_id)
                if res:
                    self.write_queue.put(("SUCCESS", repo_id, res))
                else:
                    self.write_queue.put(("EMPTY", repo_id, None))
            except Exception as ex:
                self.write_queue.put(("ERROR", repo_id, str(ex)))
            finally:
                self.task_queue.task_done()

    def _writer_loop(self):
        conn = self.db.get_connection()
        try:
            while not self.stop_event.is_set() or not self.write_queue.empty():
                try:
                    item = self.write_queue.get(timeout=0.5)
                except queue.Empty:
                    continue

                if item is None:
                    self.write_queue.task_done()
                    break

                status, repo_id, payload = item
                if status == "SUCCESS":
                    source_rec, artifacts_data = payload
                    try:
                        with conn:
                            stats = self.provenance_engine.store_pipeline_batch(conn, source_rec, artifacts_data)
                            conn.execute(
                                """
                                INSERT INTO crawl_state (source_key, state, updated_at)
                                VALUES (?, 'STORED', ?)
                                ON CONFLICT(source_key) DO UPDATE SET state = 'STORED', updated_at = excluded.updated_at
                            """,
                                (repo_id, datetime.datetime.now(datetime.timezone.utc).isoformat()),
                            )
                        extracted_total = sum(len(pairs) for _, pairs in artifacts_data)
                        self.telemetry.record_progress(repo_id, extracted_total, stats)
                    except Exception as e:
                        self.telemetry.failed_count += 1
                        self.telemetry.record_error(f"DB Write error {repo_id}: {e}")
                        self.logger.error(f"DB write failed for {repo_id}", exc_info=True)
                elif status == "EMPTY":
                    try:
                        with conn:
                            conn.execute(
                                """
                                INSERT INTO crawl_state (source_key, state, updated_at)
                                VALUES (?, 'EMPTY', ?)
                                ON CONFLICT(source_key) DO UPDATE SET state = 'EMPTY', updated_at = excluded.updated_at
                            """,
                                (repo_id, datetime.datetime.now(datetime.timezone.utc).isoformat()),
                            )
                    except Exception:
                        pass
                elif status == "ERROR":
                    self.telemetry.failed_count += 1
                    self.telemetry.record_error(f"Worker error {repo_id}: {payload}")
                    try:
                        with conn:
                            conn.execute(
                                """
                                INSERT INTO crawl_state (source_key, state, last_error, updated_at)
                                VALUES (?, 'ERROR', ?, ?)
                                ON CONFLICT(source_key) DO UPDATE SET state = 'ERROR', last_error = excluded.last_error, updated_at = excluded.updated_at
                            """,
                                (repo_id, str(payload)[:200], datetime.datetime.now(datetime.timezone.utc).isoformat()),
                            )
                    except Exception:
                        pass

                self.write_queue.task_done()
        finally:
            conn.close()

    def run(self):
        caps = CoreModule.CapabilityDetector.inspect()
        CoreModule.CapabilityDetector.print_banner(caps)

        if self.args.verify_db:
            print("Running SQLite database verification...")
            issues = self.db.run_integrity_check()
            if issues:
                print(f"FAILED: {len(issues)} issues detected:")
                for i in issues:
                    print(f"  - {i}")
            else:
                print("SUCCESS: Database integrity and foreign keys 100% verified.")
            return

        boot_conn = self.db.get_connection()
        try:
            self.provenance_engine.bootstrap_cache(boot_conn)
            stored_keys = set()
            if not self.args.fresh:
                rows = boot_conn.execute("SELECT source_key FROM crawl_state WHERE state = 'STORED'").fetchall()
                stored_keys = {r[0] for r in rows}
        finally:
            boot_conn.close()

        self.telemetry.init_counts()

        print(f"Active base data directory: {self.base_dir.resolve()}")
        print(f"Starting pipeline with {self.num_workers} fetch workers and dedicated writer thread...")

        def monitor_loop():
            while not self.stop_event.is_set():
                self.telemetry.render_terminal()
                time.sleep(3)

        mon_thread = threading.Thread(target=monitor_loop, daemon=True)
        mon_thread.start()

        writer_thread = threading.Thread(target=self._writer_loop, daemon=True)
        writer_thread.start()

        workers = []
        for _ in range(self.num_workers):
            t = threading.Thread(target=self._worker_loop, daemon=True)
            t.start()
            workers.append(t)

        start_time = time.time()
        max_duration = self.args.max_hours * 3600

        try:
            for repo_id in self.discover_huggingface_repos(limit=self.args.max_repos):
                if self.stop_event.is_set():
                    break

                if time.time() - start_time > max_duration:
                    self.logger.info("Configured max-hours elapsed. Initiating graceful shutdown.")
                    break

                disk_status = self.disk_monitor.check_safety_threshold()
                if disk_status == "EMERGENCY_STOP":
                    self.logger.critical("Disk space below 1.5%. Stopping immediately to protect DB.")
                    break

                if not self.args.fresh and repo_id in stored_keys:
                    continue

                self.telemetry.discovered_count += 1

                while not self.stop_event.is_set():
                    try:
                        self.task_queue.put(repo_id, timeout=1.0)
                        break
                    except queue.Full:
                        continue

        except KeyboardInterrupt:
            print("\nInterrupt received. Finalizing pending transactions...")
        finally:
            self.stop_event.set()

            for _ in range(self.num_workers):
                self.task_queue.put(None)
            for t in workers:
                t.join(timeout=3)

            self.write_queue.put(None)
            writer_thread.join(timeout=10)

            mon_thread.join(timeout=2)
            self._generate_final_report()

    def _generate_final_report(self):
        reports_dir = self.base_dir / "reports"
        reports_dir.mkdir(parents=True, exist_ok=True)
        report_path = reports_dir / "FINAL_REPORT.json"

        with self.telemetry.lock:
            summary = {
                "generated_at": datetime.datetime.now(datetime.timezone.utc).isoformat(),
                "discovered_repositories": self.telemetry.discovered_count,
                "processed_repositories": self.telemetry.processed_count,
                "models_indexed": self.telemetry.models_count,
                "model_families": self.telemetry.families_count,
                "benchmarks_cataloged": self.telemetry.benchmarks_count,
                "results_stored": self.telemetry.results_count,
                "conflicts_flagged": self.telemetry.conflicts_count,
                "remaining_disk_gb": self.disk_monitor.get_free_space_gb(),
            }

        report_path.write_text(json.dumps(summary, indent=2), encoding="utf-8")
        print("\n" + "=" * 64)
        print("INFERRA RUN COMPLETED")
        print(f"Report written to: {report_path}")
        print("=" * 64)

def parse_arguments() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Inferra Master Data Ingestion Pipeline: Continuous AI Model & Benchmark Acquisition Engine."
    )
    parser.add_argument(
        "--base-dir",
        type=str,
        default=DEFAULT_BASE_DIR,
        help=f"Target root storage directory (default: {DEFAULT_BASE_DIR})",
    )
    parser.add_argument(
        "--max-hours",
        type=float,
        default=24.0,
        help="Maximum hours to run before clean shutdown (default: 24.0)",
    )
    parser.add_argument(
        "--max-repos",
        type=int,
        default=500000,
        help="Maximum repositories to discover and evaluate (default: 500,000)",
    )
    parser.add_argument(
        "--hf-token",
        type=str,
        default=None,
        help="Optional Hugging Face user access token for higher public rate limits",
    )
    parser.add_argument(
        "--workers",
        type=int,
        default=8,
        help="Number of concurrent worker threads for data fetching (default: 8)",
    )
    parser.add_argument(
        "--fresh",
        action="store_true",
        help="Ignore previous crawl checkpoints and re-crawl all items",
    )
    parser.add_argument(
        "--verify-db",
        action="store_true",
        help="Execute database integrity and foreign key validation checks and exit",
    )
    return parser.parse_args()

if __name__ == "__main__":
    cli_args = parse_arguments()
    crawler = InferraCrawler(cli_args)
    crawler.run()