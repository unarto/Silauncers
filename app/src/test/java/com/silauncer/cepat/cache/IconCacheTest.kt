package com.silauncer.cepat.cache

import android.app.Application
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE, application = Application::class)
class IconCacheTest {

    private class TestDrawable : Drawable() {
        override fun draw(canvas: Canvas) {}
        override fun setAlpha(alpha: Int) {}
        override fun setColorFilter(colorFilter: ColorFilter?) {}
        @Deprecated("Deprecated in Java")
        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
    }

    @Before
    fun setup() {
        IconCache.clear()
    }

    @Test
    fun testPutAndGet() {
        val drawable = TestDrawable()
        val key = "com.test.app/MainActivity_0"
        IconCache.put(key, drawable)

        val retrieved = IconCache.get(key)
        assertNotNull(retrieved)
    }

    @Test
    fun testRemovePackagePrefixMatching() {
        val d1 = TestDrawable()
        val d2 = TestDrawable()
        val d3 = TestDrawable()

        IconCache.put("com.example.app/MainActivity_0", d1)
        IconCache.put("com.example.app/SettingsActivity_0", d2)
        IconCache.put("com.other.app/MainActivity_0", d3)

        IconCache.removePackage("com.example.app")

        assertNull(IconCache.get("com.example.app/MainActivity_0"))
        assertNull(IconCache.get("com.example.app/SettingsActivity_0"))
        assertNotNull(IconCache.get("com.other.app/MainActivity_0"))
    }
}
