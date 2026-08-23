package com.silauncer.cepat.util

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Process
import java.util.concurrent.AbstractExecutorService
import java.util.concurrent.TimeUnit

/**
 * LooperExecutor
 *
 * // [Jalur Class]: com.silauncer.cepat.util.LooperExecutor
 * // [Penjelasan]: ExecutorService yang mengeksekusi tugas pada Looper/Handler tertentu (adaptasi AOSP Launcher3).
 */
class LooperExecutor(val looper: Looper) : AbstractExecutorService() {

    val handler: Handler = Handler(looper)

    /**
     * // [Jalur Class]: com.silauncer.cepat.util.LooperExecutor
     * // [Penjelasan]: Mengeksekusi runnable langsung jika berada di thread looper yang sama, atau memposkan ke Handler.
     */
    override fun execute(runnable: Runnable) {
        if (looper == Looper.myLooper()) {
            runnable.run()
        } else {
            handler.post(runnable)
        }
    }

    fun post(runnable: Runnable) {
        handler.post(runnable)
    }

    @Deprecated("Shutdown not supported")
    override fun shutdown() {
        throw UnsupportedOperationException()
    }

    @Deprecated("ShutdownNow not supported")
    override fun shutdownNow(): MutableList<Runnable> {
        throw UnsupportedOperationException()
    }

    override fun isShutdown(): Boolean = false

    override fun isTerminated(): Boolean = false

    @Deprecated("AwaitTermination not supported")
    override fun awaitTermination(timeout: Long, unit: TimeUnit): Boolean {
        throw UnsupportedOperationException()
    }

    fun getThread(): Thread = looper.thread

    fun setThreadPriority(priority: Int) {
        val thread = getThread()
        if (thread is HandlerThread) {
            Process.setThreadPriority(thread.threadId, priority)
        }
    }
}
