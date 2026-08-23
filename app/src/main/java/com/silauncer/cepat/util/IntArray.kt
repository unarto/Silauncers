package com.silauncer.cepat.util

import java.util.Arrays
import java.util.StringTokenizer

/**
 * IntArray
 *
 * // [Jalur Class]: com.silauncer.cepat.util.IntArray
 * // [Penjelasan]: Array int primitif dinamis berkinerja tinggi tanpa autoboxing (adaptasi dari android.util.IntArray AOSP).
 */
class IntArray(initialCapacity: Int = 10) : Cloneable, Iterable<Int> {

    companion object {
        private const val MIN_CAPACITY_INCREMENT = 12

        @JvmStatic
        fun wrap(vararg array: Int): com.silauncer.cepat.util.IntArray {
            val ia = com.silauncer.cepat.util.IntArray(array.size)
            ia.mValues = array
            ia.mSize = array.size
            return ia
        }

        @JvmStatic
        fun fromConcatString(concatString: String): com.silauncer.cepat.util.IntArray {
            val tokenizer = StringTokenizer(concatString, ",")
            val array = kotlin.IntArray(tokenizer.countTokens())
            var count = 0
            while (tokenizer.hasMoreTokens()) {
                array[count++] = tokenizer.nextToken().trim().toInt()
            }
            val ia = com.silauncer.cepat.util.IntArray(array.size)
            ia.mValues = array
            ia.mSize = array.size
            return ia
        }
    }

    var mValues: kotlin.IntArray = if (initialCapacity == 0) kotlin.IntArray(0) else kotlin.IntArray(initialCapacity)
        internal set

    var mSize: Int = 0
        internal set

    /**
     * // [Jalur Class]: com.silauncer.cepat.util.IntArray
     * // [Penjelasan]: Menambahkan nilai ke bagian akhir array.
     */
    fun add(value: Int) {
        add(mSize, value)
    }

    /**
     * // [Jalur Class]: com.silauncer.cepat.util.IntArray
     * // [Penjelasan]: Menyisipkan nilai pada posisi indeks tertentu.
     */
    fun add(index: Int, value: Int) {
        ensureCapacity(1)
        val rightSegment = mSize - index
        mSize++
        checkBounds(mSize, index)

        if (rightSegment != 0) {
            System.arraycopy(mValues, index, mValues, index + 1, rightSegment)
        }

        mValues[index] = value
    }

    /**
     * // [Jalur Class]: com.silauncer.cepat.util.IntArray
     * // [Penjelasan]: Menambahkan semua nilai dari objek IntArray lain.
     */
    fun addAll(values: com.silauncer.cepat.util.IntArray) {
        val count = values.mSize
        ensureCapacity(count)
        System.arraycopy(values.mValues, 0, mValues, mSize, count)
        mSize += count
    }

    /**
     * // [Jalur Class]: com.silauncer.cepat.util.IntArray
     * // [Penjelasan]: Menyalin seluruh isi dari IntArray lain.
     */
    fun copyFrom(other: com.silauncer.cepat.util.IntArray) {
        clear()
        addAll(other)
    }

    private fun ensureCapacity(count: Int) {
        val currentSize = mSize
        val minCapacity = currentSize + count
        if (minCapacity >= mValues.size) {
            val targetCap = currentSize + if (currentSize < MIN_CAPACITY_INCREMENT / 2) MIN_CAPACITY_INCREMENT else currentSize shr 1
            val newCapacity = if (targetCap > minCapacity) targetCap else minCapacity
            val newValues = kotlin.IntArray(newCapacity)
            System.arraycopy(mValues, 0, newValues, 0, currentSize)
            mValues = newValues
        }
    }

    /**
     * // [Jalur Class]: com.silauncer.cepat.util.IntArray
     * // [Penjelasan]: Mengosongkan isi array.
     */
    fun clear() {
        mSize = 0
    }

    public override fun clone(): com.silauncer.cepat.util.IntArray {
        return wrap(*toArray())
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other is com.silauncer.cepat.util.IntArray) {
            if (mSize == other.mSize) {
                for (i in 0 until mSize) {
                    if (mValues[i] != other.mValues[i]) return false
                }
                return true
            }
        }
        return false
    }

    override fun hashCode(): Int {
        var result = mSize
        for (i in 0 until mSize) {
            result = 31 * result + mValues[i]
        }
        return result
    }

    operator fun get(index: Int): Int {
        checkBounds(mSize, index)
        return mValues[index]
    }

    operator fun set(index: Int, value: Int) {
        checkBounds(mSize, index)
        mValues[index] = value
    }

    fun indexOf(value: Int): Int {
        for (i in 0 until mSize) {
            if (mValues[i] == value) return i
        }
        return -1
    }

    fun contains(value: Int): Boolean = indexOf(value) >= 0

    fun isEmpty(): Boolean = mSize == 0

    fun removeIndex(index: Int) {
        checkBounds(mSize, index)
        System.arraycopy(mValues, index + 1, mValues, index, mSize - index - 1)
        mSize--
    }

    fun removeValue(value: Int) {
        val index = indexOf(value)
        if (index >= 0) {
            removeIndex(index)
        }
    }

    fun removeAllValues(values: com.silauncer.cepat.util.IntArray?) {
        if (values == null) return
        for (i in 0 until values.mSize) {
            removeValue(values.mValues[i])
        }
    }

    fun size(): Int = mSize

    fun toArray(): kotlin.IntArray = if (mSize == 0) kotlin.IntArray(0) else Arrays.copyOf(mValues, mSize)

    fun toConcatString(): String {
        val b = StringBuilder()
        for (i in 0 until mSize) {
            if (i > 0) b.append(", ")
            b.append(mValues[i])
        }
        return b.toString()
    }

    private fun checkBounds(len: Int, index: Int) {
        if (index < 0 || len <= index) {
            throw ArrayIndexOutOfBoundsException("length=$len; index=$index")
        }
    }

    override fun iterator(): Iterator<Int> = object : Iterator<Int> {
        private var mNextIndex = 0

        override fun hasNext(): Boolean = mNextIndex < size()

        override fun next(): Int = get(mNextIndex++)
    }
}
