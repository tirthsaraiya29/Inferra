#!/usr/bin/env python3
"""
Inferra Asset Database Compiler.

Generates inferra_models_v1.sqlite3 containing full benchmark, artifact, model,
and provider pricing data for deployment in app/src/main/assets/.
"""

import sqlite3
import os
import datetime
from pathlib import Path

PROJECT_ROOT = Path(__file__).parent.parent
ASSETS_DIR = PROJECT_ROOT / "app" / "src" / "main" / "assets"
TARGET_DB_PATH = ASSETS_DIR / "inferra_models_v1.sqlite3"

def build_database():
    ASSETS_DIR.mkdir(parents=True, exist_ok=True)
    if TARGET_DB_PATH.exists():
        TARGET_DB_PATH.unlink()

    conn = sqlite3.connect(str(TARGET_DB_PATH))
    conn.execute("PRAGMA foreign_keys = ON;")
    conn.execute("PRAGMA journal_mode = DELETE;")

    # 1. Base Tables
    conn.executescript("""
    CREATE TABLE android_models (
        id TEXT PRIMARY KEY,
        display_name TEXT NOT NULL,
        family_name TEXT,
        organization TEXT NOT NULL,
        model_type TEXT NOT NULL,
        parameter_count INTEGER,
        context_length INTEGER,
        license TEXT,
        is_open_weights INTEGER NOT NULL,
        updated_at TEXT NOT NULL
    );

    CREATE TABLE android_artifacts (
        artifact_id TEXT PRIMARY KEY,
        model_id TEXT NOT NULL REFERENCES android_models(id) ON DELETE CASCADE,
        format TEXT NOT NULL,
        quantization TEXT,
        file_size_bytes INTEGER,
        repository_id TEXT NOT NULL,
        file_path TEXT
    );

    CREATE TABLE android_benchmarks (
        benchmark_id TEXT PRIMARY KEY,
        name TEXT NOT NULL,
        domain TEXT NOT NULL,
        metric_name TEXT NOT NULL
    );

    CREATE TABLE android_benchmark_scores (
        id TEXT PRIMARY KEY,
        model_id TEXT NOT NULL REFERENCES android_models(id) ON DELETE CASCADE,
        benchmark_id TEXT NOT NULL REFERENCES android_benchmarks(benchmark_id),
        score REAL NOT NULL,
        score_normalized REAL NOT NULL,
        measurement_type TEXT NOT NULL
    );

    CREATE TABLE android_provider_pricing (
        id TEXT PRIMARY KEY,
        model_id TEXT NOT NULL REFERENCES android_models(id) ON DELETE CASCADE,
        provider_name TEXT NOT NULL,
        input_cost_per_m REAL,
        output_cost_per_m REAL,
        context_window INTEGER,
        updated_at TEXT NOT NULL
    );
    """)

    # 2. Benchmark Registry
    benchmarks = [
        ("mmlu_pro", "MMLU-Pro", "REASONING", "Accuracy"),
        ("gpqa", "GPQA Diamond", "SCIENCE", "Accuracy"),
        ("math_l5", "MATH Level 5", "MATHEMATICS", "Accuracy"),
        ("ifeval", "IFEval", "INSTRUCTION_FOLLOWING", "Strict Accuracy"),
        ("musr", "MuSR", "REASONING", "Accuracy"),
        ("bbh", "BIG-Bench Hard", "REASONING", "Accuracy"),
        ("aime", "AIME 2024", "MATHEMATICS", "Accuracy"),
        ("gsm8k", "GSM8K", "MATHEMATICS", "Accuracy"),
        ("swe_bench_verified", "SWE-bench Verified", "CODING", "Resolve Rate"),
        ("swe_bench_lite", "SWE-bench Lite", "CODING", "Resolve Rate"),
        ("humaneval_plus", "HumanEval+", "CODING", "Pass@1"),
        ("mbpp_plus", "MBPP+", "CODING", "Pass@1"),
        ("livecodebench", "LiveCodeBench", "CODING", "Pass@1"),
        ("aider_polyglot", "Aider Polyglot", "CODING", "Success Rate"),
        ("bfcl", "BFCL Function Calling", "AGENTS", "Accuracy"),
        ("tau_bench", "TAU-bench", "AGENTS", "Task Success Rate"),
        ("gaia", "GAIA", "AGENTS", "Success Rate"),
        ("ruler", "RULER Long Context", "LONG_CONTEXT", "Average Accuracy"),
        ("mmmu", "MMMU", "MULTIMODAL", "Accuracy"),
        ("mathvista", "MathVista", "MULTIMODAL", "Accuracy"),
        ("chartqa", "ChartQA", "MULTIMODAL", "Accuracy"),
        ("docvqa", "DocVQA", "MULTIMODAL", "ANLS"),
        ("video_mme", "Video-MME", "MULTIMODAL", "Accuracy"),
        ("livebench", "LiveBench", "REASONING", "Overall Average"),
        ("simpleqa", "SimpleQA", "FACTUALITY", "Correctness Rate"),
        ("arena_elo", "Chatbot Arena Elo", "HUMAN_PREFERENCE", "Bradley-Terry Elo"),
        ("arena_coding_elo", "Arena Coding Elo", "HUMAN_PREFERENCE", "Bradley-Terry Elo"),
        ("arena_hard_elo", "Arena Hard Prompts Elo", "HUMAN_PREFERENCE", "Bradley-Terry Elo"),
    ]

    for b_id, b_name, b_dom, b_met in benchmarks:
        conn.execute("INSERT INTO android_benchmarks VALUES (?, ?, ?, ?)", (b_id, b_name, b_dom, b_met))

    # 3. Premier Models Dataset
    now = datetime.datetime.now(datetime.timezone.utc).isoformat()
    models = [
        ("meta/llama-3.3-70b-instruct", "Llama 3.3 70B Instruct", "Llama 3.3", "Meta", "INSTRUCT", 70, 128000, "Llama Community License", 1, now),
        ("deepseek/deepseek-r1", "DeepSeek R1", "DeepSeek R1", "DeepSeek", "REASONING", 671, 128000, "MIT", 1, now),
        ("deepseek/deepseek-v3", "DeepSeek V3", "DeepSeek V3", "DeepSeek", "INSTRUCT", 671, 128000, "MIT", 1, now),
        ("qwen/qwen2.5-72b-instruct", "Qwen 2.5 72B Instruct", "Qwen 2.5", "Qwen", "INSTRUCT", 72, 131072, "Apache 2.0", 1, now),
        ("qwen/qwen2.5-coder-32b-instruct", "Qwen 2.5 Coder 32B Instruct", "Qwen 2.5 Coder", "Qwen", "INSTRUCT", 32, 131072, "Apache 2.0", 1, now),
        ("google/gemma-2-27b-it", "Gemma 2 27B IT", "Gemma 2", "Google", "INSTRUCT", 27, 8192, "Gemma License", 1, now),
        ("google/gemma-4-31b-it", "Gemma 4 31B IT", "Gemma 4", "Google", "INSTRUCT", 31, 128000, "Gemma License", 1, now),
        ("anthropic/claude-3-5-sonnet", "Claude 3.5 Sonnet", "Claude 3.5", "Anthropic", "INSTRUCT", 0, 200000, "Proprietary", 0, now),
        ("anthropic/claude-3-7-sonnet", "Claude 3.7 Sonnet", "Claude 3.7", "Anthropic", "REASONING", 0, 200000, "Proprietary", 0, now),
        ("openai/gpt-4o", "GPT-4o", "GPT-4", "OpenAI", "INSTRUCT", 0, 128000, "Proprietary", 0, now),
        ("openai/o3-mini", "o3-mini", "o3", "OpenAI", "REASONING", 0, 200000, "Proprietary", 0, now),
        ("mistralai/mistral-large-instruct-2411", "Mistral Large 2", "Mistral", "Mistral AI", "INSTRUCT", 123, 128000, "MNLP", 1, now),
    ]

    for m in models:
        conn.execute("INSERT INTO android_models VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", m)

    # 4. Model Artifacts
    artifacts = [
        ("art_llama33_q4", "meta/llama-3.3-70b-instruct", "GGUF", "Q4_K_M", 42500000000, "bartowski/Llama-3.3-70B-Instruct-GGUF", "Llama-3.3-70B-Instruct-Q4_K_M.gguf"),
        ("art_llama33_fp8", "meta/llama-3.3-70b-instruct", "SAFETENSORS", "FP8", 70000000000, "meta-llama/Llama-3.3-70B-Instruct", "model.safetensors"),
        ("art_ds_r1_q4", "deepseek/deepseek-r1", "GGUF", "Q4_K_M", 380000000000, "unsloth/DeepSeek-R1-GGUF", "DeepSeek-R1-Q4_K_M.gguf"),
        ("art_qwen25_72b_q4", "qwen/qwen2.5-72b-instruct", "GGUF", "Q4_K_M", 43500000000, "Qwen/Qwen2.5-72B-Instruct-GGUF", "qwen2.5-72b-instruct-q4_k_m.gguf"),
        ("art_qwen25_coder_q4", "qwen/qwen2.5-coder-32b-instruct", "GGUF", "Q4_K_M", 20100000000, "Qwen/Qwen2.5-Coder-32B-Instruct-GGUF", "qwen2.5-coder-32b-instruct-q4_k_m.gguf"),
        ("art_gemma2_27b_q4", "google/gemma-2-27b-it", "GGUF", "Q4_K_M", 16500000000, "bartowski/gemma-2-27b-it-GGUF", "gemma-2-27b-it-Q4_K_M.gguf"),
        ("art_gemma4_31b_q4", "google/gemma-4-31b-it", "GGUF", "Q4_K_M", 18200000000, "google/gemma-4-31b-it-GGUF", "gemma-4-31b-it-Q4_K_M.gguf"),
        ("art_mistral_large_q4", "mistralai/mistral-large-instruct-2411", "GGUF", "Q4_K_M", 72000000000, "bartowski/Mistral-Large-Instruct-2411-GGUF", "Mistral-Large-Instruct-2411-Q4_K_M.gguf"),
    ]

    for a in artifacts:
        conn.execute("INSERT INTO android_artifacts VALUES (?, ?, ?, ?, ?, ?, ?)", a)

    # 5. Benchmark Scores
    scores = [
        # Llama 3.3 70B
        ("s1", "meta/llama-3.3-70b-instruct", "mmlu_pro", 71.2, 0.712, "LEADERBOARD_MEASURED"),
        ("s2", "meta/llama-3.3-70b-instruct", "gpqa", 49.3, 0.493, "LEADERBOARD_MEASURED"),
        ("s3", "meta/llama-3.3-70b-instruct", "math_l5", 54.0, 0.540, "LEADERBOARD_MEASURED"),
        ("s4", "meta/llama-3.3-70b-instruct", "ifeval", 89.2, 0.892, "LEADERBOARD_MEASURED"),
        ("s5", "meta/llama-3.3-70b-instruct", "swe_bench_verified", 48.2, 0.482, "LEADERBOARD_MEASURED"),
        ("s6", "meta/llama-3.3-70b-instruct", "livecodebench", 42.1, 0.421, "LEADERBOARD_MEASURED"),
        ("s7", "meta/llama-3.3-70b-instruct", "bfcl", 88.5, 0.885, "LEADERBOARD_MEASURED"),
        ("s8", "meta/llama-3.3-70b-instruct", "arena_elo", 1310.0, 0.820, "HUMAN_PREFERENCE"),

        # DeepSeek R1
        ("s10", "deepseek/deepseek-r1", "mmlu_pro", 84.0, 0.840, "LEADERBOARD_MEASURED"),
        ("s11", "deepseek/deepseek-r1", "gpqa", 71.5, 0.715, "LEADERBOARD_MEASURED"),
        ("s12", "deepseek/deepseek-r1", "math_l5", 79.8, 0.798, "LEADERBOARD_MEASURED"),
        ("s13", "deepseek/deepseek-r1", "aime", 79.8, 0.798, "LEADERBOARD_MEASURED"),
        ("s14", "deepseek/deepseek-r1", "swe_bench_verified", 49.2, 0.492, "LEADERBOARD_MEASURED"),
        ("s15", "deepseek/deepseek-r1", "livecodebench", 65.9, 0.659, "LEADERBOARD_MEASURED"),
        ("s16", "deepseek/deepseek-r1", "arena_elo", 1362.0, 0.885, "HUMAN_PREFERENCE"),

        # DeepSeek V3
        ("s20", "deepseek/deepseek-v3", "mmlu_pro", 75.9, 0.759, "LEADERBOARD_MEASURED"),
        ("s21", "deepseek/deepseek-v3", "gpqa", 59.1, 0.591, "LEADERBOARD_MEASURED"),
        ("s22", "deepseek/deepseek-v3", "math_l5", 65.2, 0.652, "LEADERBOARD_MEASURED"),
        ("s23", "deepseek/deepseek-v3", "swe_bench_verified", 49.2, 0.492, "LEADERBOARD_MEASURED"),
        ("s24", "deepseek/deepseek-v3", "bfcl", 89.4, 0.894, "LEADERBOARD_MEASURED"),
        ("s25", "deepseek/deepseek-v3", "arena_elo", 1338.0, 0.850, "HUMAN_PREFERENCE"),

        # Qwen 2.5 72B
        ("s30", "qwen/qwen2.5-72b-instruct", "mmlu_pro", 72.5, 0.725, "LEADERBOARD_MEASURED"),
        ("s31", "qwen/qwen2.5-72b-instruct", "gpqa", 48.9, 0.489, "LEADERBOARD_MEASURED"),
        ("s32", "qwen/qwen2.5-72b-instruct", "math_l5", 58.4, 0.584, "LEADERBOARD_MEASURED"),
        ("s33", "qwen/qwen2.5-72b-instruct", "bfcl", 87.2, 0.872, "LEADERBOARD_MEASURED"),
        ("s34", "qwen/qwen2.5-72b-instruct", "arena_elo", 1302.0, 0.810, "HUMAN_PREFERENCE"),

        # Qwen 2.5 Coder 32B
        ("s40", "qwen/qwen2.5-coder-32b-instruct", "mmlu_pro", 67.4, 0.674, "LEADERBOARD_MEASURED"),
        ("s41", "qwen/qwen2.5-coder-32b-instruct", "swe_bench_verified", 48.9, 0.489, "LEADERBOARD_MEASURED"),
        ("s42", "qwen/qwen2.5-coder-32b-instruct", "livecodebench", 45.3, 0.453, "LEADERBOARD_MEASURED"),
        ("s43", "qwen/qwen2.5-coder-32b-instruct", "humaneval_plus", 88.4, 0.884, "LEADERBOARD_MEASURED"),
        ("s44", "qwen/qwen2.5-coder-32b-instruct", "arena_coding_elo", 1295.0, 0.800, "HUMAN_PREFERENCE"),

        # Claude 3.5 Sonnet
        ("s50", "anthropic/claude-3-5-sonnet", "mmlu_pro", 78.0, 0.780, "PROVIDER_REPORTED"),
        ("s51", "anthropic/claude-3-5-sonnet", "gpqa", 65.0, 0.650, "PROVIDER_REPORTED"),
        ("s52", "anthropic/claude-3-5-sonnet", "swe_bench_verified", 49.0, 0.490, "PROVIDER_REPORTED"),
        ("s53", "anthropic/claude-3-5-sonnet", "bfcl", 90.2, 0.902, "INDEPENDENT_BENCHMARK"),
        ("s54", "anthropic/claude-3-5-sonnet", "arena_elo", 1370.0, 0.895, "HUMAN_PREFERENCE"),

        # Claude 3.7 Sonnet
        ("s55", "anthropic/claude-3-7-sonnet", "mmlu_pro", 82.5, 0.825, "PROVIDER_REPORTED"),
        ("s56", "anthropic/claude-3-7-sonnet", "gpqa", 70.2, 0.702, "PROVIDER_REPORTED"),
        ("s57", "anthropic/claude-3-7-sonnet", "swe_bench_verified", 70.3, 0.703, "PROVIDER_REPORTED"),
        ("s58", "anthropic/claude-3-7-sonnet", "tau_bench", 81.4, 0.814, "PROVIDER_REPORTED"),
        ("s59", "anthropic/claude-3-7-sonnet", "arena_elo", 1395.0, 0.920, "HUMAN_PREFERENCE"),

        # GPT-4o
        ("s60", "openai/gpt-4o", "mmlu_pro", 76.8, 0.768, "PROVIDER_REPORTED"),
        ("s61", "openai/gpt-4o", "gpqa", 53.6, 0.536, "PROVIDER_REPORTED"),
        ("s62", "openai/gpt-4o", "swe_bench_verified", 38.8, 0.388, "PROVIDER_REPORTED"),
        ("s63", "openai/gpt-4o", "bfcl", 88.9, 0.889, "INDEPENDENT_BENCHMARK"),
        ("s64", "openai/gpt-4o", "arena_elo", 1365.0, 0.890, "HUMAN_PREFERENCE"),

        # o3-mini
        ("s70", "openai/o3-mini", "mmlu_pro", 82.1, 0.821, "PROVIDER_REPORTED"),
        ("s71", "openai/o3-mini", "gpqa", 79.7, 0.797, "PROVIDER_REPORTED"),
        ("s72", "openai/o3-mini", "math_l5", 87.3, 0.873, "PROVIDER_REPORTED"),
        ("s73", "openai/o3-mini", "swe_bench_verified", 42.0, 0.420, "PROVIDER_REPORTED"),
        ("s74", "openai/o3-mini", "arena_elo", 1380.0, 0.905, "HUMAN_PREFERENCE"),
    ]

    for s in scores:
        conn.execute("INSERT INTO android_benchmark_scores VALUES (?, ?, ?, ?, ?, ?)", s)

    # 6. Provider Pricing
    pricing = [
        ("p1", "anthropic/claude-3-5-sonnet", "Anthropic API", 3.00, 15.00, 200000, now),
        ("p2", "anthropic/claude-3-7-sonnet", "Anthropic API", 3.00, 15.00, 200000, now),
        ("p3", "openai/gpt-4o", "OpenAI API", 2.50, 10.00, 128000, now),
        ("p4", "openai/o3-mini", "OpenAI API", 1.10, 4.40, 200000, now),
        ("p5", "deepseek/deepseek-v3", "DeepSeek API", 0.14, 0.28, 64000, now),
        ("p6", "deepseek/deepseek-r1", "DeepSeek API", 0.55, 2.19, 64000, now),
        ("p7", "meta/llama-3.3-70b-instruct", "Together AI", 0.88, 0.88, 128000, now),
        ("p8", "qwen/qwen2.5-72b-instruct", "Together AI", 0.90, 0.90, 131072, now),
    ]

    for p in pricing:
        conn.execute("INSERT INTO android_provider_pricing VALUES (?, ?, ?, ?, ?, ?, ?)", p)

    # 7. Build Wide-Column Table (android_models_wide)
    benchmark_ids = [b[0] for b in benchmarks]
    col_defs = [f"{b_id} REAL" for b_id in benchmark_ids]

    wide_table_sql = f"""
    CREATE TABLE android_models_wide (
        model_id TEXT PRIMARY KEY REFERENCES android_models(id) ON DELETE CASCADE,
        display_name TEXT NOT NULL,
        organization TEXT NOT NULL,
        parameter_count INTEGER,
        context_length INTEGER,
        license TEXT,
        {", ".join(col_defs)}
    );
    """
    conn.execute(wide_table_sql)

    select_cols = [f"MAX(CASE WHEN s.benchmark_id = '{b_id}' THEN ROUND(s.score, 2) END) AS {b_id}" for b_id in benchmark_ids]
    populate_wide_sql = f"""
    INSERT INTO android_models_wide
    SELECT
        m.id,
        m.display_name,
        m.organization,
        m.parameter_count,
        m.context_length,
        m.license,
        {", ".join(select_cols)}
    FROM android_models m
    LEFT JOIN android_benchmark_scores s ON m.id = s.model_id
    GROUP BY m.id;
    """
    conn.execute(populate_wide_sql)

    conn.commit()
    conn.execute("VACUUM;")
    conn.close()
    print(f"SUCCESS: Generated asset database at {TARGET_DB_PATH} ({os.path.getsize(TARGET_DB_PATH)} bytes)")

if __name__ == "__main__":
    build_database()
