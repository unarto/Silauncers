package com.silauncer.cepat.keyboard

import android.graphics.Canvas
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.silauncer.cepat.keyboard.FocusIndicatorHelper.SimpleFocusIndicatorHelper

/**
 * FocusedItemDecorator
 *
 * // [Jalur Class]: com.silauncer.cepat.keyboard.FocusedItemDecorator
 * // [Penjelasan]: RecyclerView.ItemDecoration untuk menggambar dan menganimasikan highlight latar belakang item yang terfokus
 * (adaptasi dari AOSP Launcher3 FocusedItemDecorator).
 */
class FocusedItemDecorator(
    container: View,
    private val helper: FocusIndicatorHelper = SimpleFocusIndicatorHelper(container)
) : RecyclerView.ItemDecoration() {

    fun getFocusListener(): View.OnFocusChangeListener = helper

    fun getFocusHelper(): FocusIndicatorHelper = helper

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        helper.draw(c)
    }
}
