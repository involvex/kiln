package dev.involvex.kiln.feature.apps.data

fun sha256CertificateFingerprint(certificateBytes: ByteArray): String =
    dev.involvex.kiln.core.common.sha256CertificateFingerprint(certificateBytes)
