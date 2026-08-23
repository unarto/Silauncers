package com.silauncer.cepat.folder

import android.app.Application
import android.content.ComponentName
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.silauncer.cepat.apps.AppInfo
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// [Jalur Class]: com.silauncer.cepat.folder.SmartFolderCategorizerTest
// [Penjelasan]: Unit test untuk memverifikasi fungsionalitas pengelompokan cerdas dan penamaan otomatis folder (SmartFolderCategorizer) menggunakan pencocokan heuristik kata kunci.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE, application = Application::class)
class SmartFolderCategorizerTest {

    private lateinit var context: Context
    private lateinit var categorizer: SmartFolderCategorizer

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        categorizer = SmartFolderCategorizer(context)
    }

    @Test
    fun testCategoryDetectionByKeywords() {
        // [Jalur Class]: com.silauncer.cepat.folder.SmartFolderCategorizerTest
        // [Penjelasan]: Memverifikasi bahwa aplikasi sosial diidentifikasi secara tepat sebagai "Sosial" berbasis kata kunci nama paket.
        val wa = AppInfo(
            name = "WhatsApp",
            componentName = ComponentName("com.whatsapp", "com.whatsapp.Main"),
            packageName = "com.whatsapp"
        )
        val ig = AppInfo(
            name = "Instagram",
            componentName = ComponentName("com.instagram.android", "com.instagram.android.activity.Main"),
            packageName = "com.instagram.android"
        )
        assertEquals("Sosial", categorizer.getCategoryName(listOf(wa, ig)))

        // [Jalur Class]: com.silauncer.cepat.folder.SmartFolderCategorizerTest
        // [Penjelasan]: Memverifikasi pendeteksian kategori game berbasis nama paket.
        val pubg = AppInfo(
            name = "PUBG Mobile",
            componentName = ComponentName("com.tencent.ig", "com.tencent.ig.MainActivity"),
            packageName = "com.tencent.ig"
        )
        val subway = AppInfo(
            name = "Subway Surfers",
            componentName = ComponentName("com.kiloo.subwaysurf", "com.kiloo.subwaysurf.MainActivity"),
            packageName = "com.kiloo.subwaysurf"
        )
        assertEquals("Game", categorizer.getCategoryName(listOf(pubg, subway)))

        // [Jalur Class]: com.silauncer.cepat.folder.SmartFolderCategorizerTest
        // [Penjelasan]: Memverifikasi pendeteksian kategori media/audio.
        val spotify = AppInfo(
            name = "Spotify",
            componentName = ComponentName("com.spotify.music", "com.spotify.music.MainActivity"),
            packageName = "com.spotify.music"
        )
        assertEquals("Media", categorizer.getCategoryName(listOf(spotify)))
    }

    @Test
    fun testMajorityCategorySelection() {
        // [Jalur Class]: com.silauncer.cepat.folder.SmartFolderCategorizerTest
        // [Penjelasan]: Menggabungkan beberapa aplikasi dari berbagai kategori, dan memastikan kategori mayoritas yang terpilih.
        val wa = AppInfo(
            name = "WhatsApp",
            componentName = ComponentName("com.whatsapp", "com.whatsapp.Main"),
            packageName = "com.whatsapp"
        )
        val ig = AppInfo(
            name = "Instagram",
            componentName = ComponentName("com.instagram.android", "com.instagram.android.activity.Main"),
            packageName = "com.instagram.android"
        )
        val pubg = AppInfo(
            name = "PUBG Mobile",
            componentName = ComponentName("com.tencent.ig", "com.tencent.ig.MainActivity"),
            packageName = "com.tencent.ig"
        )

        // 2 Sosial, 1 Game -> Harus mengembalikan "Sosial"
        assertEquals("Sosial", categorizer.getCategoryName(listOf(wa, ig, pubg)))
    }
}
