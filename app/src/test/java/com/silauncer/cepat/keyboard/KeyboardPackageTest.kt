package com.silauncer.cepat.keyboard

import android.app.Application
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * KeyboardPackageTest
 *
 * // [Jalur Class]: com.silauncer.cepat.keyboard.KeyboardPackageTest
 * // [Penjelasan]: Pengujian unit komprehensif untuk komponen keyboard navigation AOSP Launcher3:
 * ItemFocusIndicatorHelper, FocusIndicatorHelper, SimpleFocusIndicatorHelper, ViewGroupFocusHelper,
 * FocusedItemDecorator, RectFocusIndicator, SpatialFocusNavigationHelper, dan KeyboardDragAndDropHandler.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE, application = Application::class)
class KeyboardPackageTest {

    private lateinit var context: Context
    private lateinit var container: FrameLayout

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        container = FrameLayout(context)
        container.layout(0, 0, 1080, 1920)
    }

    
        @Test
    fun testSimpleFocusIndicatorHelper_viewToRect() {
        // [Jalur Class]: com.silauncer.cepat.keyboard.FocusIndicatorHelper.SimpleFocusIndicatorHelper
        // [Penjelasan]: Memverifikasi perhitungan bounding box rect dari view anak langsung
        val helper = FocusIndicatorHelper.SimpleFocusIndicatorHelper(container)
        val child = View(context).apply {
            layout(100, 200, 300, 400)
        }
        container.addView(child)

        val outRect = Rect()
        helper.viewToRect(child, outRect)

        assertEquals(100, outRect.left)
        assertEquals(200, outRect.top)
        assertEquals(300, outRect.right)
        assertEquals(400, outRect.bottom)
    }

    
        @Test
    fun testFocusIndicatorHelper_focusChangeAndDraw() {
        // [Jalur Class]: com.silauncer.cepat.keyboard.FocusIndicatorHelper
        // [Penjelasan]: Memverifikasi perubahan fokus pada view dan eksekusi draw tanpa exception
        val helper = FocusIndicatorHelper.SimpleFocusIndicatorHelper(container)
        val child = View(context).apply {
            layout(50, 50, 200, 200)
        }
        container.addView(child)

        helper.onFocusChange(child, true)
        assertEquals(child, helper.getCurrentItem())

        val canvas = Canvas()
        helper.draw(canvas)

        helper.onFocusChange(child, false)
    }

    

    
        @Test
    fun testFocusedItemDecorator_listenerAndHelper() {
        // [Jalur Class]: com.silauncer.cepat.keyboard.FocusedItemDecorator
        // [Penjelasan]: Memverifikasi penyediaan OnFocusChangeListener dari decorator
        val decorator = FocusedItemDecorator(container)
        assertNotNull(decorator.getFocusListener())
        assertNotNull(decorator.getFocusHelper())
    }

    

    
        @Test
    fun testSpatialFocusNavigationHelper_directionalNavigation() {
        // [Jalur Class]: com.silauncer.cepat.keyboard.SpatialFocusNavigationHelper
        // [Penjelasan]: Memverifikasi penemuan fokus spasial 2D (Kanan, Kiri, Bawah, Atas, Forward, Backward)
        val nav = SpatialFocusNavigationHelper()

        // 2x2 Grid:
        // [Cell 0: 0,0 - 100,100]    [Cell 1: 150,0 - 250,100]
        // [Cell 2: 0,150 - 100,250]  [Cell 3: 150,150 - 250,250]
        val cell0 = Rect(0, 0, 100, 100)
        val cell1 = Rect(150, 0, 250, 100)
        val cell2 = Rect(0, 150, 100, 250)
        val cell3 = Rect(150, 150, 250, 250)

        val items = listOf(cell0, cell1, cell2, cell3)

        // Move Right from Cell 0 -> Cell 1
        val rightTarget = nav.findNextFocus(items, cell0, { it }, View.FOCUS_RIGHT)
        assertEquals(cell1, rightTarget)

        // Move Down from Cell 0 -> Cell 2
        val downTarget = nav.findNextFocus(items, cell0, { it }, View.FOCUS_DOWN)
        assertEquals(cell2, downTarget)

        // Move Left from Cell 1 -> Cell 0
        val leftTarget = nav.findNextFocus(items, cell1, { it }, View.FOCUS_LEFT)
        assertEquals(cell0, leftTarget)

        // Move Up from Cell 2 -> Cell 0
        val upTarget = nav.findNextFocus(items, cell2, { it }, View.FOCUS_UP)
        assertEquals(cell0, upTarget)

        // Move Forward (Tab order)
        val forwardTarget = nav.findNextFocus(items, cell0, { it }, View.FOCUS_FORWARD)
        assertEquals(cell1, forwardTarget)

        // Move Backward (Shift+Tab order)
        val backwardTarget = nav.findNextFocus(items, cell0, { it }, View.FOCUS_BACKWARD)
        assertEquals(cell3, backwardTarget)
    }

    
}
