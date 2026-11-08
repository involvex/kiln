package dev.involvex.kiln.feature.termux.run

/**
 * Standard Termux private data paths for a given [packageId].
 * Forks that change applicationId keep the `/data/data/{id}/files/…` layout.
 */
object TermuxPaths {
    fun filesDir(packageId: String): String = "/data/data/$packageId/files"

    fun prefixDir(packageId: String): String = "${filesDir(packageId)}/usr"

    fun binDir(packageId: String): String = "${prefixDir(packageId)}/bin"

    fun homeDir(packageId: String): String = "${filesDir(packageId)}/home"

    fun executable(packageId: String, binaryName: String): String = "${binDir(packageId)}/$binaryName"
}
