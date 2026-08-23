package com.silauncer.cepat.touch

// [Jalur Class]: com.silauncer.cepat.touch.GestureDragState
// [Penjelasan]: State machine terpadu untuk mengelola siklus hidup gesture sentuhan, deteksi long press, dan drag & drop antar workspace dan folder tanpa konflik status atau gesture ganda.
enum class GestureDragState {
    IDLE,
    PRESSED,
    LONG_PRESS,
    DRAGGING_FOLDER,
    DRAG_OUT_FOLDER,
    DRAGGING_WORKSPACE,
    DROP,
    CANCEL
}
