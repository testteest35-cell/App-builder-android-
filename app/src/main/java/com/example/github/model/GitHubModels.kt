package com.example.github.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GitHubUser(
    val login: String,
    val id: Long,
    @Json(name = "avatar_url") val avatarUrl: String?,
    val name: String?,
    @Json(name = "public_repos") val publicRepos: Int = 0,
    val bio: String?
)

@JsonClass(generateAdapter = true)
data class GitHubRepo(
    val id: Long,
    val name: String,
    @Json(name = "full_name") val fullName: String,
    val private: Boolean,
    val description: String?,
    @Json(name = "default_branch") val defaultBranch: String = "main",
    @Json(name = "html_url") val htmlUrl: String?,
    @Json(name = "updated_at") val updatedAt: String?
)

@JsonClass(generateAdapter = true)
data class GitHubBranch(
    val name: String,
    val commit: GitHubBranchCommit
)

@JsonClass(generateAdapter = true)
data class GitHubBranchCommit(
    val sha: String,
    val url: String
)

@JsonClass(generateAdapter = true)
data class GitHubCommitResponse(
    val sha: String,
    val commit: GitHubCommitDetail,
    val author: GitHubCommitAuthor?
)

@JsonClass(generateAdapter = true)
data class GitHubCommitDetail(
    val message: String,
    val author: GitHubAuthorDetail
)

@JsonClass(generateAdapter = true)
data class GitHubAuthorDetail(
    val name: String,
    val email: String,
    val date: String
)

@JsonClass(generateAdapter = true)
data class GitHubCommitAuthor(
    val login: String,
    @Json(name = "avatar_url") val avatarUrl: String?
)

@JsonClass(generateAdapter = true)
data class GitHubTreeResponse(
    val sha: String,
    val tree: List<GitHubTreeEntry>,
    val truncated: Boolean = false
)

@JsonClass(generateAdapter = true)
data class GitHubTreeEntry(
    val path: String,
    val mode: String,
    val type: String, // "blob" or "tree"
    val sha: String,
    val size: Long? = null,
    val url: String
)

@JsonClass(generateAdapter = true)
data class GitHubContentItem(
    val name: String,
    val path: String,
    val sha: String,
    val size: Long,
    val type: String, // "file" or "dir"
    val content: String? = null,
    val encoding: String? = null,
    @Json(name = "download_url") val downloadUrl: String?
)

@JsonClass(generateAdapter = true)
data class CreateRepoRequest(
    val name: String,
    val description: String? = null,
    val private: Boolean = false,
    @Json(name = "auto_init") val autoInit: Boolean = false
)
