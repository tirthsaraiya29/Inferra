package com.inferra.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Serializable
enum class ModelVariant {
    BASE,
    INSTRUCT,
    REASONING,
    FINE_TUNE,
    DISTILLED
}

@Serializable
enum class AliasType {
    HUGGING_FACE,
    PROVIDER_MODEL_ID,
    DISPLAY_NAME
}

@Immutable
@Serializable
data class ModelFamily(
    val id: String,
    val name: String,
    val author: String,
    val description: String = "",
    val websiteUrl: String = ""
)

@Immutable
@Serializable
data class ModelAlias(
    val aliasId: String,
    val canonicalId: String,
    val aliasType: AliasType
)

@Immutable
@Serializable
data class CanonicalModel(
    val id: String,                           // e.g. "canonical:qwen-qwen2.5-coder-32b-instruct"
    val familyId: String,                     // e.g. "family:qwen-2.5"
    val variantType: ModelVariant,
    val name: String,
    val author: String,
    val description: String,
    val baseModelId: String? = null,
    val distilledFromId: String? = null,
    val totalParamsBillion: Float,
    val activeParamsBillion: Float,
    val isMoe: Boolean = false,
    val architecture: String,
    val contextLengthTokens: Int,
    val modalities: List<Modality>,
    val tasks: List<ModelTask>,
    val license: LicenseType,
    val licenseName: String,
    val createdDate: String = "",
    val updatedDate: String = ""
)
