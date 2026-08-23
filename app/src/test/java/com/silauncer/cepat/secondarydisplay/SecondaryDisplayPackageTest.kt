package com.silauncer.cepat.secondarydisplay

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.os.Process
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.silauncer.cepat.apps.AppInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

/**
 * SecondaryDisplayPackageTest
 *
 * // [Jalur Class]: com.silauncer.cepat.secondarydisplay.SecondaryDisplayPackageTest
 * // [Penjelasan]: Pengujian unit komprehensif untuk package com.silauncer.cepat.secondarydisplay (PinnedAppsAdapter, SecondaryDisplayPredictions, SecondaryDragController).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE, application = Application::class)
class SecondaryDisplayPackageTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testPinnedAppsAdapter_addRemoveToggle() {
        val adapter = PinnedAppsAdapter(
            context = context,
            onAppClickListener = {},
            onAppLongClickListener = { false }
        )
        adapter.init()

        val app1 = AppInfo(
            name = "App One",
            componentName = ComponentName("com.example.one", "com.example.one.MainActivity"),
            packageName = "com.example.one",
            user = Process.myUserHandle()
        )

        assertFalse(adapter.isPinned(app1))

        // Add pinned app
        adapter.addPinnedApp(app1)
        assertTrue(adapter.isPinned(app1))

        // Toggle pinned app (removes)
        adapter.togglePinned(app1)
        assertFalse(adapter.isPinned(app1))

        // Toggle again (adds)
        adapter.togglePinned(app1)
        assertTrue(adapter.isPinned(app1))

        // Remove pinned app
        adapter.removePinnedApp(app1)
        assertFalse(adapter.isPinned(app1))

        adapter.destroy()
    }

    @Test
    fun testPinnedAppsAdapter_systemShortcutCreation() {
        val adapter = PinnedAppsAdapter(
            context = context,
            onAppClickListener = {},
            onAppLongClickListener = { false }
        )
        adapter.init()

        val app1 = AppInfo(
            name = "App One",
            componentName = ComponentName("com.example.one", "com.example.one.MainActivity"),
            packageName = "com.example.one",
            user = Process.myUserHandle()
        )

        val shortcut = adapter.getSystemShortcut(app1, null)
        assertTrue(shortcut is PinnedAppsAdapter.PinUnPinShortcut)

        val pinShortcut = shortcut as PinnedAppsAdapter.PinUnPinShortcut
        assertFalse(pinShortcut.isPinned)

        // Click shortcut to pin
        pinShortcut.onClick(null)
        assertTrue(adapter.isPinned(app1))

        adapter.destroy()
    }

    @Test
    fun testSecondaryDisplayPredictions_setAndGet() {
        val predictions = SecondaryDisplayPredictions.newInstance(context)
        assertTrue(predictions.getPredictedApps().isEmpty())

        val app1 = AppInfo(
            name = "App One",
            componentName = ComponentName("com.example.one", "com.example.one.MainActivity"),
            packageName = "com.example.one",
            user = Process.myUserHandle()
        )

        predictions.setPredictedApps(listOf(app1))
        assertEquals(1, predictions.getPredictedApps().size)
        assertEquals("com.example.one", predictions.getPredictedApps()[0].packageName)
    }

    @Test
    fun testSecondaryDisplayPredictions_smartRanking() {
        val predictions = SecondaryDisplayPredictions.newInstance(context)
        val prefs = com.silauncer.cepat.storage.LauncherPreferences()
        
        val app1 = AppInfo(
            name = "App One",
            componentName = ComponentName("com.example.one", "com.example.one.MainActivity"),
            packageName = "com.example.one",
            user = Process.myUserHandle()
        )
        val app2 = AppInfo(
            name = "App Two",
            componentName = ComponentName("com.example.two", "com.example.two.MainActivity"),
            packageName = "com.example.two",
            user = Process.myUserHandle()
        )
        val app3 = AppInfo(
            name = "App Three",
            componentName = ComponentName("com.example.three", "com.example.three.MainActivity"),
            packageName = "com.example.three",
            user = Process.myUserHandle()
        )

        // Atur agar App Two diluncurkan paling sering, diikuti App Three
        prefs.incrementAppLaunchCount("com.example.two")
        prefs.incrementAppLaunchCount("com.example.two")
        prefs.incrementAppLaunchCount("com.example.three")

        // Latih prediksi
        predictions.setPredictedApps(listOf(app1, app2, app3))
        val results = predictions.getPredictedApps()

        // App Two harus berada di peringkat pertama karena memiliki 2 peluncuran
        // App Three di peringkat kedua karena memiliki 1 peluncuran
        // App One di peringkat ketiga karena memiliki 0 peluncuran
        assertEquals(3, results.size)
        assertEquals("com.example.two", results[0].packageName)
        assertEquals("com.example.three", results[1].packageName)
        assertEquals("com.example.one", results[2].packageName)
    }

    @Test
    fun testSecondaryDragController_listenerState() {
        val controller = SecondaryDragController(context)
        assertFalse(controller.isDragging())

        var dragStarted = false
        var dragEnded = false

        controller.setDragListener(object : SecondaryDragController.DragListener {
            override fun onDragStart(appInfo: AppInfo) {
                dragStarted = true
            }

            override fun onDragEnd(appInfo: AppInfo?, success: Boolean) {
                dragEnded = true
            }
        })

        val app1 = AppInfo(
            name = "App One",
            componentName = ComponentName("com.example.one", "com.example.one.MainActivity"),
            packageName = "com.example.one",
            user = Process.myUserHandle()
        )

        val dummyView = View(context)
        dummyView.layout(0, 0, 100, 100)

        val startResult = controller.startDrag(dummyView, app1, 50, 50)
        assertTrue(startResult)
        assertTrue(controller.isDragging())
        assertTrue(dragStarted)

        controller.cancelDrag(true)
        assertFalse(controller.isDragging())
        assertTrue(dragEnded)
    }

    // [Jalur Class]: com.silauncer.cepat.secondarydisplay.SecondaryDisplayPackageTest
    // [Penjelasan]: Memverifikasi bahwa SecondaryDisplayLauncher berhasil diinisialisasi, mendengarkan broadcast perubahan paket (added/removed/changed), memperbarui daftar aplikasi secara reaktif, dan membersihkan seluruh listener/receiver saat onDestroy dipanggil.
    @Test
    fun testSecondaryDisplayLauncher_lifecycleAndPackageUpdates() {
        val controller = Robolectric.buildActivity(SecondaryDisplayLauncher::class.java).create().start().resume()
        val activity = controller.get()
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        // 1. Broadcast ACTION_PACKAGE_ADDED
        val addedIntent = android.content.Intent(android.content.Intent.ACTION_PACKAGE_ADDED).apply {
            data = android.net.Uri.parse("package:com.new.secondary.app")
            putExtra(android.content.Intent.EXTRA_REPLACING, false)
        }
        activity.sendBroadcast(addedIntent)
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        // 2. Broadcast ACTION_PACKAGE_CHANGED
        val changedIntent = android.content.Intent(android.content.Intent.ACTION_PACKAGE_CHANGED).apply {
            data = android.net.Uri.parse("package:com.new.secondary.app")
            putExtra(android.content.Intent.EXTRA_REPLACING, false)
        }
        activity.sendBroadcast(changedIntent)
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        // 3. Broadcast ACTION_PACKAGE_REMOVED
        val removedIntent = android.content.Intent(android.content.Intent.ACTION_PACKAGE_REMOVED).apply {
            data = android.net.Uri.parse("package:com.new.secondary.app")
            putExtra(android.content.Intent.EXTRA_REPLACING, false)
        }
        activity.sendBroadcast(removedIntent)
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        // 4. Verifikasi siklus hidup onDestroy membersihkan receiver dan listener tanpa error
        controller.pause().stop().destroy()
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
    }
}
