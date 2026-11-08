package dev.involvex.kiln.core.model

/** Curated rating from debloat-db.json. Never invent values in code. */
enum class DebloatRating {
    RECOMMENDED,
    ADVANCED,
    EXPERT,
    UNSAFE,
    UNKNOWN,
}

enum class DebloatPackageState {
    ENABLED,
    DISABLED,
    UNINSTALLED_FOR_USER,
}

enum class DebloatAction {
    DISABLE,
    ENABLE,
    UNINSTALL_FOR_USER,
    INSTALL_EXISTING,
}

data class DebloatCatalogEntry(
    val packageName: String,
    val label: String?,
    val vendor: String?,
    val category: String?,
    val rating: DebloatRating,
    val description: String?,
    val breaks: List<String> = emptyList(),
    val minSdk: Int = 0,
    val maxSdk: Int = 99,
)

data class DebloatAppRow(
    val packageName: String,
    val label: String,
    val isSystem: Boolean,
    val state: DebloatPackageState,
    val rating: DebloatRating,
    val category: String?,
    val description: String?,
    val breaks: List<String>,
    val denylisted: Boolean,
    val denylistReason: String?,
)

data class DebloatSnapshotPackage(val packageName: String, val state: DebloatPackageState)

data class DebloatSnapshot(val createdAtEpochMs: Long, val packages: List<DebloatSnapshotPackage>)

data class DebloatActionResult(val packageName: String, val action: DebloatAction, val success: Boolean, val message: String?)
