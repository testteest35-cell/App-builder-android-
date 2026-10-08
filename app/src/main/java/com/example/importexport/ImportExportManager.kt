package com.example.importexport

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.core.model.Project
import com.example.core.model.ProjectTemplateType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class ImportExportManager(private val context: Context) {

    /**
     * Compresses the entire project into a ZIP archive, excluding build caches.
     */
    suspend fun exportProjectToZip(project: Project): Result<File> = withContext(Dispatchers.IO) {
        try {
            val exportDir = File(context.cacheDir, "exported_projects")
            if (!exportDir.exists()) exportDir.mkdirs()

            val zipFile = File(exportDir, "${project.name}.zip")
            if (zipFile.exists()) zipFile.delete()

            FileOutputStream(zipFile).use { fos ->
                ZipOutputStream(BufferedOutputStream(fos)).use { zos ->
                    zipFolderRecursive(project.rootDir, project.rootDir, zos)
                }
            }
            Result.success(zipFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun zipFolderRecursive(rootDir: File, currentFile: File, zos: ZipOutputStream) {
        val files = currentFile.listFiles() ?: return
        val buffer = ByteArray(8192)

        for (file in files) {
            // Exclude temporary build folders to keep zip clean and compact
            if (file.name == "build" || file.name == ".gradle" || file.name == ".idea" || file.name == ".cxx") {
                continue
            }
            if (file.isDirectory) {
                zipFolderRecursive(rootDir, file, zos)
            } else {
                val relativePath = file.relativeTo(rootDir).path.replace('\\', '/')
                val entry = ZipEntry(relativePath)
                zos.putNextEntry(entry)
                FileInputStream(file).use { fis ->
                    var len: Int
                    while (fis.read(buffer).also { len = it } > 0) {
                        zos.write(buffer, 0, len)
                    }
                }
                zos.closeEntry()
            }
        }
    }

    /**
     * Imports a project from an input stream (e.g. from SAF Uri or local file),
     * protected against Zip Slip attacks.
     */
    suspend fun importProjectFromZip(
        inputStream: InputStream,
        targetProjectsDir: File,
        preferredName: String? = null
    ): Result<Project> = withContext(Dispatchers.IO) {
        try {
            val tempExtractDir = File(context.cacheDir, "unzip_${System.currentTimeMillis()}")
            if (!tempExtractDir.exists()) tempExtractDir.mkdirs()

            val buffer = ByteArray(8192)
            ZipInputStream(BufferedInputStream(inputStream)).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    val newFile = File(tempExtractDir, entry.name)

                    // Security check: Guard against Zip Slip path traversal
                    if (!newFile.canonicalPath.startsWith(tempExtractDir.canonicalPath)) {
                        throw SecurityException("Zip Slip path traversal exploit detected: ${entry.name}")
                    }

                    if (entry.isDirectory) {
                        newFile.mkdirs()
                    } else {
                        newFile.parentFile?.mkdirs()
                        FileOutputStream(newFile).use { fos ->
                            var len: Int
                            while (zis.read(buffer).also { len = it } > 0) {
                                fos.write(buffer, 0, len)
                            }
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            // Determine root folder (check if unzipped into an enclosing single root folder)
            val extractedChildren = tempExtractDir.listFiles() ?: emptyArray()
            val finalSourceDir = if (extractedChildren.size == 1 && extractedChildren[0].isDirectory) {
                extractedChildren[0]
            } else {
                tempExtractDir
            }

            val projectName = preferredName?.replace("[^a-zA-Z0-9_]".toRegex(), "")
                ?: finalSourceDir.name.replace("[^a-zA-Z0-9_]".toRegex(), "").ifEmpty { "ImportedApp" }

            val destinationProjectDir = File(targetProjectsDir, projectName)
            if (destinationProjectDir.exists()) {
                destinationProjectDir.deleteRecursively()
            }
            finalSourceDir.copyRecursively(destinationProjectDir, overwrite = true)
            tempExtractDir.deleteRecursively()

            val project = Project(
                id = projectName,
                name = projectName,
                packageName = "com.imported.${projectName.lowercase()}",
                template = ProjectTemplateType.COMPOSE_EMPTY,
                minSdk = 26,
                rootDir = destinationProjectDir
            )
            Result.success(project)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Saves built APK to public Downloads folder using MediaStore (SAF compliant).
     */
    suspend fun saveApkToDownloads(apkFile: File): Result<Uri?> = withContext(Dispatchers.IO) {
        try {
            if (!apkFile.exists()) {
                return@withContext Result.failure(FileNotFoundException("APK not found"))
            }

            val fileName = apkFile.name
            val resolver = context.contentResolver

            val uri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.android.package-archive")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/DroidIDE")
                }
                val insertUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (insertUri != null) {
                    resolver.openOutputStream(insertUri)?.use { output ->
                        FileInputStream(apkFile).use { input ->
                            input.copyTo(output)
                        }
                    }
                }
                insertUri
            } else {
                val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val target = File(downloads, fileName)
                apkFile.copyTo(target, overwrite = true)
                Uri.fromFile(target)
            }

            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
