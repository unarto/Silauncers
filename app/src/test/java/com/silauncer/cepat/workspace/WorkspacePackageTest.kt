package com.silauncer.cepat.workspace

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.silauncer.cepat.R
import com.silauncer.cepat.util.ResourceHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * WorkspacePackageTest
 *
 * // [Jalur Class]: com.silauncer.cepat.workspace.WorkspacePackageTest
 * // [Penjelasan]: Pengujian unit komprehensif untuk package com.silauncer.cepat.workspace dan com.silauncer.cepat.util.ResourceHelper guna memastikan penguraian berkas XML dan kalkulasi responsif ukuran grid launcher berjalan dengan akurasi 100%.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE, application = Application::class)
class WorkspacePackageTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        // // [Jalur Class]: com.silauncer.cepat.workspace.WorkspacePackageTest
        // // [Penjelasan]: Menyiapkan context aplikasi pengujian menggunakan Android Provider.
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testResourceHelper_and_WorkspaceSpecsParsing() {
        // // [Jalur Class]: com.silauncer.cepat.workspace.WorkspacePackageTest
        // // [Penjelasan]: Memverifikasi bahwa ResourceHelper mampu memuat berkas XML valid_workspace_file dan WorkspaceSpecs dapat menguraikannya ke dalam daftar spesifikasi tinggi dan lebar dengan benar.
        val resourceHelper = ResourceHelper(context, R.xml.valid_workspace_file)
        val specs = WorkspaceSpecs(resourceHelper)

        assertNotNull(specs.workspaceHeightSpecList)
        assertNotNull(specs.workspaceWidthSpecList)
        assertTrue(specs.workspaceHeightSpecList.isNotEmpty())
        assertTrue(specs.workspaceWidthSpecList.isNotEmpty())

        // Memastikan isi list terurai dengan benar sesuai dengan valid_workspace_file.xml
        assertEquals(3, specs.workspaceHeightSpecList.size)
        assertEquals(1, specs.workspaceWidthSpecList.size)
    }

    @Test
    fun testCalculatedWorkspaceSpec_WidthAndHeight() {
        // // [Jalur Class]: com.silauncer.cepat.workspace.WorkspacePackageTest
        // // [Penjelasan]: Memverifikasi fungsionalitas logika kalkulasi CalculatedWorkspaceSpec untuk lebar dan tinggi berdasarkan ruang layar yang tersedia (available space).
        val resourceHelper = ResourceHelper(context, R.xml.valid_workspace_file)
        val specs = WorkspaceSpecs(resourceHelper)

        // Hitung spesifikasi lebar untuk 4 kolom dengan lebar layar 1080px
        val widthSpec = specs.getCalculatedWidthSpec(columns = 4, availableWidth = 1080)
        assertEquals(1080, widthSpec.availableSpace)
        assertEquals(4, widthSpec.cells)

        // Hitung spesifikasi tinggi untuk 5 baris dengan tinggi layar 1920px
        val heightSpec = specs.getCalculatedHeightSpec(rows = 5, availableHeight = 1920)
        assertEquals(1920, heightSpec.availableSpace)
        assertEquals(5, heightSpec.cells)
    }
}
