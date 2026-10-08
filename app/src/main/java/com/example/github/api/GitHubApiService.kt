package com.example.github.api

import com.example.github.model.*
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface GitHubApiService {

    @GET("user")
    suspend fun getAuthenticatedUser(
        @Header("Authorization") authHeader: String
    ): Response<GitHubUser>

    @GET("user/repos")
    suspend fun getUserRepos(
        @Header("Authorization") authHeader: String,
        @Query("sort") sort: String = "updated",
        @Query("per_page") perPage: Int = 30
    ): Response<List<GitHubRepo>>

    @GET("repos/{owner}/{repo}/branches")
    suspend fun getRepoBranches(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Header("Authorization") authHeader: String? = null
    ): Response<List<GitHubBranch>>

    @GET("repos/{owner}/{repo}/commits")
    suspend fun getRepoCommits(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Header("Authorization") authHeader: String? = null,
        @Query("per_page") perPage: Int = 20
    ): Response<List<GitHubCommitResponse>>

    @GET("repos/{owner}/{repo}/git/trees/{tree_sha}")
    suspend fun getGitTree(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("tree_sha") treeSha: String,
        @Query("recursive") recursive: Int = 1,
        @Header("Authorization") authHeader: String? = null
    ): Response<GitHubTreeResponse>

    @GET("repos/{owner}/{repo}/contents/{path}")
    suspend fun getRepoContent(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path") path: String,
        @Header("Authorization") authHeader: String? = null
    ): Response<GitHubContentItem>

    @Streaming
    @GET
    suspend fun downloadRawFile(@Url fileUrl: String): Response<ResponseBody>

    @POST("user/repos")
    suspend fun createRepository(
        @Header("Authorization") authHeader: String,
        @Body request: CreateRepoRequest
    ): Response<GitHubRepo>
}
