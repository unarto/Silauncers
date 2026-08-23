// [Jalur Class]: com.silauncer.cepat.settings.SettingsArchitectureTest
// [Penjelasan]: Pengujian unit komprehensif untuk memvalidasi pemisahan SRP pada modul Settings, IconPackManager, LanguageHelper, SettingsWallpaperHelper, SettingsNodeFactory, dan SettingsTreeAdapter.
package com.silauncer.cepat.settings

import android.app.Activity
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.view.View
import android.widget.ImageView
import androidx.test.core.app.ApplicationProvider
import com.silauncer.cepat.deviceprofile.InvariantDeviceProfile
import com.silauncer.cepat.icons.IconPackManager
import com.silauncer.cepat.settings.SettingsNode.ChildNode.ActionChildNode
import com.silauncer.cepat.settings.SettingsNode.ChildNode.OptionChildNode
import com.silauncer.cepat.settings.SettingsNode.ChildNode.SwitchChildNode
import com.silauncer.cepat.settings.SettingsNode.ParentNode
import com.silauncer.cepat.storage.LauncherPreferences
import com.silauncer.cepat.utils.LanguageHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE, application = Application::class)
class SettingsArchitectureTest {

    private lateinit var context: Context
    private lateinit var prefs: LauncherPreferences

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        prefs = LauncherPreferences()
        prefs.resetToDefaults()
    }

    @Test
    fun testSettingsNodeFactoryStructure() {
        var hiddenAppsCalled = false
        var resetCalled = false
        var settingChangedCalled = false

        val factory = SettingsNodeFactory(
            context = context,
            prefs = prefs,
            onManageHiddenApps = { hiddenAppsCalled = true },
            onResetLayout = { resetCalled = true },
            onSettingChanged = { settingChangedCalled = true }
        )

        val parents = factory.createTreeNodes()
        assertEquals(5, parents.size)

        // 1. Grid & Tata Letak
        val gridParent = parents[0]
        assertEquals("parent_grid", gridParent.id)
        assertEquals("Grid & Tata Letak", gridParent.title)
        assertEquals(2, gridParent.children.size)

        val gridOption = gridParent.children[0] as OptionChildNode
        assertEquals("child_grid_layout", gridOption.id)
        assertEquals("${InvariantDeviceProfile.DEFAULT_COLUMNS}x${InvariantDeviceProfile.DEFAULT_ROWS}", gridOption.displayValue)

        // Ubah nilai grid lewat callback
        gridOption.onSelected(SettingsNode.OptionItem("6x6", "6 Kolom × 6 Baris"))
        assertTrue(settingChangedCalled)
        assertEquals(6, prefs.gridColumns)
        assertEquals(6, prefs.gridRows)

        // 2. Ikon & Label (Termasuk Icon Pack)
        settingChangedCalled = false
        val iconParent = parents[1]
        assertEquals("parent_icons", iconParent.id)

        val iconPackOption = iconParent.children.filterIsInstance<OptionChildNode>().firstOrNull { it.id == "child_icon_pack" }
        assertNotNull(iconPackOption)
        assertEquals("system_aosp", iconPackOption?.currentValue)
        assertEquals("Default Sistem / AOSP", iconPackOption?.displayValue)

        // Ubah icon pack
        iconPackOption?.onSelected?.invoke(SettingsNode.OptionItem("com.example.custompack", "Custom Pack"))
        assertTrue(settingChangedCalled)
        assertEquals("com.example.custompack", prefs.iconPack)

        val switchLabel = iconParent.children.filterIsInstance<SwitchChildNode>().firstOrNull()
        assertNotNull(switchLabel)
        assertEquals("child_show_labels", switchLabel?.id)
        assertEquals(prefs.showAppLabel, switchLabel?.isChecked)

        // Toggle switch
        switchLabel?.onCheckedChange?.invoke(!prefs.showAppLabel)
        assertFalse(prefs.showAppLabel)

        // 3. Bahasa Aplikasi (Language Settings)
        settingChangedCalled = false
        val langParent = parents[2]
        assertEquals("parent_language", langParent.id)
        val langOption = langParent.children.filterIsInstance<OptionChildNode>().firstOrNull { it.id == "child_app_language" }
        assertNotNull(langOption)
        assertEquals("system", langOption?.currentValue)

        langOption?.onSelected?.invoke(SettingsNode.OptionItem("id", "Bahasa Indonesia"))
        assertTrue(settingChangedCalled)
        assertEquals("id", prefs.appLanguage)

        // 4. App Drawer & Sorting
        val drawerParent = parents[3]
        assertEquals("parent_drawer", drawerParent.id)
        val actionHidden = drawerParent.children.filterIsInstance<ActionChildNode>().firstOrNull()
        assertNotNull(actionHidden)
        actionHidden?.onAction?.invoke()
        assertTrue(hiddenAppsCalled)

        // 5. Maintenance & Reset
        val maintenanceParent = parents[4]
        assertEquals("parent_maintenance", maintenanceParent.id)
        val actionReset = maintenanceParent.children.filterIsInstance<ActionChildNode>().firstOrNull()
        assertNotNull(actionReset)
        actionReset?.onAction?.invoke()
        assertTrue(resetCalled)
    }

    @Test
    fun testIconPackManagerAndLanguageHelper() {
        val iconPackMgr = IconPackManager.getInstance()
        assertNotNull(iconPackMgr)

        // Verifikasi label AOSP
        val aospLabel = iconPackMgr.getIconPackLabel(context, IconPackManager.SYSTEM_AOSP)
        assertEquals("Default Sistem / AOSP", aospLabel)

        // Verifikasi fallback icon getIcon
        val icon = iconPackMgr.getIcon(context, ComponentName("com.example", "MainActivity"), IconPackManager.SYSTEM_AOSP)
        assertNull(icon)

        // Verifikasi pembersihan cache
        iconPackMgr.clearCache()

        // Verifikasi LanguageHelper
        val languages = LanguageHelper.getSupportedLanguages()
        assertTrue(languages.size >= 3)
        assertEquals("Bahasa Indonesia", LanguageHelper.getLanguageDisplayName(LanguageHelper.LANGUAGE_INDONESIAN))
        assertEquals("English", LanguageHelper.getLanguageDisplayName(LanguageHelper.LANGUAGE_ENGLISH))
        assertEquals("Default Sistem", LanguageHelper.getLanguageDisplayName(LanguageHelper.LANGUAGE_SYSTEM))

        // Safe apply
        LanguageHelper.applyLanguage(context, LanguageHelper.LANGUAGE_INDONESIAN)
        LanguageHelper.applyLanguage(context, LanguageHelper.LANGUAGE_SYSTEM)
    }

    @Test
    fun testSettingsTreeAdapterFlatteningAndExpandCollapse() {
        val child1 = OptionChildNode("c1", "p1", "Option 1", null, "1", "1", emptyList(), {})
        val child2 = SwitchChildNode("c2", "p1", "Switch 1", null, true, {})
        val parent1 = ParentNode("p1", "Parent 1", "Subtitle", 0, isExpanded = true, children = listOf(child1, child2))
        val parent2 = ParentNode("p2", "Parent 2", "Subtitle", 0, isExpanded = false, children = listOf(child1))

        val adapter = SettingsTreeAdapter(context)
        adapter.setNodes(listOf(parent1, parent2))

        // parent1 (expanded = 1 parent + 2 children = 3) + parent2 (collapsed = 1 parent = 1) -> total 4 items
        assertEquals(4, adapter.itemCount)

        // Collapse parent 1
        parent1.isExpanded = false
        adapter.setNodes(listOf(parent1, parent2))
        assertEquals(2, adapter.itemCount)

        // Expand both
        parent1.isExpanded = true
        parent2.isExpanded = true
        adapter.setNodes(listOf(parent1, parent2))
        assertEquals(5, adapter.itemCount)
    }

    @Test
    fun testSettingsWallpaperHelperSafeExecution() {
        val activity = Robolectric.buildActivity(Activity::class.java).create().get()
        val helper = SettingsWallpaperHelper()

        val imageView = ImageView(activity)
        val scrimView = View(activity)

        // Pastikan eksekusi wallpaper helper aman tanpa exception
        helper.applyWindowTransparency(activity)
        helper.setupDynamicWallpaper(activity, imageView, scrimView)
        helper.clear(imageView)
    }
}
