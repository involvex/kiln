package dev.involvex.kiln.core.model

/** One row on the Freeze / hibernate screen. */
data class FreezeApp(val packageName: String, val label: String, val isSystem: Boolean, val isFrozen: Boolean)

enum class FreezeListFilter {
    ALL,
    FROZEN,
    ACTIVE,
}

object FreezeGate {
    fun canMutate(capabilities: Set<PrivilegeCapability>): Boolean = PrivilegeCapability.DEBLOAT in capabilities
}

object FreezeListFilterLogic {
    fun filter(apps: List<FreezeApp>, query: String, freezeFilter: FreezeListFilter): List<FreezeApp> {
        val q = query.trim()
        return apps.asSequence()
            .filter { app ->
                q.isEmpty() ||
                    app.label.contains(q, ignoreCase = true) ||
                    app.packageName.contains(q, ignoreCase = true)
            }
            .filter { app ->
                when (freezeFilter) {
                    FreezeListFilter.ALL -> true
                    FreezeListFilter.FROZEN -> app.isFrozen
                    FreezeListFilter.ACTIVE -> !app.isFrozen
                }
            }
            .toList()
    }
}
