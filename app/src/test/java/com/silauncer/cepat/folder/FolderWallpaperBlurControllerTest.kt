package com.silauncer.cepat.folder

import android.content.Context
import android.view.View
import android.widget.ImageView
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.isActive
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.lang.reflect.Field

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class FolderWallpaperBlurControllerTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var controller: FolderWallpaperBlurController
    private lateinit var context: Context

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        controller = FolderWallpaperBlurController(context)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testLifecycleMemoryLeak() {
        val imageView = ImageView(context)
        val dimOverlay = View(context)
        
        // Pemanggilan pertama, memicu pembuatan scope dan job
        controller.applyWallpaperBackground(imageView, dimOverlay)
        
        // Memastikan activeJob tidak null
        val activeJobField: Field = FolderWallpaperBlurController::class.java.getDeclaredField("activeJob")
        activeJobField.isAccessible = true
        assertNotNull(activeJobField.get(controller))
        
        val scopeField: Field = FolderWallpaperBlurController::class.java.getDeclaredField("controllerScope")
        scopeField.isAccessible = true
        val scope = scopeField.get(controller) as kotlinx.coroutines.CoroutineScope
        assertTrue(scope.isActive)
        
        // Memanggil clear
        controller.clear()
        
        // Memastikan activeJob null
        assertNull(activeJobField.get(controller))
        
        // Memastikan scope di-cancel
        assertFalse(scope.isActive)
        
        // Pemanggilan clear berulang aman
        controller.clear()
        
        // Memastikan bisa dipanggil ulang tanpa error (behavior normal dan recreate scope)
        controller.applyWallpaperBackground(imageView, dimOverlay)
        val newScope = scopeField.get(controller) as kotlinx.coroutines.CoroutineScope
        assertTrue(newScope.isActive)
    }
}
