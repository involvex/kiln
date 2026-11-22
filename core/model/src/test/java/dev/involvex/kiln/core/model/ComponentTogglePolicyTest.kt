package dev.involvex.kiln.core.model

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ComponentTogglePolicyTest {
    @Test
    fun offerToggle_requires_exported_capability_and_safe_package() {
        val deny = setOf("com.android.launcher3")
        assertTrue(
            ComponentTogglePolicy.canOfferToggle(
                exported = true,
                hasDebloatCapability = true,
                packageName = "com.example.app",
                denylistedPackages = deny,
            ),
        )
        assertFalse(
            ComponentTogglePolicy.canOfferToggle(
                exported = false,
                hasDebloatCapability = true,
                packageName = "com.example.app",
                denylistedPackages = deny,
            ),
        )
        assertFalse(
            ComponentTogglePolicy.canOfferToggle(
                exported = true,
                hasDebloatCapability = false,
                packageName = "com.example.app",
                denylistedPackages = deny,
            ),
        )
        assertFalse(
            ComponentTogglePolicy.canOfferToggle(
                exported = true,
                hasDebloatCapability = true,
                packageName = "com.android.launcher3",
                denylistedPackages = deny,
            ),
        )
        assertFalse(
            ComponentTogglePolicy.canOfferToggle(
                exported = true,
                hasDebloatCapability = true,
                packageName = "com.android.systemui",
                denylistedPackages = emptySet(),
            ),
        )
    }

    @Test
    fun isDenylisted_includes_hard_packages() {
        assertTrue(ComponentTogglePolicy.isDenylisted("com.android.settings", emptySet()))
        assertTrue(ComponentTogglePolicy.isDenylisted("com.foo", setOf("com.foo")))
        assertFalse(ComponentTogglePolicy.isDenylisted("com.example.app", emptySet()))
    }
}
