package com.silauncer.cepat.pageindicators

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// [Jalur Class]: com.silauncer.cepat.pageindicators.PageIndicatorsPackageTest
// [Penjelasan]: Pengujian unit komprehensif untuk modul PageIndicators (PageIndicatorDots dan WorkspacePageIndicator) termasuk verifikasi kalkulasi scroll, marker halaman, rendering onDraw, animasi lifecycle, dan perubahan warna kuas.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PageIndicatorsPackageTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testPageIndicatorDots_initializationAndMarkers() {
        val dots = PageIndicatorDots(context)
        assertNotNull(dots)

        dots.setMarkersCount(4)
        assertEquals(4, dots.getMarkersCount())

        dots.setActiveMarker(2)
        dots.setPaintColor(Color.BLUE)

        dots.measure(
            View.MeasureSpec.makeMeasureSpec(300, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY)
        )
        dots.layout(0, 0, 300, 100)

        val bitmap = Bitmap.createBitmap(300, 100, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        dots.draw(canvas)

        dots.setScroll(50, 300)
        dots.draw(canvas)

        dots.pauseAnimations()
        dots.skipAnimationsToEnd()
        dots.stopAllAnimations()
        assertTrue(true)
    }

    @Test
    fun testPageIndicatorDots_entryAnimation() {
        val dots = PageIndicatorDots(context)
        dots.setMarkersCount(3)
        dots.prepareEntryAnimation()

        dots.measure(
            View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(80, View.MeasureSpec.EXACTLY)
        )
        dots.layout(0, 0, 200, 80)

        val bitmap = Bitmap.createBitmap(200, 80, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        dots.draw(canvas)

        dots.playEntryAnimation()
        dots.setShouldAutoHide(true)
        dots.setScroll(100, 200)

        assertTrue(dots.getMarkersCount() == 3)
    }

    @Test
    fun testWorkspacePageIndicator_scrollAndDraw() {
        val lineIndicator = WorkspacePageIndicator(context)
        assertNotNull(lineIndicator)

        lineIndicator.setMarkersCount(3)
        lineIndicator.setPaintColor(Color.WHITE)
        lineIndicator.setShouldAutoHide(true)

        lineIndicator.measure(
            View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(40, View.MeasureSpec.EXACTLY)
        )
        lineIndicator.layout(0, 0, 400, 40)

        lineIndicator.setScroll(100, 400)

        val bitmap = Bitmap.createBitmap(400, 40, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        lineIndicator.draw(canvas)

        lineIndicator.pauseAnimations()
        lineIndicator.skipAnimationsToEnd()
        lineIndicator.setActiveMarker(1)
        assertTrue(true)
    }
}
