package com.inferra.domain.model

data class ProvenanceInfo(
    val organization: String,
    val repositoryPath: String,
    val officialReleaseUrl: String,
    val isOpenWeights: Boolean,
    val licenseName: String,
)

object ProvenanceResolver {

    private val PROPRIETARY_PROVIDERS = mapOf(
        "anthropic" to "https://www.anthropic.com/news",
        "openai" to "https://openai.com/index",
        "google-vertex" to "https://cloud.google.com/vertex-ai"
    )

    fun resolveProvenance(
        modelId: String,
        organization: String,
        isOpenWeights: Boolean,
        licenseName: String?
    ): ProvenanceInfo {
        val repoPath = if (modelId.contains("/")) {
            val parts = modelId.split("/")
            val org = parts[0]
            val name = parts[1]
            val normalizedOrg = when (org.lowercase()) {
                "meta" -> "meta-llama"
                "google" -> "google"
                "qwen" -> "Qwen"
                "deepseek" -> "deepseek-ai"
                "mistralai" -> "mistralai"
                "anthropic" -> "anthropic"
                "openai" -> "openai"
                else -> org
            }
            "$normalizedOrg/$name"
        } else {
            "$organization/$modelId"
        }

        val url = if (isOpenWeights) {
            "https://huggingface.co/$repoPath"
        } else {
            val key = organization.lowercase()
            PROPRIETARY_PROVIDERS[key] ?: "https://huggingface.co/$repoPath"
        }

        return ProvenanceInfo(
            organization = organization,
            repositoryPath = repoPath,
            officialReleaseUrl = url,
            isOpenWeights = isOpenWeights,
            licenseName = licenseName ?: if (isOpenWeights) "Open Weights" else "Proprietary"
        )
    }

    fun resolveArtifactUrl(repositoryId: String, filePath: String?): String {
        return if (!filePath.isNullOrBlank()) {
            "https://huggingface.co/$repositoryId/blob/main/$filePath"
        } else {
            "https://huggingface.co/$repositoryId"
        }
    }
}
