package com.silauncer.cepat.dragndrop

import android.view.View
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.recyclerview.widget.RecyclerView
import com.silauncer.cepat.folder.FolderInfo
import com.silauncer.cepat.launcher.LauncherItem

/**
 * [Jalur Class]: com.silauncer.cepat.dragndrop.DragSpringAnimationHelper
 * [Penjelasan]: Kelas pembantu modular yang mengisolasi animasi fisika pegas (SpringAnimation Physics)
 * untuk item yang mendarat di workspace dan item yang terdisolusi saat folder auto-disband ala Launcher3 AOSP.
 */
class DragSpringAnimationHelper {

    companion object {
        const val SCALE_LANDING_BOUNCE_DEFAULT = 1.08f
        const val SCALE_LANDING_BOUNCE_DRAGOUT = 1.18f
        const val SCALE_DISSOLVED_DEFAULT = 0.85f
    }

    // [Jalur Class]: com.silauncer.cepat.dragndrop.DragSpringAnimationHelper
    // [Penjelasan]: Menerapkan animasi pegas mikro (bounce effect) pada view item yang baru dijatuhkan pada sel target
    fun animateLandingBounce(targetView: View, initialScale: Float = SCALE_LANDING_BOUNCE_DEFAULT) {
        targetView.scaleX = initialScale
        targetView.scaleY = initialScale
        val springX = SpringAnimation(targetView, DynamicAnimation.SCALE_X, 1.0f).apply {
            spring.stiffness = SpringForce.STIFFNESS_LOW
            spring.dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
        }
        val springY = SpringAnimation(targetView, DynamicAnimation.SCALE_Y, 1.0f).apply {
            spring.stiffness = SpringForce.STIFFNESS_LOW
            spring.dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
        }
        springX.start()
        springY.start()
    }

    // [Jalur Class]: com.silauncer.cepat.dragndrop.DragSpringAnimationHelper
    // [Penjelasan]: Menerapkan efek pegas pada item sisa dari folder yang dibubarkan secara otomatis (auto-disband)
    fun animateDissolvedItem(dissolvedView: View, initialScale: Float = SCALE_DISSOLVED_DEFAULT) {
        dissolvedView.scaleX = initialScale
        dissolvedView.scaleY = initialScale
        val sX = SpringAnimation(dissolvedView, DynamicAnimation.SCALE_X, 1.0f).apply {
            spring.stiffness = SpringForce.STIFFNESS_LOW
            spring.dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
        }
        val sY = SpringAnimation(dissolvedView, DynamicAnimation.SCALE_Y, 1.0f).apply {
            spring.stiffness = SpringForce.STIFFNESS_LOW
            spring.dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
        }
        sX.start()
        sY.start()
    }

    // [Jalur Class]: com.silauncer.cepat.dragndrop.DragSpringAnimationHelper
    // [Penjelasan]: Mengevaluasi dan memicu animasi pegas untuk item yang ditarik keluar dari folder serta item yang terdisolusi
    fun triggerDragOutAnimations(
        recyclerView: RecyclerView,
        newItems: List<LauncherItem>,
        droppedItem: LauncherItem,
        folderInfo: FolderInfo,
        targetPos: Int
    ) {
        recyclerView.post {
            val droppedIndex = newItems.indexOfFirst { it.id == droppedItem.id }
            val animIndex = if (droppedIndex != -1) droppedIndex else targetPos
            val vh = recyclerView.findViewHolderForAdapterPosition(animIndex)
            vh?.itemView?.let { targetView ->
                animateLandingBounce(targetView, initialScale = SCALE_LANDING_BOUNCE_DRAGOUT)
            }

            val remainingApp = folderInfo.getSingleRemainingApp()
            val remainingShortcut = folderInfo.getSingleRemainingShortcut()
            if (remainingApp != null || remainingShortcut != null) {
                val dissolvedIndex = newItems.indexOfFirst {
                    if (remainingApp != null) {
                        (it as? LauncherItem.App)?.appInfo?.cacheKey == remainingApp.cacheKey
                    } else if (remainingShortcut != null) {
                        (it as? LauncherItem.Shortcut)?.shortcutInfo?.cacheKey == remainingShortcut.cacheKey
                    } else false
                }
                if (dissolvedIndex != -1 && dissolvedIndex != animIndex) {
                    val vhDissolved = recyclerView.findViewHolderForAdapterPosition(dissolvedIndex)
                    vhDissolved?.itemView?.let { dView ->
                        animateDissolvedItem(dView, initialScale = SCALE_DISSOLVED_DEFAULT)
                    }
                }
            }
        }
    }
}
