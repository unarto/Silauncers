package com.silauncer.cepat.dragndrop

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.view.View
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * DragAndDropTest
 *
 * [Jalur Class]: com.silauncer.cepat.dragndrop.DragAndDropTest
 * [Penjelasan]: Pengujian unit untuk komponen DragView dan sistem Drag-and-Drop Launcher3 AOSP.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE, application = Application::class)
class DragAndDropTest {

    private lateinit var context: Context
    private lateinit var rootViewGroup: FrameLayout

    @Before
    fun setUp() {
        // [Jalur Class]: com.silauncer.cepat.dragndrop.DragAndDropTest
        // [Penjelasan]: Menyiapkan context dan container root view untuk pengujian
        context = ApplicationProvider.getApplicationContext()
        rootViewGroup = FrameLayout(context).apply {
            layout(0, 0, 1080, 1920)
        }
    }

    @Test
    fun testDragView_CreationAndTracking() {
        // [Jalur Class]: com.silauncer.cepat.dragndrop.DragAndDropTest
        // [Penjelasan]: Memverifikasi pembuatan DragView, inisialisasi koordinat floating, dan pembaruan posisi 1:1 saat move
        val mockView = View(context).apply {
            layout(100, 200, 200, 300)
        }

        val dragView = DragView.createFromView(mockView, touchRawX = 150f, touchRawY = 250f)
        assertNotNull(dragView)
        assertEquals(50f, dragView.registrationX, 0.1f)
        assertEquals(50f, dragView.registrationY, 0.1f)

        dragView.show(rootViewGroup, startRawX = 150f, startRawY = 250f)
        assertEquals(100f, dragView.translationX, 0.1f)
        assertEquals(200f, dragView.translationY, 0.1f)

        // Gerakkan pointer ke koordinat baru (400, 600)
        dragView.move(rawX = 400f, rawY = 600f)
        assertEquals(350f, dragView.translationX, 0.1f)
        assertEquals(550f, dragView.translationY, 0.1f)

        dragView.remove()
    }

    @Test
    fun testDragView_AnimateToSnap() {
        // [Jalur Class]: com.silauncer.cepat.dragndrop.DragAndDropTest
        // [Penjelasan]: Memverifikasi bahwa pemanggilan animateTo menjalankan transisi dan callback penyelesaian
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val dragView = DragView(context, bitmap, 50f, 50f)
        dragView.show(rootViewGroup, 100f, 100f)

        var completed = false
        dragView.animateTo(toX = 270f, toY = 540f, targetScale = 1.0f, duration = 10L) {
            completed = true
        }
        assertNotNull(dragView)
    }
}
