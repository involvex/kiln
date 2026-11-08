package dev.involvex.kiln.feature.apps.data

import java.security.MessageDigest

/**
 * SHA-256 of a DER-encoded X.509 certificate, uppercase hex with colon separators.
 * Example: `AB:CD:12:…`
 */
fun sha256CertificateFingerprint(certificateBytes: ByteArray): String {
    require(certificateBytes.isNotEmpty()) { "certificate bytes must not be empty" }
    val digest = MessageDigest.getInstance("SHA-256").digest(certificateBytes)
    return digest.joinToString(separator = ":") { byte ->
        "%02X".format(byte)
    }
}
