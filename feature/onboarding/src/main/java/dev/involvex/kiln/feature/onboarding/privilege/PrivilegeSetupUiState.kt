package dev.involvex.kiln.feature.onboarding.privilege

import dev.involvex.kiln.core.model.PrivilegeCapability
import dev.involvex.kiln.core.model.PrivilegeStatus
import dev.involvex.kiln.core.model.PrivilegeTier
import dev.involvex.kiln.core.model.ShizukuSetupPhase

data class PrivilegeSetupUiState(
    val status: PrivilegeStatus = PrivilegeStatus(
        phase = ShizukuSetupPhase.NOT_INSTALLED,
        tier = PrivilegeTier.NONE,
        capabilities = emptySet(),
        managerPackage = null,
        managerLabel = null,
        selfTest = null,
        binderAlive = false,
    ),
    val isRunningSelfTest: Boolean = false,
    val message: String? = null,
    val diagnosisText: String? = null,
    val copied: Boolean = false,
) {
    val phase: ShizukuSetupPhase get() = status.phase
    val capabilities: Set<PrivilegeCapability> get() = status.capabilities
}
