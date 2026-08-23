package com.silauncer.cepat.util

// [Jalur Class]: com.silauncer.cepat.util.OnAlarmListener
// [Penjelasan]: Antarmuka callback untuk menerima notifikasi ketika Alarm mencapai waktu pemicu yang dijadwalkan secara responsif.
interface OnAlarmListener {
    fun onAlarm(alarm: Alarm)
}
