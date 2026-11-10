package dev.involvex.kiln.core.network

import dev.involvex.kiln.core.common.Result
import dev.involvex.kiln.core.model.NetworkProxyPreferences
import dev.involvex.kiln.core.model.NetworkProxyPrefs
import dev.involvex.kiln.core.model.NetworkProxyType
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import timber.log.Timber

@Singleton
class KilnHttpClient @Inject constructor(private val proxyPreferences: NetworkProxyPreferences) {
    private val clientMutex = Mutex()

    @Volatile
    private var cached: Pair<NetworkProxyPrefs, OkHttpClient>? = null

    suspend fun getBytes(url: String, headers: Map<String, String> = emptyMap()): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            requireHttps(url)
            val request = buildGet(url, headers)
            client().newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure("HTTP ${response.code}")
                }
                val body = response.body?.bytes()
                    ?: return@withContext Result.failure("Empty response body")
                Result.success(body)
            }
        } catch (error: IOException) {
            Timber.w(error, "HTTP GET failed")
            Result.failure(error.message ?: "network_error", error)
        } catch (error: IllegalArgumentException) {
            Result.failure(error.message ?: "invalid_url", error)
        }
    }

    suspend fun getText(url: String, headers: Map<String, String> = emptyMap()): Result<String> =
        when (val bytes = getBytes(url, headers)) {
            is Result.Success -> Result.success(bytes.value.toString(Charsets.UTF_8))
            is Result.Failure -> bytes
        }

    /**
     * Downloads [url] into [target], appending when the file already has bytes and the server
     * accepts `Range`. Invokes [onProgress] with bytesWritten / totalBytes (total may be -1).
     */
    suspend fun downloadToFile(
        url: String,
        target: File,
        headers: Map<String, String> = emptyMap(),
        onProgress: (bytesWritten: Long, totalBytes: Long) -> Unit = { _, _ -> },
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            requireHttps(url)
            target.parentFile?.mkdirs()
            val existing = if (target.exists()) target.length() else 0L
            val requestHeaders = headers.toMutableMap()
            if (existing > 0L) {
                requestHeaders["Range"] = "bytes=$existing-"
            }
            client().newCall(buildGet(url, requestHeaders)).execute().use { response ->
                handleDownloadResponse(response, target, existing, onProgress)
            }
        } catch (error: IOException) {
            Timber.w(error, "HTTP download failed")
            Result.failure(error.message ?: "network_error", error)
        } catch (error: IllegalArgumentException) {
            Result.failure(error.message ?: "invalid_url", error)
        }
    }

    /**
     * Tries each URL in order until one download succeeds. Deletes [target] between attempts
     * so partial Range state does not confuse a different mirror.
     */
    suspend fun downloadToFileFirstSuccess(
        urls: List<String>,
        target: File,
        headers: Map<String, String> = emptyMap(),
        onProgress: (bytesWritten: Long, totalBytes: Long) -> Unit = { _, _ -> },
    ): Result<File> {
        if (urls.isEmpty()) return Result.failure("no_urls")
        var lastFailure: Result.Failure? = null
        for ((index, url) in urls.withIndex()) {
            if (index > 0 && target.exists()) {
                target.delete()
            }
            when (val result = downloadToFile(url, target, headers, onProgress)) {
                is Result.Success -> return result

                is Result.Failure -> {
                    lastFailure = result
                    Timber.w("Download failed for mirror candidate (%s)", result.message)
                }
            }
        }
        return lastFailure ?: Result.failure("network_error")
    }

    /**
     * Tries each URL until [block] returns Success (e.g. entry.jar + verify).
     */
    suspend fun <T> firstSuccessfulUrl(urls: List<String>, block: suspend (url: String) -> Result<T>): Result<T> {
        if (urls.isEmpty()) return Result.failure("no_urls")
        var lastFailure: Result.Failure? = null
        for (url in urls) {
            when (val result = block(url)) {
                is Result.Success -> return result
                is Result.Failure -> lastFailure = result
            }
        }
        return lastFailure ?: Result.failure("network_error")
    }

    private suspend fun handleDownloadResponse(
        response: Response,
        target: File,
        existing: Long,
        onProgress: (Long, Long) -> Unit,
    ): Result<File> {
        if (!response.isSuccessful && response.code != HTTP_PARTIAL) {
            return Result.failure("HTTP ${response.code}")
        }
        val body = response.body ?: return Result.failure("Empty response body")
        val resume = response.code == HTTP_PARTIAL && existing > 0L
        val totalBytes = resolveTotalBytes(response, existing, resume)
        writeDownloadBody(body.byteStream(), target, existing, resume, totalBytes, onProgress)
        return Result.success(target)
    }

    private fun resolveTotalBytes(response: Response, existing: Long, resume: Boolean): Long {
        val totalFromHeader = response.header("Content-Length")?.toLongOrNull() ?: return -1L
        return if (resume) existing + totalFromHeader else totalFromHeader
    }

    private suspend fun writeDownloadBody(
        input: java.io.InputStream,
        target: File,
        existing: Long,
        resume: Boolean,
        totalBytes: Long,
        onProgress: (Long, Long) -> Unit,
    ) {
        RandomAccessFile(target, "rw").use { raf ->
            if (resume) {
                raf.seek(existing)
            } else {
                raf.setLength(0)
            }
            var written = if (resume) existing else 0L
            val buffer = ByteArray(DEFAULT_BUFFER)
            input.use { stream ->
                while (true) {
                    coroutineContext.ensureActive()
                    val read = stream.read(buffer)
                    if (read < 0) break
                    raf.write(buffer, 0, read)
                    written += read
                    onProgress(written, totalBytes)
                }
            }
        }
    }

    private fun buildGet(url: String, headers: Map<String, String>): Request {
        val builder = Request.Builder().url(url).get()
        headers.forEach { (key, value) -> builder.header(key, value) }
        return builder.build()
    }

    private fun requireHttps(url: String) {
        require(url.startsWith("https://")) { "Only HTTPS is allowed" }
    }

    private suspend fun client(): OkHttpClient {
        val prefs = proxyPreferences.current()
        cached?.let { (cachedPrefs, cachedClient) ->
            if (cachedPrefs == prefs) return cachedClient
        }
        return clientMutex.withLock {
            cached?.let { (cachedPrefs, cachedClient) ->
                if (cachedPrefs == prefs) return cachedClient
            }
            val built = NetworkDefaults.okHttpClient(prefs)
            cached = prefs to built
            built
        }
    }

    private companion object {
        const val HTTP_PARTIAL = 206
        const val DEFAULT_BUFFER = 8 * 1024
    }
}

