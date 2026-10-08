package com.example.build.remote

import android.content.Context
import com.example.core.model.BuildResult
import com.example.core.model.BuildStep
import com.example.core.model.Project
import com.example.core.model.StepState
import com.example.importexport.ImportExportManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class RemoteBuildClient(private val context: Context) {

    private val importExportManager = ImportExportManager(context)

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(180, TimeUnit.SECONDS)
        .build()

    fun executeRemoteBuild(
        serverBaseUrl: String,
        project: Project
    ): Flow<Pair<List<BuildStep>, BuildResult?>> = flow {
        val steps = listOf(
            BuildStep("1_archive", "Package Source", "Compressing project archive for upload"),
            BuildStep("2_upload", "Upload to Server", "Transmitting project to build cluster"),
            BuildStep("3_remote_build", "Gradle Compilation", "Compiling Kotlin & assembling APK on server"),
            BuildStep("4_download", "Download Artifact", "Fetching built APK artifact from server")
        ).toMutableList()

        val logs = mutableListOf<String>()
        val startTime = System.currentTimeMillis()

        logs.add("[RemoteBuild] Initiating remote build on $serverBaseUrl")
        logs.add("[RemoteBuild] Target project: ${project.name} (${project.packageName})")
        emit(steps.toList() to null)

        // Step 1: Package
        steps[0] = steps[0].copy(state = StepState.RUNNING)
        emit(steps.toList() to null)
        val zipResult = importExportManager.exportProjectToZip(project)
        if (zipResult.isFailure) {
            steps[0] = steps[0].copy(state = StepState.FAILED, errorOutput = zipResult.exceptionOrNull()?.message)
            emit(steps.toList() to BuildResult(false, null, 0, logs, "Failed to compress project"))
            return@flow
        }
        val zipFile = zipResult.getOrThrow()
        steps[0] = steps[0].copy(state = StepState.SUCCESS, durationMs = 250)
        logs.add("[RemoteBuild] Source zipped: ${zipFile.length()} bytes")
        emit(steps.toList() to null)

        // Step 2: Upload
        steps[1] = steps[1].copy(state = StepState.RUNNING)
        emit(steps.toList() to null)

        val cleanUrl = serverBaseUrl.trimEnd('/')
        var jobId: String? = null
        try {
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("projectName", project.name)
                .addFormDataPart("packageName", project.packageName)
                .addFormDataPart(
                    "projectZip",
                    zipFile.name,
                    zipFile.asRequestBody("application/zip".toMediaTypeOrNull())
                )
                .build()

            val request = Request.Builder()
                .url("$cleanUrl/api/v1/build")
                .post(requestBody)
                .build()

            val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
            if (!response.isSuccessful) {
                throw Exception("Server rejected build upload: HTTP ${response.code} ${response.message}")
            }
            val responseJson = JSONObject(response.body?.string().orEmpty())
            jobId = responseJson.optString("jobId")
            logs.add("[RemoteBuild] Upload successful! Assigned Job ID: $jobId")
            steps[1] = steps[1].copy(state = StepState.SUCCESS, durationMs = 500)
            emit(steps.toList() to null)
        } catch (e: Exception) {
            logs.add("[RemoteBuild] Server upload failed: ${e.message}")
            steps[1] = steps[1].copy(state = StepState.FAILED, errorOutput = e.message)
            emit(steps.toList() to BuildResult(false, null, 0, logs, e.message))
            return@flow
        }

        // Step 3: Poll Remote Build
        steps[2] = steps[2].copy(state = StepState.RUNNING)
        emit(steps.toList() to null)

        var buildCompleted = false
        var buildFailed = false
        var pollAttempts = 0
        while (!buildCompleted && !buildFailed && pollAttempts < 60) {
            delay(1500)
            pollAttempts++
            try {
                val pollReq = Request.Builder().url("$cleanUrl/api/v1/build/$jobId/status").get().build()
                val pollRes = withContext(Dispatchers.IO) { client.newCall(pollReq).execute() }
                if (pollRes.isSuccessful) {
                    val statusObj = JSONObject(pollRes.body?.string().orEmpty())
                    val status = statusObj.optString("status")
                    val currentLogs = statusObj.optJSONArray("logs")
                    if (currentLogs != null) {
                        for (i in 0 until currentLogs.length()) {
                            val line = currentLogs.getString(i)
                            if (!logs.contains(line)) logs.add(line)
                        }
                    }

                    if (status.equals("SUCCESS", ignoreCase = true)) {
                        buildCompleted = true
                    } else if (status.equals("FAILED", ignoreCase = true)) {
                        buildFailed = true
                    }
                }
            } catch (e: Exception) {
                logs.add("Polling attempt $pollAttempts: ${e.message}")
            }
        }

        if (buildFailed || !buildCompleted) {
            steps[2] = steps[2].copy(state = StepState.FAILED, errorOutput = "Remote compilation failed on server")
            emit(steps.toList() to BuildResult(false, null, 0, logs, "Remote build failed"))
            return@flow
        }
        steps[2] = steps[2].copy(state = StepState.SUCCESS, durationMs = 3500)
        emit(steps.toList() to null)

        // Step 4: Download APK
        steps[3] = steps[3].copy(state = StepState.RUNNING)
        emit(steps.toList() to null)

        val outputDir = File(project.rootDir, "build/outputs/apk/debug")
        if (!outputDir.exists()) outputDir.mkdirs()
        val destApk = File(outputDir, "${project.name.lowercase()}-remote-debug.apk")

        try {
            val downloadReq = Request.Builder().url("$cleanUrl/api/v1/build/$jobId/download").get().build()
            val downloadRes = withContext(Dispatchers.IO) { client.newCall(downloadReq).execute() }
            if (downloadRes.isSuccessful && downloadRes.body != null) {
                FileOutputStream(destApk).use { fos ->
                    downloadRes.body!!.byteStream().copyTo(fos)
                }
                steps[3] = steps[3].copy(state = StepState.SUCCESS, durationMs = 800)
                logs.add("[RemoteBuild] Downloaded APK: ${destApk.name} (${destApk.length()} bytes)")
                val totalTime = System.currentTimeMillis() - startTime
                val result = BuildResult(true, destApk, totalTime, logs)
                emit(steps.toList() to result)
            } else {
                throw Exception("Could not download APK artifact from server")
            }
        } catch (e: Exception) {
            steps[3] = steps[3].copy(state = StepState.FAILED, errorOutput = e.message)
            emit(steps.toList() to BuildResult(false, null, 0, logs, e.message))
        }
    }
}
