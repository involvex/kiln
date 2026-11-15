package dev.involvex.kiln.core.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FreezeListFilterTest {
    private val apps = listOf(
        FreezeApp("com.a", "Alpha", isSystem = false, isFrozen = true),
        FreezeApp("com.b", "Beta", isSystem = false, isFrozen = false),
        FreezeApp("com.c", "Gamma", isSystem = true, isFrozen = true),
    )

    @Test
    fun filter_byQuery() {
        val result = FreezeListFilterLogic.filter(apps, "bet", FreezeListFilter.ALL)
        assertEquals(listOf("com.b"), result.map { it.packageName })
    }

    @Test
    fun filter_frozenOnly() {
        val result = FreezeListFilterLogic.filter(apps, "", FreezeListFilter.FROZEN)
        assertEquals(listOf("com.a", "com.c"), result.map { it.packageName })
    }

    @Test
    fun filter_activeOnly() {
        val result = FreezeListFilterLogic.filter(apps, "", FreezeListFilter.ACTIVE)
        assertEquals(listOf("com.b"), result.map { it.packageName })
    }

    @Test
    fun gate_requiresDebloat() {
        assertEquals(false, FreezeGate.canMutate(emptySet()))
        assertEquals(true, FreezeGate.canMutate(setOf(PrivilegeCapability.DEBLOAT)))
    }
}
