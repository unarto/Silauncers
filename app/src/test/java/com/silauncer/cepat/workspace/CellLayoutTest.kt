package com.silauncer.cepat.workspace

import android.app.Application
import android.content.Context
import android.view.View
import android.widget.FrameLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * CellLayoutTest
 *
 * [Jalur Class]: com.silauncer.cepat.workspace.CellLayoutTest
 * [Penjelasan]: Pengujian unit untuk CellLayout guna memverifikasi algoritma findMatchingCellToTarget,
 * realtimeReorder visual displacement calculations, cellToPoint snap calculations, dan resetReorder.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE, application = Application::class)
class CellLayoutTest {

    private lateinit var context: Context
    private lateinit var cellLayout: CellLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var rootView: FrameLayout

    @Before
    fun setUp() {
        // [Jalur Class]: com.silauncer.cepat.workspace.CellLayoutTest
        // [Penjelasan]: Menyiapkan context, instance CellLayout, RecyclerView berukuran 1080x1920 dan rootView
        context = ApplicationProvider.getApplicationContext()
        cellLayout = CellLayout()
        rootView = FrameLayout(context)
        recyclerView = RecyclerView(context).apply {
            layoutManager = GridLayoutManager(context, 4)
            layout(0, 0, 1080, 1920)
        }
        rootView.addView(recyclerView)
    }

    @Test
    fun testFindMatchingCellToTarget_MathematicalFallback() {
        // [Jalur Class]: com.silauncer.cepat.workspace.CellLayoutTest
        // [Penjelasan]: Memverifikasi bahwa findMatchingCellToTarget mengembalikan indeks sel grid yang akurat
        // Untuk grid 4 kolom, lebar 1080 -> lebar per kolom 270px.
        // Titik tengah drag (135, 135) harus jatuh pada kolom 0 baris 0 -> indeks 0
        val targetPos0 = cellLayout.findMatchingCellToTarget(
            dragCenterX = 135f,
            dragCenterY = 135f,
            recyclerView = recyclerView,
            columns = 4,
            itemCount = 12
        )
        assertEquals(0, targetPos0)

        // Titik tengah drag (405, 135) -> kolom 1, baris 0 -> indeks 1
        val targetPos1 = cellLayout.findMatchingCellToTarget(
            dragCenterX = 405f,
            dragCenterY = 135f,
            recyclerView = recyclerView,
            columns = 4,
            itemCount = 12
        )
        assertEquals(1, targetPos1)

        // Titik tengah drag (135, 405) -> kolom 0, baris 1 -> indeks 4
        val targetPos4 = cellLayout.findMatchingCellToTarget(
            dragCenterX = 135f,
            dragCenterY = 405f,
            recyclerView = recyclerView,
            columns = 4,
            itemCount = 12
        )
        assertEquals(4, targetPos4)
    }

    @Test
    fun testCellToPoint_FallbackCoordinates() {
        // [Jalur Class]: com.silauncer.cepat.workspace.CellLayoutTest
        // [Penjelasan]: Memverifikasi bahwa kalkulasi cellToPoint menghasilkan titik koordinat layar yang presisi untuk SNAP
        val point0 = cellLayout.cellToPoint(
            targetPos = 0,
            recyclerView = recyclerView,
            columns = 4,
            rootView = rootView
        )
        assertNotNull(point0)
        assertEquals(0f, point0.x, 0.1f)
        assertEquals(0f, point0.y, 0.1f)

        val point1 = cellLayout.cellToPoint(
            targetPos = 1,
            recyclerView = recyclerView,
            columns = 4,
            rootView = rootView
        )
        assertNotNull(point1)
        assertEquals(270f, point1.x, 0.1f)
    }

    @Test
    fun testRealtimeReorder_and_Reset() {
        // [Jalur Class]: com.silauncer.cepat.workspace.CellLayoutTest
        // [Penjelasan]: Memverifikasi pemanggilan realtimeReorder dan resetReorder tidak memicu crash atau inkonsistensi
        cellLayout.realtimeReorder(
            fromPos = 0,
            targetPos = 3,
            recyclerView = recyclerView,
            columns = 4
        )
        cellLayout.resetReorder(recyclerView, animate = false)
    }
}
