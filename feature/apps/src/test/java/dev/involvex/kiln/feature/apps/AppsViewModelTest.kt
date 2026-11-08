package dev.involvex.kiln.feature.apps

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AppsViewModelTest {
    @Test
    fun `onScreenShown updates ui state`() = runTest {
        val viewModel = AppsViewModel()
        viewModel.uiState.test {
            assertEquals(AppsUiState(), awaitItem())
            viewModel.onScreenShown(
                title = "Apps",
                description = "Placeholder",
            )
            assertEquals(
                AppsUiState(title = "Apps", description = "Placeholder"),
                awaitItem(),
            )
            cancelAndIgnoreRemainingEvents()
        }
    }
}
