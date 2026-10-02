import tempfile
from pathlib import Path
import pytest

from inferra_data import (
    InferraConfig,
    NormalizationEngine,
    IdentityResolutionEngine,
    ModelType,
    ArtifactFormat,
    MasterDatabase,
    DataValidationEngine,
    CanonicalModelEntity,
    ValidationSeverity,
)


def test_parameter_count_normalization():
    assert NormalizationEngine.normalize_parameter_count("30.5B") == (30_500_000_000, "30.5B")
    assert NormalizationEngine.normalize_parameter_count("70b") == (70_000_000_000, "70b")
    assert NormalizationEngine.normalize_parameter_count("1.5T") == (1_500_000_000_000, "1.5T")
    assert NormalizationEngine.normalize_parameter_count("500M") == (500_000_000, "500M")
    assert NormalizationEngine.normalize_parameter_count("8000") == (8000, "8000")
    assert NormalizationEngine.normalize_parameter_count(None) == (None, None)


def test_context_length_normalization():
    assert NormalizationEngine.normalize_context_length("128k") == (131072, "128k")
    assert NormalizationEngine.normalize_context_length("32K") == (32768, "32K")
    assert NormalizationEngine.normalize_context_length("4096") == (4096, "4096")
    assert NormalizationEngine.normalize_context_length("1m") == (1048576, "1m")


def test_quantization_detection():
    fmt, quant = NormalizationEngine.normalize_quantization("Llama-3.1-8B-Q4_K_M.gguf")
    assert fmt == ArtifactFormat.GGUF
    assert quant == "Q4_K_M"

    fmt, quant = NormalizationEngine.normalize_quantization("model.safetensors")
    assert fmt == ArtifactFormat.SAFETENSORS
    assert quant == "NONE"

    fmt, quant = NormalizationEngine.normalize_quantization("model-fp8.safetensors")
    assert fmt == ArtifactFormat.SAFETENSORS
    assert quant == "FP8"


def test_identity_resolution():
    c_id, fam_id, m_type, conf, rule = IdentityResolutionEngine.resolve_huggingface_id(
        "meta-llama/Llama-3.1-70B-Instruct"
    )
    assert c_id == "meta/llama-3.1-70b-instruct"
    assert m_type == ModelType.INSTRUCT
    assert conf == 1.0

    # Quantizer mirror resolution
    c_id2, _, m_type2, conf2, rule2 = IdentityResolutionEngine.resolve_huggingface_id(
        "bartowski/Llama-3.1-70B-Instruct-GGUF"
    )
    assert "llama-3.1-70b-instruct" in c_id2
    assert m_type2 == ModelType.INSTRUCT
    assert conf2 == 0.85


def test_validation_detects_impossible_parameters():
    with tempfile.TemporaryDirectory() as tmp_dir:
        db_path = Path(tmp_dir) / "test.db"
        db = MasterDatabase(db_path)

        # Insert invalid model
        model = CanonicalModelEntity(
            canonical_id="test/invalid-model",
            family_id="test/family",
            org_id="test",
            display_name="Invalid Model",
            model_type=ModelType.BASE,
            parameter_count=-500,  # Impossible value
            context_length=128000,
        )

        # Register organization and family first to pass foreign key constraints
        db.conn.execute("INSERT INTO organizations (org_id, name) VALUES ('test', 'Test Org')")
        db.conn.execute("INSERT INTO model_families (family_id, org_id, name) VALUES ('test/family', 'test', 'Test')")
        db.upsert_canonical_model(model)

        val = DataValidationEngine(db, strict=False)
        issues = val.run_all_checks()

        error_issues = [i for i in issues if i.severity == ValidationSeverity.ERROR]
        assert len(error_issues) >= 1
        assert error_issues[0].field_name == "parameter_count"