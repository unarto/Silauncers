package com.silauncer.cepat.util

import android.os.Handler
import android.os.Looper
import android.os.SystemClock

// [Jalur Class]: com.silauncer.cepat.util.Alarm
// [Penjelasan]: Utilitas pengatur waktu (alarm) adaptasi AOSP Launcher3 yang mendukung penjadwalan mandiri, pembatalan instan, dan eksekusi callback secara thread-safe.
class Alarm : Runnable {
    private var alarmTriggerTime: Long = 0
    private var waitingForCallback = false
    private val handler: Handler = Handler(Looper.getMainLooper())
    private var alarmListener: OnAlarmListener? = null
    private var alarmPending = false
    private var lastSetTimeout: Long = 0

    fun setOnAlarmListener(alarmListener: OnAlarmListener?) {
        this.alarmListener = alarmListener
    }

    fun setAlarm(millisecondsInFuture: Long) {
        val currentTime = SystemClock.uptimeMillis()
        alarmPending = true
        val oldTriggerTime = alarmTriggerTime
        alarmTriggerTime = currentTime + millisecondsInFuture
        lastSetTimeout = millisecondsInFuture

        if (waitingForCallback && oldTriggerTime > alarmTriggerTime) {
            handler.removeCallbacks(this)
            waitingForCallback = false
        }
        if (!waitingForCallback) {
            handler.postDelayed(this, alarmTriggerTime - currentTime)
            waitingForCallback = true
        }
    }

    fun cancelAlarm() {
        alarmPending = false
    }

    override fun run() {
        waitingForCallback = false
        if (alarmPending) {
            val currentTime = SystemClock.uptimeMillis()
            if (alarmTriggerTime > currentTime) {
                handler.postDelayed(this, Math.max(0, alarmTriggerTime - currentTime))
                waitingForCallback = true
            } else {
                alarmPending = false
                alarmListener?.onAlarm(this)
            }
        }
    }

    fun alarmPending(): Boolean {
        return alarmPending
    }

    fun getLastSetTimeout(): Long {
        return lastSetTimeout
    }
}
