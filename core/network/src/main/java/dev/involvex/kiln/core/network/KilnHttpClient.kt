package dev.involvex.kiln.core.network

import dev.involvex.kiln.core.common.Result
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import timber.log.Timber

@Singleton
class KilnHttpClient @Inject constructor(private val okHttpClient: OkHttpClient) {
    suspend fun getBytes(url: String, headers: Map<String, String> = emptyMap()): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            requireHttps(url)
            val request = buildGet(url, headers)
            okHttpClient.newCall(request).execute().use { response ->
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
            okHttpClient.newCall(buildGet(url, requestHeaders)).execute().use { response ->
                handleDownloadResponse(response, target, existing, onProgress)
            }
        } catch (error: IOException) {
            Timber.w(error, "HTTP download failed")
            Result.failure(error.message ?: "network_error", error)
        } catch (error: IllegalArgumentException) {
            Result.failure(error.message ?: "invalid_url", error)
        }
    }

    private suspend fun handleDownloadResponse(
        response: Response,
        target: File,
        existing: Long,
        onProgress: (Long, Long) -> Unit,
    ): Result<File> {
        if (response.code == HTTP_PARTIAL && existing > 0L) {
            return appendBody(response, target, existing, onProgress)
        }
        if (!response.isSuccessful) {
            return Result.failure("HTTP ${response.code}")
        }
        if (existing > 0L) {
            target.delete()
        }
        return writeBody(response, target, onProgress)
    }

    private fun buildGet(url: String, headers: Map<String, String>): Request = Request.Builder()
        .url(url)
        .apply { headers.forEach { (k, v) -> header(k, v) } }
        .get()
        .build()

    private fun writeBody(response: Response, target: File, onProgress: (Long, Long) -> Unit): Result<File> {
        val body = response.body ?: return Result.failure("Empty response body")
        val total = body.contentLength()
        var written = 0L
        body.byteStream().use { input ->
            target.outputStream().use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    output.write(buffer, 0, read)
                    written += read
                    onProgress(written, total)
                }
            }
        }
        return Result.success(target)
    }

    private suspend fun appendBody(response: Response, target: File, existing: Long, onProgress: (Long, Long) -> Unit): Result<File> {
        val body = response.body ?: return Result.failure("Empty response body")
        val contentLength = body.contentLength()
        val total = if (contentLength >= 0) existing + contentLength else -1L
        var written = existing
        body.byteStream().use { input ->
            RandomAccessFile(target, "rw").use { raf ->
                raf.seek(existing)
                val buffer = ByteArray(DEFAULT_BUFFER)
                while (true) {
                    coroutineContext.ensureActive()
                    val read = input.read(buffer)
                    if (read < 0) break
                    raf.write(buffer, 0, read)
                    written += read
                    onProgress(written, total)
                }
            }
        }
        return Result.success(target)
    }

    private fun requireHttps(url: String) {
        require(url.startsWith("https://")) { "Only HTTPS is allowed" }
    }

    private companion object {
        const val HTTP_PARTIAL = 206
        const val DEFAULT_BUFFER = 8 * 1024
    }
}

object NetworkDefaults {
    private const val CONNECT_TIMEOUT_SECONDS = 30L
    private const val READ_TIMEOUT_SECONDS = 120L
    private const val CALL_TIMEOUT_SECONDS = 180L

    fun okHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
}
