package dev.involvex.kiln.feature.backup.data

import dev.involvex.kiln.core.common.Result
import dev.involvex.kiln.core.model.BackupPreferences
import dev.involvex.kiln.core.model.BackupTreeEntry
import dev.involvex.kiln.core.model.WebDavPropfindParser
import dev.involvex.kiln.core.model.WebDavSecretRepository
import dev.involvex.kiln.core.model.WebDavUrl
import dev.involvex.kiln.core.network.KilnHttpClient
import dev.involvex.kiln.core.network.KilnHttpResponse
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

@Singleton
class WebDavClient @Inject constructor(
    private val http: KilnHttpClient,
    private val backupPreferences: BackupPreferences,
    private val secrets: WebDavSecretRepository,
) {
    suspend fun testConnection(): Result<Unit> {
        val url = collectionUrl() ?: return Result.failure("webdav_invalid_url")
        return when (val response = propfind(url, depth = 0)) {
            is Result.Failure -> response
            is Result.Success -> mapStatus(response.value.code)
        }
    }

    suspend fun ensurePackageCollection(packageName: String): Result<Unit> {
        val root = collectionUrl() ?: return Result.failure("webdav_invalid_url")
        when (val rootMk = mkcolIfNeeded(root)) {
            is Result.Failure -> return rootMk
            is Result.Success -> Unit
        }
        val pkgUrl = packageCollectionUrl(packageName) ?: return Result.failure("webdav_invalid_url")
        return mkcolIfNeeded(pkgUrl)
    }

    suspend fun putArchive(packageName: String, fileName: String, bytes: ByteArray): Result<Unit> {
        val url = archiveUrl(packageName, fileName) ?: return Result.failure("webdav_invalid_url")
        when (val ensured = ensurePackageCollection(packageName)) {
            is Result.Failure -> return ensured
            is Result.Success -> Unit
        }
        val body = bytes.toRequestBody(OCTET_STREAM)
        return when (val response = http.execute("PUT", url, authHeaders(), body)) {
            is Result.Failure -> response
            is Result.Success -> mapStatus(response.value.code, successCodes = PUT_OK)
        }
    }

    suspend fun listArchives(packageName: String): Result<List<BackupTreeEntry>> {
        val url = packageCollectionUrl(packageName) ?: return Result.failure("webdav_invalid_url")
        return when (val response = propfind(url, depth = 1)) {
            is Result.Failure -> response

            is Result.Success -> {
                val mapped = mapStatus(response.value.code, successCodes = PROPFIND_OK)
                if (mapped is Result.Failure) {
                    mapped
                } else {
                    val xml = response.value.body.toString(Charsets.UTF_8)
                    Result.success(WebDavPropfindParser.archiveEntries(xml))
                }
            }
        }
    }

    suspend fun downloadArchive(hrefOrAbsoluteUrl: String): Result<ByteArray> {
        val absolute = resolveHref(hrefOrAbsoluteUrl) ?: return Result.failure("webdav_invalid_url")
        return when (val response = http.execute("GET", absolute, authHeaders())) {
            is Result.Failure -> response

            is Result.Success -> {
                if (response.value.code !in HTTP_OK_RANGE) {
                    when (val mapped = mapStatus(response.value.code)) {
                        is Result.Failure -> mapped
                        is Result.Success -> Result.failure("webdav_http_${response.value.code}")
                    }
                } else {
                    Result.success(response.value.body)
                }
            }
        }
    }

    suspend fun deleteHref(hrefOrAbsoluteUrl: String): Result<Unit> {
        val absolute = resolveHref(hrefOrAbsoluteUrl) ?: return Result.failure("webdav_invalid_url")
        return when (val response = http.execute("DELETE", absolute, authHeaders())) {
            is Result.Failure -> response
            is Result.Success -> mapStatus(response.value.code, successCodes = DELETE_OK)
        }
    }

    private suspend fun propfind(url: String, depth: Int): Result<KilnHttpResponse> {
        val body = PROPFIND_BODY.toRequestBody(XML)
        val headers = authHeaders() + mapOf(
            "Depth" to depth.toString(),
            "Content-Type" to "application/xml; charset=utf-8",
        )
        return http.execute("PROPFIND", url, headers, body)
    }

    private suspend fun mkcolIfNeeded(url: String): Result<Unit> = when (val response = http.execute("MKCOL", url, authHeaders())) {
        is Result.Failure -> response

        is Result.Success -> {
            if (response.value.code in MKCOL_OK) {
                Result.success(Unit)
            } else {
                mapStatus(response.value.code)
            }
        }
    }

    private suspend fun authHeaders(): Map<String, String> {
        val prefs = backupPreferences.current()
        val user = prefs.webdavUsername.trim()
        val password = secrets.getPassword().orEmpty()
        if (user.isEmpty()) return emptyMap()
        return mapOf("Authorization" to Credentials.basic(user, password))
    }

    private suspend fun collectionUrl(): String? {
        val prefs = backupPreferences.current()
        return WebDavUrl.join(prefs.webdavBaseUrl, prefs.webdavRemotePath)
    }

    private suspend fun packageCollectionUrl(packageName: String): String? {
        val prefs = backupPreferences.current()
        return WebDavUrl.join(prefs.webdavBaseUrl, prefs.webdavRemotePath, packageName)
    }

    private suspend fun archiveUrl(packageName: String, fileName: String): String? {
        val prefs = backupPreferences.current()
        return WebDavUrl.join(prefs.webdavBaseUrl, prefs.webdavRemotePath, packageName, fileName)
    }

    private suspend fun resolveHref(href: String): String? {
        val trimmed = href.trim()
        if (trimmed.startsWith("https://", ignoreCase = true)) return trimmed
        val prefs = backupPreferences.current()
        val base = WebDavUrl.normalizeBaseUrl(prefs.webdavBaseUrl) ?: return null
        val origin = runCatching {
            val uri = URI(base)
            "${uri.scheme}://${uri.authority}"
        }.getOrNull() ?: return null
        val path = if (trimmed.startsWith("/")) trimmed else "/$trimmed"
        return origin + path
    }

    private fun mapStatus(code: Int, successCodes: Set<Int> = DEFAULT_OK): Result<Unit> = when {
        code in successCodes -> Result.success(Unit)
        code == HTTP_UNAUTHORIZED || code == HTTP_FORBIDDEN -> Result.failure("webdav_unauthorized")
        code == HTTP_NOT_FOUND -> Result.failure("webdav_not_found")
        code == HTTP_METHOD_NOT_ALLOWED -> Result.failure("webdav_method_not_allowed")
        else -> Result.failure("webdav_http_$code")
    }

    private companion object {
        val OCTET_STREAM = "application/octet-stream".toMediaType()
        val XML = "application/xml; charset=utf-8".toMediaType()
        const val HTTP_OK = 200
        const val HTTP_CREATED = 201
        const val HTTP_NO_CONTENT = 204
        const val HTTP_MULTI_STATUS = 207
        const val HTTP_UNAUTHORIZED = 401
        const val HTTP_FORBIDDEN = 403
        const val HTTP_NOT_FOUND = 404
        const val HTTP_METHOD_NOT_ALLOWED = 405
        val HTTP_OK_RANGE = HTTP_OK..299
        val DEFAULT_OK = setOf(HTTP_OK, HTTP_MULTI_STATUS, HTTP_CREATED, HTTP_NO_CONTENT)
        val PUT_OK = setOf(HTTP_OK, HTTP_CREATED, HTTP_NO_CONTENT)
        val PROPFIND_OK = setOf(HTTP_MULTI_STATUS, HTTP_OK)
        val DELETE_OK = setOf(HTTP_OK, HTTP_NO_CONTENT, HTTP_NOT_FOUND)
        val MKCOL_OK = setOf(HTTP_OK, HTTP_CREATED, HTTP_METHOD_NOT_ALLOWED)
        const val PROPFIND_BODY =
            """<?xml version="1.0" encoding="utf-8"?>""" +
                """<d:propfind xmlns:d="DAV:"><d:prop>""" +
                """<d:displayname/><d:getlastmodified/><d:resourcetype/>""" +
                """</d:prop></d:propfind>"""
    }
}
