package com.silauncer.cepat.folder

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.res.Resources
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.silauncer.cepat.R
import com.silauncer.cepat.anim.Interpolators

// [Jalur Class]: com.silauncer.cepat.folder.FolderAnimationController
// [Penjelasan]: Mengelola seluruh logika animasi buka, tutup, dan pemudaran drag-exit modal folder menggunakan kurva interpolasi FastOutSlowIn/Emphasized serta transisi scale (0.85f ke 1.0f) dan alpha (0f ke 1f) yang halus ala Launcher3 AOSP (diadaptasi dari Launcher3-aml_ips_340914000/src/com/android/launcher3/anim/).
class FolderAnimationController(
    private val scrimView: View,
    private val cardContainer: View,
    private val resources: Resources,
    private val wallpaperView: View? = null,
    private val dimOverlayView: View? = null
) {
    var isAnimating = false
        private set

    private var currentAnimator: AnimatorSet? = null

    // [Jalur Class]: com.silauncer.cepat.folder.FolderAnimationController
    // [Penjelasan]: Membuka folder dengan transisi skala (0.85f ke 1.0f) dan transparansi (0f ke 1f) menggunakan kurva Interpolators.FAST_OUT_SLOW_IN dari AOSP Launcher3 untuk efek ekspansi yang alami dan mulus.
    fun animateOpen(onEnd: () -> Unit) {
        isAnimating = true
        val duration = resources.getInteger(R.integer.folder_animation_duration_ms).toLong()

        scrimView.alpha = 0f
        wallpaperView?.alpha = 0f
        dimOverlayView?.alpha = 0f
        cardContainer.alpha = 0f
        cardContainer.scaleX = 0.85f
        cardContainer.scaleY = 0.85f

        val scrimAnim = ObjectAnimator.ofFloat(scrimView, View.ALPHA, 0f, 1f)
        val cardAlpha = ObjectAnimator.ofFloat(cardContainer, View.ALPHA, 0f, 1f)
        val cardScaleX = ObjectAnimator.ofFloat(cardContainer, View.SCALE_X, 0.85f, 1.0f)
        val cardScaleY = ObjectAnimator.ofFloat(cardContainer, View.SCALE_Y, 0.85f, 1.0f)

        val animList = mutableListOf<Animator>(scrimAnim, cardAlpha, cardScaleX, cardScaleY)
        wallpaperView?.let { animList.add(ObjectAnimator.ofFloat(it, View.ALPHA, 0f, 1f)) }
        dimOverlayView?.let { animList.add(ObjectAnimator.ofFloat(it, View.ALPHA, 0f, 1f)) }

        val set = AnimatorSet().apply {
            playTogether(animList)
            this.duration = duration
            interpolator = Interpolators.FAST_OUT_SLOW_IN
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    isAnimating = false
                    if (currentAnimator == this@apply) {
                        currentAnimator = null
                    }
                    onEnd()
                }
            })
        }

        currentAnimator?.cancel()
        currentAnimator = set
        set.start()
    }

    // [Jalur Class]: com.silauncer.cepat.folder.FolderAnimationController
    // [Penjelasan]: Menutup folder dengan transisi skala (1.0f ke 0.85f) dan transparansi (1f ke 0f) menggunakan Interpolators.FAST_OUT_SLOW_IN dari AOSP Launcher3 untuk penutupan yang responsif dan konsisten.
    fun animateClose(onEnd: () -> Unit) {
        isAnimating = true
        val duration = (resources.getInteger(R.integer.folder_animation_duration_ms) * 0.8f).toLong()

        val scrimAnim = ObjectAnimator.ofFloat(scrimView, View.ALPHA, 1f, 0f)
        val cardAlpha = ObjectAnimator.ofFloat(cardContainer, View.ALPHA, 1f, 0f)
        val cardScaleX = ObjectAnimator.ofFloat(cardContainer, View.SCALE_X, 1.0f, 0.85f)
        val cardScaleY = ObjectAnimator.ofFloat(cardContainer, View.SCALE_Y, 1.0f, 0.85f)

        val animList = mutableListOf<Animator>(scrimAnim, cardAlpha, cardScaleX, cardScaleY)
        wallpaperView?.let { animList.add(ObjectAnimator.ofFloat(it, View.ALPHA, 1f, 0f)) }
        dimOverlayView?.let { animList.add(ObjectAnimator.ofFloat(it, View.ALPHA, 1f, 0f)) }

        val set = AnimatorSet().apply {
            playTogether(animList)
            this.duration = duration
            interpolator = Interpolators.FAST_OUT_SLOW_IN
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    isAnimating = false
                    if (currentAnimator == this@apply) {
                        currentAnimator = null
                    }
                    onEnd()
                }
            })
        }

        currentAnimator?.cancel()
        currentAnimator = set
        set.start()
    }

    // [Jalur Class]: com.silauncer.cepat.folder.FolderAnimationController
    // [Penjelasan]: Menganimasikan pemudaran cepat (fade-out) dan scale down pada overlay modal folder saat item ditarik keluar, dengan mempertahankan visibilitas penuh ikon yang sedang diseret (360 derajat)
    fun animateFadeOutForDragExit(
        draggedView: View? = null,
        onEnd: () -> Unit = {}
    ) {
        isAnimating = true
        val animList = mutableListOf<Animator>()

        animList.add(ObjectAnimator.ofFloat(scrimView, View.ALPHA, scrimView.alpha, 0f))
        wallpaperView?.let { animList.add(ObjectAnimator.ofFloat(it, View.ALPHA, it.alpha, 0f)) }
        dimOverlayView?.let { animList.add(ObjectAnimator.ofFloat(it, View.ALPHA, it.alpha, 0f)) }

        if (draggedView != null) {
            // [Penjelasan]: Memudarkan elemen-elemen saudara di dalam folder modal agar layar workspace di belakang terlihat jelas, namun ikon yang di-drag tetap terlihat
            if (cardContainer is ViewGroup) {
                for (i in 0 until cardContainer.childCount) {
                    val child = cardContainer.getChildAt(i)
                    if (child is RecyclerView) {
                        for (j in 0 until child.childCount) {
                            val itemChild = child.getChildAt(j)
                            if (itemChild != draggedView) {
                                animList.add(ObjectAnimator.ofFloat(itemChild, View.ALPHA, itemChild.alpha, 0f))
                            }
                        }
                    } else {
                        animList.add(ObjectAnimator.ofFloat(child, View.ALPHA, child.alpha, 0f))
                    }
                }
            }
        } else {
            animList.add(ObjectAnimator.ofFloat(cardContainer, View.ALPHA, cardContainer.alpha, 0f))
            animList.add(ObjectAnimator.ofFloat(cardContainer, View.SCALE_X, cardContainer.scaleX, 0.85f))
            animList.add(ObjectAnimator.ofFloat(cardContainer, View.SCALE_Y, cardContainer.scaleY, 0.85f))
        }

        val set = AnimatorSet().apply {
            playTogether(animList)
            this.duration = 180
            interpolator = Interpolators.FAST_OUT_SLOW_IN
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    isAnimating = false
                    if (currentAnimator == this@apply) {
                        currentAnimator = null
                    }
                    onEnd()
                }
            })
        }

        currentAnimator?.cancel()
        currentAnimator = set
        set.start()
    }

    fun cancel() {
        currentAnimator?.cancel()
        currentAnimator = null
        isAnimating = false
    }
}
