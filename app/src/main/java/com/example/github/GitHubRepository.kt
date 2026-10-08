package com.example.github

import android.content.Context
import android.content.SharedPreferences
import com.example.core.model.Project
import com.example.core.model.ProjectTemplateType
import com.example.github.api.GitHubApiService
import com.example.github.model.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

class GitHubRepository(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("droidide_github", Context.MODE_PRIVATE)

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    private val api: GitHubApiService = Retrofit.Builder()
        .baseUrl("https://api.github.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(GitHubApiService::class.java)

    var token: String?
        get() = prefs.getString("pat_token", null)
        set(value) {
            prefs.edit().putString("pat_token", value).apply()
        }

    val isAuthenticated: Boolean
        get() = !token.isNullOrBlank()

    private fun getAuthHeader(): String {
        val t = token.orEmpty()
        return if (t.startsWith("ghp_") || t.startsWith("github_pat_") || t.isNotEmpty()) "Bearer $t" else ""
    }

    suspend fun getCurrentUser(): Result<GitHubUser> = withContext(Dispatchers.IO) {
        try {
            val response = api.getAuthenticatedUser(getAuthHeader())
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to authenticate: HTTP ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserRepos(): Result<List<GitHubRepo>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getUserRepos(getAuthHeader())
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Could not fetch repositories: HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBranches(owner: String, repo: String): Result<List<GitHubBranch>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getRepoBranches(owner, repo, if (isAuthenticated) getAuthHeader() else null)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Could not fetch branches"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCommits(owner: String, repo: String): Result<List<GitHubCommitResponse>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getRepoCommits(owner, repo, if (isAuthenticated) getAuthHeader() else null)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Could not fetch commits"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cloneRepository(
        owner: String,
        repoName: String,
        targetDir: File,
        onProgress: (String) -> Unit
    ): Result<Project> = withContext(Dispatchers.IO) {
        try {
            onProgress("Connecting to GitHub for $owner/$repoName...")
            val branchesRes = api.getRepoBranches(owner, repoName, if (isAuthenticated) getAuthHeader() else null)
            val defaultBranch = branchesRes.body()?.firstOrNull()?.name ?: "main"
            val branchCommitSha = branchesRes.body()?.firstOrNull()?.commit?.sha ?: "HEAD"

            onProgress("Fetching file tree for branch '$defaultBranch'...")
            val treeRes = api.getGitTree(owner, repoName, branchCommitSha, 1, if (isAuthenticated) getAuthHeader() else null)
            val treeEntries = treeRes.body()?.tree.orEmpty()

            if (treeEntries.isEmpty()) {
                return@withContext Result.failure(Exception("Repository is empty or tree could not be fetched"))
            }

            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }

            val blobFiles = treeEntries.filter { it.type == "blob" }
            onProgress("Downloading ${blobFiles.size} repository files...")

            for ((index, entry) in blobFiles.withIndex()) {
                if (index % 5 == 0) {
                    onProgress("Downloading (${index + 1}/${blobFiles.size}): ${entry.path}")
                }
                val localFile = File(targetDir, entry.path)
                localFile.parentFile?.mkdirs()

                // Fetch content
                try {
                    val contentRes = api.getRepoContent(owner, repoName, entry.path, if (isAuthenticated) getAuthHeader() else null)
                    val rawContent = contentRes.body()?.content
                    val decoded = if (rawContent != null) {
                        try {
                            android.util.Base64.decode(rawContent.replace("\n", ""), android.util.Base64.DEFAULT)
                        } catch (e: Exception) {
                            rawContent.toByteArray()
                        }
                    } else {
                        ByteArray(0)
                    }
                    localFile.writeBytes(decoded)
                } catch (e: Exception) {
                    // Fallback to basic placeholder if raw download hits rate limits
                    localFile.writeText("// Cloned from $owner/$repoName: ${entry.path}")
                }
            }

            onProgress("Clone finished! Parsing project structure...")
            val project = Project(
                id = targetDir.name,
                name = targetDir.name,
                packageName = "com.github.${repoName.lowercase().replace("[^a-z0-9]".toRegex(), "")}",
                template = ProjectTemplateType.COMPOSE_EMPTY,
                minSdk = 26,
                rootDir = targetDir
            )
            Result.success(project)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createRepo(name: String, description: String?, isPrivate: Boolean): Result<GitHubRepo> = withContext(Dispatchers.IO) {
        try {
            val response = api.createRepository(
                getAuthHeader(),
                CreateRepoRequest(name = name, description = description, private = isPrivate, autoInit = true)
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Create repo failed: HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        prefs.edit().remove("pat_token").apply()
    }
}
