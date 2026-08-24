// [Jalur Class]: com.silauncer.cepat.shadow.ShadowMMKV
// [Penjelasan]: Shadow Robolectric murni untuk lingkungan pengujian unit JVM agar pemanggilan MMKV berjalan mulus tanpa membutuhkan library native C++ JNI (.so) Android.

package com.silauncer.cepat.shadow

import com.tencent.mmkv.MMKV
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.annotation.Resetter
import org.robolectric.shadow.api.Shadow
import java.util.concurrent.ConcurrentHashMap

@Implements(MMKV::class)
class ShadowMMKV {

    private val data = ConcurrentHashMap<String, Any>()

    @Implementation
    fun encode(key: String, value: Int): Boolean {
        data[key] = value
        return true
    }

    @Implementation
    fun encode(key: String, value: String?): Boolean {
        if (value != null) data[key] = value else data.remove(key)
        return true
    }

    @Implementation
    fun encode(key: String, value: Boolean): Boolean {
        data[key] = value
        return true
    }

    @Implementation
    fun encode(key: String, value: Float): Boolean {
        data[key] = value
        return true
    }

    @Implementation
    fun encode(key: String, value: Set<String>?): Boolean {
        if (value != null) data[key] = HashSet(value) else data.remove(key)
        return true
    }

    @Implementation
    fun decodeInt(key: String): Int {
        return (data[key] as? Int) ?: 0
    }

    @Implementation
    fun decodeInt(key: String, defaultValue: Int): Int {
        return (data[key] as? Int) ?: defaultValue
    }

    @Implementation
    fun decodeString(key: String): String? {
        return data[key] as? String
    }

    @Implementation
    fun decodeString(key: String, defaultValue: String?): String? {
        return (data[key] as? String) ?: defaultValue
    }

    @Implementation
    fun decodeBool(key: String): Boolean {
        return (data[key] as? Boolean) ?: false
    }

    @Implementation
    fun decodeBool(key: String, defaultValue: Boolean): Boolean {
        return (data[key] as? Boolean) ?: defaultValue
    }

    @Implementation
    fun decodeFloat(key: String): Float {
        return (data[key] as? Float) ?: 0f
    }

    @Implementation
    fun decodeFloat(key: String, defaultValue: Float): Float {
        return (data[key] as? Float) ?: defaultValue
    }

    @Suppress("UNCHECKED_CAST")
    @Implementation
    fun decodeStringSet(key: String): Set<String>? {
        return data[key] as? Set<String>
    }

    @Suppress("UNCHECKED_CAST")
    @Implementation
    fun decodeStringSet(key: String, defaultValue: Set<String>?): Set<String>? {
        return (data[key] as? Set<String>) ?: defaultValue
    }

    companion object {
        private val instances = ConcurrentHashMap<String, MMKV>()

        @Implementation
        @JvmStatic
        fun initialize(context: Any?): String {
            return "/tmp/mmkv_shadow"
        }

        @Implementation
        @JvmStatic
        fun initialize(rootDir: String?): String {
            return rootDir ?: "/tmp/mmkv_shadow"
        }

        @Implementation
        @JvmStatic
        fun mmkvWithID(mmapID: String?): MMKV {
            val id = mmapID ?: "default"
            return instances.getOrPut(id) {
                Shadow.newInstanceOf(MMKV::class.java)
            }
        }

        @Implementation
        @JvmStatic
        fun defaultMMKV(): MMKV {
            return mmkvWithID("default")
        }

        @Resetter
        @JvmStatic
        fun reset() {
            instances.clear()
        }
    }
}
