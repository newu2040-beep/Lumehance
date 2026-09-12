package com.example

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.example.domain.model.EnhancePreset
import com.example.domain.model.EnhancementConfig
import com.example.engine.ImageProcessor
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Lumenhance", appName)
  }

  @Test
  fun `preset configurations are properly initialized`() {
    val balanced = EnhancementConfig.fromPreset(EnhancePreset.BALANCED)
    assertEquals(2, balanced.upscaleFactor)
    assertTrue(balanced.denoiseStrength > 0f)

    val superRes = EnhancementConfig.fromPreset(EnhancePreset.SUPER_RES_4X)
    assertEquals(4, superRes.upscaleFactor)

    val vintage = EnhancementConfig.fromPreset(EnhancePreset.VINTAGE_RESTORE)
    assertTrue(vintage.oldPhotoRestore)
  }

  @Test
  fun `image processor enhances bitmap without crashing`() = runBlocking {
    val testBitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
    val config = EnhancementConfig.fromPreset(EnhancePreset.BALANCED)
    val output = ImageProcessor.enhance(testBitmap, config) { _, _ -> }

    assertNotNull(output.enhancedBitmap)
    assertEquals(128, output.enhancedBitmap.width) // 2x upscale
    assertEquals(128, output.enhancedBitmap.height)
    assertTrue(output.passes.isNotEmpty())
  }
}
