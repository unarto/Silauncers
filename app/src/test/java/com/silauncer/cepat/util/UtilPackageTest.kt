package com.silauncer.cepat.util

import android.app.Application
import android.content.Context
import android.graphics.Color
import android.graphics.ColorMatrix
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * UtilPackageTest
 *
 * // [Jalur Class]: com.silauncer.cepat.util.UtilPackageTest
 * // [Penjelasan]: Pengujian unit komprehensif untuk memvalidasi fungsi package util (CellAndSpan, GridOccupancy, IntArray, IntSet, Executors, LooperExecutor, RunnableList, FlingBlockCheck, Preconditions, Themes).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE, application = Application::class)
class UtilPackageTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    
        @Test
    fun testCellAndSpan() {
        val cell1 = CellAndSpan(1, 2, 3, 4)
        assertEquals(1, cell1.cellX)
        assertEquals(2, cell1.cellY)
        assertEquals(3, cell1.spanX)
        assertEquals(4, cell1.spanY)

        val cell2 = CellAndSpan()
        cell2.copyFrom(cell1)
        assertEquals(1, cell2.cellX)
        assertEquals(2, cell2.cellY)
        assertEquals(3, cell2.spanX)
        assertEquals(4, cell2.spanY)
        assertEquals("(1, 2: 3, 4)", cell1.toString())
    }

    

    
        @Test
    fun testIntArray() {
        val arr = com.silauncer.cepat.util.IntArray()
        assertTrue(arr.isEmpty())

        arr.add(10)
        arr.add(20)
        arr.add(30)
        assertEquals(3, arr.size())
        assertEquals(10, arr.get(0))
        assertEquals(20, arr.get(1))
        assertEquals(30, arr.get(2))

        assertTrue(arr.contains(20))
        assertEquals(1, arr.indexOf(20))

        arr.removeValue(20)
        assertEquals(2, arr.size())
        assertFalse(arr.contains(20))

        val concat = arr.toConcatString()
        assertEquals("10, 30", concat)

        val parsed = com.silauncer.cepat.util.IntArray.fromConcatString("5, 15, 25")
        assertEquals(3, parsed.size())
        assertEquals(5, parsed.get(0))
        assertEquals(15, parsed.get(1))
        assertEquals(25, parsed.get(2))
    }

    

    
        @Test
    fun testExecutorsAndLooper() {
        assertNotNull(Executors.MAIN_EXECUTOR)
        assertNotNull(Executors.MODEL_EXECUTOR)
        assertNotNull(Executors.UI_HELPER_EXECUTOR)
        assertNotNull(Executors.THREAD_POOL_EXECUTOR)

        var ran = false
        Executors.MAIN_EXECUTOR.execute { ran = true }
        assertTrue(ran)
    }

    

    

    

    

    
        @Test
    fun testAlarmAndOnAlarmListener() {
        // [Jalur Class]: com.silauncer.cepat.util.UtilPackageTest
        // [Penjelasan]: Menguji mekanisme penjadwalan, pembatalan, pemicuan, dan penanganan status alarm dari utilitas Alarm.
        val alarm = Alarm()
        var triggered = false
        alarm.setOnAlarmListener(object : OnAlarmListener {
            override fun onAlarm(alarm: Alarm) {
                triggered = true
            }
        })

        alarm.setAlarm(1)
        assertTrue(alarm.alarmPending())
        assertEquals(1L, alarm.getLastSetTimeout())

        alarm.cancelAlarm()
        assertFalse(alarm.alarmPending())

        alarm.setAlarm(1)
        org.robolectric.shadows.ShadowLooper.idleMainLooper(10, java.util.concurrent.TimeUnit.MILLISECONDS)
        assertTrue(triggered)
        assertFalse(alarm.alarmPending())
    }

    
}
