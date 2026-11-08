package dev.involvex.kiln.core.model

/**
 * Shared domain primitives land here from M1 onward.
 * M0 only seeds the module so feature modules can depend on it safely.
 */
data class PackageId(val value: String) {
    init {
        require(value.isNotBlank()) { "package id must not be blank" }
    }
}
