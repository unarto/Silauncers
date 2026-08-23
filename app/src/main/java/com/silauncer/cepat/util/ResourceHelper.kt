package com.silauncer.cepat.util

import android.content.Context
import android.content.res.TypedArray
import android.content.res.XmlResourceParser
import android.util.AttributeSet

/**
 * Helper that can be subclassed in tests to provide a way to parse attributes correctly.
 *
 * // [Jalur Class]: com.silauncer.cepat.util.ResourceHelper
 * // [Penjelasan]: Utilitas pembantu untuk mengekstrak dan memproses berkas XML serta atribut kustom (styleables) dari resource sistem/aplikasi.
 */
open class ResourceHelper(private val context: Context, private val specsFileId: Int) {
    open fun getXml(): XmlResourceParser {
        // // [Jalur Class]: com.silauncer.cepat.util.ResourceHelper
        // // [Penjelasan]: Mendapatkan XmlResourceParser dari resource ID yang diberikan.
        return context.resources.getXml(specsFileId)
    }

    open fun obtainStyledAttributes(attrs: AttributeSet, styleId: kotlin.IntArray): TypedArray {
        // // [Jalur Class]: com.silauncer.cepat.util.ResourceHelper
        // // [Penjelasan]: Mengambil atribut ber-style berdasarkan AttributeSet dan ID styleable menggunakan kotlin.IntArray untuk menghindari shadowing dari IntArray kustom.
        return context.obtainStyledAttributes(attrs, styleId)
    }
}
