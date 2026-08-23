package com.silauncer.cepat.util

import android.os.HandlerThread
import android.os.Looper
import android.os.Process
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit


/**
 * Executors
 *
 * // [Jalur Class]: com.silauncer.cepat.util.Executors
 * // [Penjelasan]: Kumpulan thread pool dan LooperExecutor terpusat untuk launcher (MAIN, MODEL, UI_HELPER, THREAD_POOL) adaptasi AOSP.
 */
object Executors {

    private val POOL_SIZE = Math.max(Runtime.getRuntime().availableProcessors(), 2)
    private const val KEEP_ALIVE = 1L

    @JvmField
    val THREAD_POOL_EXECUTOR = ThreadPoolExecutor(
        POOL_SIZE, POOL_SIZE, KEEP_ALIVE, TimeUnit.SECONDS, LinkedBlockingQueue()
    )

    @JvmField
    val MAIN_EXECUTOR = LooperExecutor(Looper.getMainLooper())

    @JvmField
    val UI_HELPER_EXECUTOR = LooperExecutor(
        createAndStartNewLooper("UiThreadHelper", Process.THREAD_PRIORITY_FOREGROUND)
    )

    @JvmField
    val MODEL_EXECUTOR = LooperExecutor(
        createAndStartNewLooper("launcher-loader")
    )

    /**
     * // [Jalur Class]: com.silauncer.cepat.util.Executors
     * // [Penjelasan]: Membuat dan memulai HandlerThread baru dengan nama dan prioritas tertentu.
     */
    @JvmStatic
    @JvmOverloads
    fun createAndStartNewLooper(name: String, priority: Int = Process.THREAD_PRIORITY_DEFAULT): Looper {
        val thread = HandlerThread(name, priority)
        thread.start()
        return thread.looper
    }
}
