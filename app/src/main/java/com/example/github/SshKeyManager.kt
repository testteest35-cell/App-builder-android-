package com.example.github

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.interfaces.RSAPublicKey

data class SshKeyInfo(
    val publicKeyString: String,
    val fingerprint: String,
    val createdAt: String
)

class SshKeyManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("droidide_ssh", Context.MODE_PRIVATE)

    fun getExistingKey(): SshKeyInfo? {
        val pub = prefs.getString("ssh_public_key", null) ?: return null
        val fp = prefs.getString("ssh_fingerprint", "Unknown") ?: "Unknown"
        val date = prefs.getString("ssh_created_at", "Saved") ?: "Saved"
        return SshKeyInfo(pub, fp, date)
    }

    fun generateNewKeyPair(): SshKeyInfo {
        val kpg = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(2048)
        val keyPair: KeyPair = kpg.generateKeyPair()
        val rsaPub = keyPair.public as RSAPublicKey

        // Encode OpenSSH public key wire format
        val byteOs = ByteArrayOutputStream()
        val dataOs = DataOutputStream(byteOs)

        val header = "ssh-rsa".toByteArray()
        dataOs.writeInt(header.size)
        dataOs.write(header)

        val exp = rsaPub.publicExponent.toByteArray()
        dataOs.writeInt(exp.size)
        dataOs.write(exp)

        val mod = rsaPub.modulus.toByteArray()
        dataOs.writeInt(mod.size)
        dataOs.write(mod)

        val wireBytes = byteOs.toByteArray()
        val b64Key = Base64.encodeToString(wireBytes, Base64.NO_WRAP)
        val openSshPub = "ssh-rsa $b64Key droidide-mobile@android"

        // Calculate SHA-256 fingerprint
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(wireBytes)
        val fingerprint = "SHA256:" + Base64.encodeToString(digest, Base64.NO_WRAP).trimEnd('=')

        val now = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US).format(java.util.Date())

        prefs.edit()
            .putString("ssh_public_key", openSshPub)
            .putString("ssh_fingerprint", fingerprint)
            .putString("ssh_created_at", now)
            .apply()

        return SshKeyInfo(openSshPub, fingerprint, now)
    }

    suspend fun registerWithGitHub(token: String, title: String): Result<String> = withContext(Dispatchers.IO) {
        val keyInfo = getExistingKey() ?: generateNewKeyPair()
        val client = OkHttpClient()

        try {
            val json = JSONObject().apply {
                put("title", title)
                put("key", keyInfo.publicKeyString)
            }
            val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
            val request = Request.Builder()
                .url("https://api.github.com/user/keys")
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/vnd.github+json")
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                Result.success("Successfully registered SSH key to GitHub account!")
            } else {
                val errBody = response.body?.string().orEmpty()
                Result.failure(Exception("HTTP ${response.code}: $errBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
