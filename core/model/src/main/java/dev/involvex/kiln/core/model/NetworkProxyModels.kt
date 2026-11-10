package dev.involvex.kiln.core.model

import kotlinx.coroutines.flow.Flow

enum class NetworkProxyType {
    NONE,
    HTTP,
    SOCKS,
}

/**
 * Global outbound proxy. [orbotPreset] forces SOCKS to 127.0.0.1:9050 (Orbot default).
 * Password is never exported in Kiln-Config.
 */
data class NetworkProxyPrefs(
    val type: NetworkProxyType = NetworkProxyType.NONE,
    val host: String = "",
    val port: Int = 0,
    val username: String = "",
    val password: String = "",
    val orbotPreset: Boolean = false,
) {
    fun effectiveType(): NetworkProxyType = when {
        orbotPreset -> NetworkProxyType.SOCKS
        else -> type
    }

    fun effectiveHost(): String = when {
        orbotPreset -> ORBOT_HOST
        else -> host.trim()
    }

    fun effectivePort(): Int = when {
        orbotPreset -> ORBOT_SOCKS_PORT
        port in 1..MAX_TCP_PORT -> port
        else -> 0
    }

    fun isActive(): Boolean {
        val t = effectiveType()
        if (t == NetworkProxyType.NONE) return false
        return effectiveHost().isNotEmpty() && effectivePort() in 1..MAX_TCP_PORT
    }

    companion object {
        const val ORBOT_HOST = "127.0.0.1"
        const val ORBOT_SOCKS_PORT = 9050
        const val MAX_TCP_PORT = 65535
    }
}

interface NetworkProxyPreferences {
    fun observe(): Flow<NetworkProxyPrefs>

    suspend fun current(): NetworkProxyPrefs

    suspend fun setType(type: NetworkProxyType)

    suspend fun setHost(host: String)

    suspend fun setPort(port: Int)

    suspend fun setUsername(username: String)

    suspend fun setPassword(password: String)

    suspend fun setOrbotPreset(enabled: Boolean)
}