object NetworkDefaults {
    private const val CONNECT_TIMEOUT_SECONDS = 30L

    // F-Droid index-v2.json is ~70MB; allow slow mobile links to finish.
    private const val READ_TIMEOUT_SECONDS = 300L
    private const val CALL_TIMEOUT_SECONDS = 600L

    fun okHttpClient(proxyPrefs: NetworkProxyPrefs = NetworkProxyPrefs()): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .followRedirects(true)
        if (proxyPrefs.isActive()) {
            val proxyType = when (proxyPrefs.effectiveType()) {
                NetworkProxyType.HTTP -> Proxy.Type.HTTP
                NetworkProxyType.SOCKS -> Proxy.Type.SOCKS
                NetworkProxyType.NONE -> null
            }
            if (proxyType != null) {
                val address = InetSocketAddress(proxyPrefs.effectiveHost(), proxyPrefs.effectivePort())
                builder.proxy(Proxy(proxyType, address))
                val user = proxyPrefs.username.trim()
                val pass = proxyPrefs.password
                if (user.isNotEmpty()) {
                    builder.proxyAuthenticator { _, response ->
                        if (response.request.header("Proxy-Authorization") != null) {
                            null
                        } else {
                            response.request.newBuilder()
                                .header("Proxy-Authorization", Credentials.basic(user, pass))
                                .build()
                        }
                    }
                }
            }
        }
        return builder.build()
    }
}
