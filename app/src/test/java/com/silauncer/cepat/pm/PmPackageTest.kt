package com.silauncer.cepat.pm

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Process
import androidx.test.core.app.ApplicationProvider
import com.silauncer.cepat.util.PackageUserKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * PmPackageTest
 *
 * // [Jalur Class]: com.silauncer.cepat.pm.PmPackageTest
 * // [Penjelasan]: Pengujian unit untuk memverifikasi fungsionalitas paket pm (UserCache, PinRequestHelper, dan ShortcutConfigActivityInfo).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE, application = Application::class)
class PmPackageTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testUserCacheProfilesAndSerialNumbers() {
        // [Jalur Class]: com.silauncer.cepat.pm.PmPackageTest
        // [Penjelasan]: Menguji bahwa UserCache mengembalikan daftar profile user dan pemetaan serial number secara valid.
        val userCache = UserCache.getInstance(context)
        val profiles = userCache.getUserProfiles()
        assertNotNull(profiles)
        assertTrue(profiles.isNotEmpty())

        val myUser = Process.myUserHandle()
        assertTrue(profiles.contains(myUser))

        val serialNumber = userCache.getSerialNumberForUser(myUser)
        val userFromSerial = userCache.getUserForSerialNumber(serialNumber)
        assertNotNull(userFromSerial)
        assertEquals(myUser, userFromSerial)
    }

    @Test
    fun testUserCacheListenerRegistration() {
        // [Jalur Class]: com.silauncer.cepat.pm.PmPackageTest
        // [Penjelasan]: Menguji pendaftaran dan pelepasan listener perubahan profile user pada UserCache.
        val userCache = UserCache.getInstance(context)
        var called = false
        val listener = Runnable { called = true }

        val closeable = userCache.addUserChangeListener(listener)
        assertNotNull(closeable)
        closeable.close()

        // [Penjelasan]: Memastikan cache tetap dapat melayani query user profile dan serial number secara instan setelah listener dilepas.
        val myUser = Process.myUserHandle()
        val serial = userCache.getSerialNumberForUser(myUser)
        val userFromSerial = userCache.getUserForSerialNumber(serial)
        assertEquals(myUser, userFromSerial)
        assertTrue(userCache.getUserProfiles().contains(myUser))
    }

    @Test
    fun testUserCacheEagerInitialization() {
        // [Jalur Class]: com.silauncer.cepat.pm.PmPackageTest
        // [Penjelasan]: Menguji bahwa UserCache sudah memuat profile dan serial number pada saat instansiasi tanpa harus mendaftarkan listener terlebih dahulu.
        val userCache = UserCache.getInstance(context)
        val myUser = Process.myUserHandle()
        val profiles = userCache.getUserProfiles()
        assertTrue(profiles.contains(myUser))

        val serial = userCache.getSerialNumberForUser(myUser)
        val user = userCache.getUserForSerialNumber(serial)
        assertEquals(myUser, user)
    }

    @Test
    fun testPinRequestHelperExtraction() {
        // [Jalur Class]: com.silauncer.cepat.pm.PmPackageTest
        // [Penjelasan]: Menguji PinRequestHelper mengekstrak null dari intent yang tidak berisi PinItemRequest.
        val emptyIntent = Intent()
        val request = PinRequestHelper.getPinItemRequest(emptyIntent)
        assertNull(request)
    }

    @Test
    fun testShortcutConfigActivityInfoQuery() {
        // [Jalur Class]: com.silauncer.cepat.pm.PmPackageTest
        // [Penjelasan]: Menguji bahwa ShortcutConfigActivityInfo.queryList mengembalikan list non-null (meskipun kosong pada Robolectric tanpa dummy activity).
        val myUser = Process.myUserHandle()
        val key = PackageUserKey("com.silauncer.cepat", myUser)
        val list = ShortcutConfigActivityInfo.queryList(context, key)
        assertNotNull(list)
    }
}
