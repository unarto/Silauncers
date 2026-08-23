package com.silauncer.cepat.folder

import android.app.Application
import android.content.Context
import android.graphics.Point
import android.graphics.PointF
import android.graphics.Rect
import androidx.test.core.app.ApplicationProvider
import com.silauncer.cepat.apps.AppInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * FolderPackageTest
 *
 * // [Jalur Class]: com.silauncer.cepat.folder.FolderPackageTest
 * // [Penjelasan]: Unit test untuk memverifikasi logika tata letak grid 3 kolom (FolderGridOrganizer),
 * aturan thumbnail 3x3 (ClippedFolderIconLayoutRule), dan penamaan folder (FolderNameProvider).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE, application = Application::class)
class FolderPackageTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testFolderGridOrganizer_threeColumns() {
        // [Jalur Class]: com.silauncer.cepat.folder.FolderGridOrganizer
        // [Penjelasan]: Memastikan FolderGridOrganizer memetakan index peringkat ke koordinat grid 3 kolom secara akurat
        val organizer = FolderGridOrganizer(countX = 3, maxCountY = 4)
        assertEquals(3, organizer.countX)
        assertEquals(12, organizer.maxItemsPerPage)

        val pt = Point()
        // Item rank 0 -> col 0, row 0
        organizer.getPosForRank(0, pt)
        assertEquals(0, pt.x)
        assertEquals(0, pt.y)

        // Item rank 1 -> col 1, row 0
        organizer.getPosForRank(1, pt)
        assertEquals(1, pt.x)
        assertEquals(0, pt.y)

        // Item rank 2 -> col 2, row 0
        organizer.getPosForRank(2, pt)
        assertEquals(2, pt.x)
        assertEquals(0, pt.y)

        // Item rank 3 -> col 0, row 1 (baris kedua)
        organizer.getPosForRank(3, pt)
        assertEquals(0, pt.x)
        assertEquals(1, pt.y)

        // Item rank 4 -> col 1, row 1
        organizer.getPosForRank(4, pt)
        assertEquals(1, pt.x)
        assertEquals(1, pt.y)

        // Item rank 5 -> col 2, row 1
        organizer.getPosForRank(5, pt)
        assertEquals(2, pt.x)
        assertEquals(1, pt.y)

        // Verifikasi kalkulasi halaman
        assertEquals(0, organizer.getPageForRank(5))
        assertEquals(1, organizer.getPageForRank(12))
        assertEquals(1, organizer.getNumPages(5))
        assertEquals(2, organizer.getNumPages(13))
    }

    @Test
    fun testClippedFolderIconLayoutRule_nineItems() {
        // [Jalur Class]: com.silauncer.cepat.folder.ClippedFolderIconLayoutRule
        // [Penjelasan]: Memastikan ClippedFolderIconLayoutRule mendukung batas maksimal 9 thumbnail untuk grid 3x3
        val rule = ClippedFolderIconLayoutRule(numColumns = 3, numRows = 3)
        assertEquals(9, rule.maxNumItemsInPreview)

        val bounds = Rect(0, 0, 90, 90)
        val outPoint = PointF()

        // Valid index
        assertTrue(rule.getPreviewItemPosition(0, bounds, outPoint))
        assertEquals(15f, outPoint.x, 0.01f)
        assertEquals(15f, outPoint.y, 0.01f)

        assertTrue(rule.getPreviewItemPosition(4, bounds, outPoint)) // Center cell (col 1, row 1)
        assertEquals(45f, outPoint.x, 0.01f)
        assertEquals(45f, outPoint.y, 0.01f)

        assertTrue(rule.getPreviewItemPosition(8, bounds, outPoint)) // Bottom right cell (col 2, row 2)
        assertEquals(75f, outPoint.x, 0.01f)
        assertEquals(75f, outPoint.y, 0.01f)

        // Out of bounds index
        assertFalse(rule.getPreviewItemPosition(9, bounds, outPoint))
        assertFalse(rule.getPreviewItemPosition(-1, bounds, outPoint))
    }

    @Test
    fun testFolderNameProvider() {
        // [Jalur Class]: com.silauncer.cepat.folder.FolderNameProvider
        // [Penjelasan]: Memastikan FolderNameProvider menghasilkan nama default yang valid
        val provider = FolderNameProvider(context)
        val defaultName = provider.getDefaultFolderName()
        assertTrue(defaultName.isNotEmpty())
        assertEquals(defaultName, provider.getSuggestedFolderName(emptyList()))
    }
}
