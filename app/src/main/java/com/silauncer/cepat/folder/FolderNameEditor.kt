package com.silauncer.cepat.folder

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText

/**
 * FolderNameEditor
 *
 * // [Penjelasan]: Mengelola seluruh logika pengeditan nama folder, termasuk validasi, auto-fallback ke nama kategori pintar (smart suggestion), dan interaksi keyboard.
 */
class FolderNameEditor(
    private val context: Context,
    private val titleEditText: EditText
) {
    var folderInfo: FolderInfo? = null

    init {
        // [Penjelasan]: Mengatur listener pengeditan nama folder, memantau perubahan teks secara real-time.
        titleEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                folderInfo?.setTitle(s?.toString()?.trim() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // [Penjelasan]: Ketika fokus terlepas (fokus hilang), jika judul dikosongkan oleh pengguna, kembalikan otomatis (fallback) ke nama kategori pintar.
        titleEditText.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val info = folderInfo
                if (info != null) {
                    val currentText = titleEditText.text.toString().trim()
                    if (currentText.isEmpty()) {
                        val provider = FolderNameProvider(context)
                        val fallbackTitle = provider.getSuggestedFolderName(info.getItems())
                        titleEditText.setText(fallbackTitle)
                        info.setTitle(fallbackTitle)
                    }
                }
            }
        }

        titleEditText.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER)
            ) {
                hideKeyboard(v)
                v.clearFocus()
                true
            } else {
                false
            }
        }
    }

    fun bind(info: FolderInfo) {
        this.folderInfo = info
        titleEditText.setText(info.title)
        
        // [Penjelasan]: Hitung dan atur saran kategori pintar (smart suggestion) via hint
        val provider = FolderNameProvider(context)
        titleEditText.hint = provider.getSuggestedFolderName(info.getItems())
    }

    fun unbind() {
        folderInfo = null
    }

    fun updateTitle(newTitle: String) {
        if (titleEditText.text.toString() != newTitle) {
            titleEditText.setText(newTitle)
        }
    }

    fun clearFocus() {
        hideKeyboard(titleEditText)
        titleEditText.clearFocus()
    }

    private fun hideKeyboard(view: View) {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(view.windowToken, 0)
    }
}
