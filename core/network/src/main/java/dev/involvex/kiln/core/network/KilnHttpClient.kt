package dev.involvex.kiln.core.network

import dev.involvex.kiln.core.common.Result
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber

@Singleton
class KilnHttpClient @Inject constructor(private val okHttpClient: OkHttpClient) {
    suspend fun getBytes(url: String, headers: Map<String, String> = emptyMap()): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            require(url.startsWith("https://")) { "Only HTTPS is allowed" }
            val request = Request.Builder()
                .url(url)
                .apply { headers.forEach { (k, v) -> header(k, v) } }
                .get()
                .build()
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
