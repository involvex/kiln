package dev.involvex.kiln.feature.apps.apktools

import com.android.apksig.ApkSigner
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate

/**
 * Resigns a single APK with a user-supplied keystore via apksig (Tier NONE).
 * Passwords stay on the call stack; callers must clear [CharArray]s after use.
 */
object ApkResign {
    data class Credentials(val storePassword: CharArray, val keyAlias: String, val keyPassword: CharArray)

    fun listAliases(keystoreStream: InputStream, storePassword: CharArray): kotlin.Result<List<String>> = runCatching {
        val keyStore = loadKeyStore(keystoreStream, storePassword)
        keyStore.aliases().toList().filter { keyStore.isKeyEntry(it) }
    }.fold(
        onSuccess = { kotlin.Result.success(it) },
        onFailure = { kotlin.Result.failure(IllegalStateException(mapKeyStoreError(it))) },
    )

    fun sign(inputApk: File, outputApk: File, keystoreFile: File, credentials: Credentials): kotlin.Result<Unit> {
        val storePassword = credentials.storePassword
        val keyPassword = credentials.keyPassword
        return runCatching {
            require(inputApk.isFile) { "input_missing" }
            keystoreFile.inputStream().use { stream ->
                val keyStore = loadKeyStore(stream, storePassword)
                val alias = credentials.keyAlias.trim()
                require(alias.isNotEmpty()) { "alias_empty" }
                require(keyStore.isKeyEntry(alias)) { "alias_missing" }
                val key = keyStore.getKey(alias, keyPassword) as? PrivateKey
                    ?: error("key_unreadable")
                val chain = keyStore.getCertificateChain(alias)
                    ?.filterIsInstance<X509Certificate>()
                    ?.takeIf { it.isNotEmpty() }
                    ?: error("cert_chain_missing")
                val signerConfig = ApkSigner.SignerConfig.Builder(alias, key, chain).build()
                ApkSigner.Builder(listOf(signerConfig))
                    .setInputApk(inputApk)
                    .setOutputApk(outputApk)
                    .setV1SigningEnabled(true)
                    .setV2SigningEnabled(true)
                    .setV3SigningEnabled(true)
                    .build()
                    .sign()
            }
        }.fold(
            onSuccess = { kotlin.Result.success(Unit) },
            onFailure = { kotlin.Result.failure(IllegalStateException(mapSignError(it))) },
        )
    }

    fun clearCredentials(credentials: Credentials) {
        credentials.storePassword.fill('\u0000')
        credentials.keyPassword.fill('\u0000')
    }

    private fun loadKeyStore(stream: InputStream, storePassword: CharArray): KeyStore {
        val bytes = stream.readBytes()
        return tryLoad("PKCS12", bytes, storePassword)
            ?: tryLoad(KeyStore.getDefaultType(), bytes, storePassword)
            ?: error("keystore_unreadable")
    }

    private fun tryLoad(type: String, bytes: ByteArray, storePassword: CharArray): KeyStore? = runCatching {
        KeyStore.getInstance(type).apply {
            load(ByteArrayInputStream(bytes), storePassword)
        }
    }.getOrNull()

    private fun mapKeyStoreError(error: Throwable): String {
        val message = error.message.orEmpty()
        return when {
            message.contains("password", ignoreCase = true) -> "keystore_password"
            message.contains("Integrity", ignoreCase = true) -> "keystore_password"
            message == "keystore_unreadable" -> "keystore_unreadable"
            else -> "keystore_unreadable"
        }
    }

    private fun mapSignError(error: Throwable): String {
        val message = error.message.orEmpty()
        return when {
            message == "input_missing" -> "input_missing"
            message == "alias_empty" -> "alias_empty"
            message == "alias_missing" -> "alias_missing"
            message == "key_unreadable" -> "key_unreadable"
            message == "cert_chain_missing" -> "cert_chain_missing"
            message == "keystore_unreadable" -> "keystore_unreadable"
            message.contains("password", ignoreCase = true) -> "keystore_password"
            else -> "resign_failed"
        }
    }
}
