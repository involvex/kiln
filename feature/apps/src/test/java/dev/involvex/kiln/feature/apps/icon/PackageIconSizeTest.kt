package dev.involvex.kiln.feature.apps.icon

import android.content.Context
import coil3.request.Options
import coil3.size.Size
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PackageIconSizeTest {
    private val context = mockk<Context>(relaxed = true)

    @Test
    fun iconPixelSize_downscalesLargeMipmapToRequestCeiling() {
        val options = Options(context, size = Size(48, 48))
        val (w, h) = iconPixelSize(options, drawableWidth = 512, drawableHeight = 512)
        assertEquals(48, w)
        assertEquals(48, h)
    }

    @Test
    fun iconPixelSize_capsUndefinedSizeAtMaxEdge() {
        val options = Options(context, size = Size.ORIGINAL)
        val (w, h) = iconPixelSize(options, drawableWidth = 1024, drawableHeight = 1024)
        assertTrue(w <= 192)
        assertTrue(h <= 192)
        assertEquals(w, h)
    }
}
