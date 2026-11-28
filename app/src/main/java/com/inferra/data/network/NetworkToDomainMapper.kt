package com.inferra.data.network

import com.inferra.domain.model.FileTreeItem
import com.inferra.domain.model.LfsInfo
import com.inferra.domain.model.RepoItem
import com.inferra.domain.model.RepoType
import com.inferra.domain.model.UserProfile
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

object NetworkToDomainMapper {

    fun mapModelToRepoItem(dto: HuggingFaceModelDto): RepoItem {
        val fullId = dto.id
        val parts = fullId.split("/")
        val authorStr = dto.author ?: if (parts.size > 1) parts[0] else "Community"
        val modelNameStr = if (parts.size > 1) parts[1] else fullId

        val licenseName = extractLicenseName(dto.tags, dto.cardData)

        return RepoItem(
            id = fullId,
            name = modelNameStr,
            author = authorStr,
            type = RepoType.MODEL,
            description = dto.pipelineTag?.let { "Pipeline: $it" } ?: dto.libraryName?.let { "Library: $it" },
            downloads = dto.downloads ?: 0L,
            likes = dto.likes ?: 0L,
            lastModified = dto.lastModified?.take(10),
            createdAt = dto.createdAt?.take(10),
            isPrivate = dto.isPrivate == true,
            isGated = dto.gated != null,
            pipelineTag = dto.pipelineTag,
            libraryName = dto.libraryName,
            tags = dto.tags ?: emptyList(),
            license = licenseName,
            avatarUrl = "https://huggingface.co/avatars/${authorStr.take(1).uppercase()}.png"
        )
    }

    fun mapDatasetToRepoItem(dto: HuggingFaceDatasetDto): RepoItem {
        val fullId = dto.id
        val parts = fullId.split("/")
        val authorStr = dto.author ?: if (parts.size > 1) parts[0] else "Community"
        val datasetNameStr = if (parts.size > 1) parts[1] else fullId

        val licenseName = extractLicenseName(dto.tags, dto.cardData)

        return RepoItem(
            id = fullId,
            name = datasetNameStr,
            author = authorStr,
            type = RepoType.DATASET,
            downloads = dto.downloads ?: 0L,
            likes = dto.likes ?: 0L,
            lastModified = dto.lastModified?.take(10),
            createdAt = dto.createdAt?.take(10),
            isPrivate = dto.isPrivate == true,
            isGated = dto.gated != null,
            tags = dto.tags ?: emptyList(),
            license = licenseName,
            avatarUrl = "https://huggingface.co/avatars/${authorStr.take(1).uppercase()}.png"
        )
    }

    fun mapSpaceToRepoItem(dto: HuggingFaceSpaceDto): RepoItem {
        val fullId = dto.id
        val parts = fullId.split("/")
        val authorStr = dto.author ?: if (parts.size > 1) parts[0] else "Community"
        val spaceNameStr = if (parts.size > 1) parts[1] else fullId

        val licenseName = extractLicenseName(dto.tags, dto.cardData)

        return RepoItem(
            id = fullId,
            name = spaceNameStr,
            author = authorStr,
            type = RepoType.SPACE,
            description = dto.sdk?.let { "SDK: $it" },
            downloads = dto.downloads ?: 0L,
            likes = dto.likes ?: 0L,
            lastModified = dto.lastModified?.take(10),
            createdAt = dto.createdAt?.take(10),
            isPrivate = dto.isPrivate == true,
            isGated = dto.gated != null,
            sdk = dto.sdk,
            tags = dto.tags ?: emptyList(),
            license = licenseName,
            avatarUrl = "https://huggingface.co/avatars/${authorStr.take(1).uppercase()}.png"
        )
    }

    fun mapFileTreeItem(dto: HfFileTreeItemDto): FileTreeItem {
        val lfsInfo = if (dto.lfs != null && dto.lfs is JsonObject) {
            val obj = dto.lfs as JsonObject
            LfsInfo(
                oid = obj["oid"]?.jsonPrimitive?.content,
                size = obj["size"]?.jsonPrimitive?.content?.toLongOrNull(),
                pointerSize = obj["pointerSize"]?.jsonPrimitive?.content?.toLongOrNull()
            )
        } else null

        return FileTreeItem(
            path = dto.path,
            type = dto.type,
            size = dto.size ?: lfsInfo?.size,
            lfs = lfsInfo,
            sha256 = lfsInfo?.oid ?: dto.oid
        )
    }

    fun mapWhoamiToUserProfile(dto: HfWhoamiResponseDto): UserProfile {
        return UserProfile(
            username = dto.name ?: "Anonymous",
            fullname = dto.fullname,
            avatarUrl = dto.avatarUrl,
            isOrg = dto.type == "org",
            isPro = dto.isPro,
            email = dto.email
        )
    }

    private fun extractLicenseName(tags: List<String>?, cardData: HfCardDataDto?): String? {
        val licTag = tags?.find { it.startsWith("license:") }?.removePrefix("license:")
        if (!licTag.isNullOrBlank()) return licTag
        val cardLic = cardData?.license?.jsonPrimitive?.content
        if (!cardLic.isNullOrBlank()) return cardLic
        return null
    }
}
