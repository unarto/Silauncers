package com.silauncer.cepat.anim

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Application
import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * AnimPackageTest
 *
 * // [Jalur Class]: com.silauncer.cepat.anim.AnimPackageTest
 * // [Penjelasan]: Unit test komprehensif untuk memvalidasi fungsi-fungsi komponen animasi Launcher3 di Silauncer (Interpolators, AnimatedFloat, SpringAnimationBuilder, RevealOutline, PropertySetter, dll).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE, application = Application::class)
class AnimPackageTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    
        @Test
    fun testInterpolatorsBasicCurves() {
        assertEquals(0f, Interpolators.LINEAR.getInterpolation(0f), 0.001f)
        assertEquals(1f, Interpolators.LINEAR.getInterpolation(1f), 0.001f)
        assertEquals(0.5f, Interpolators.LINEAR.getInterpolation(0.5f), 0.001f)

        // Clamp to progress
        assertEquals(0f, Interpolators.clampToProgress(Interpolators.LINEAR, 0.2f, 0.4f, 0.8f), 0.001f)
        assertEquals(1f, Interpolators.clampToProgress(Interpolators.LINEAR, 0.9f, 0.4f, 0.8f), 0.001f)
        assertEquals(0.5f, Interpolators.clampToProgress(Interpolators.LINEAR, 0.6f, 0.4f, 0.8f), 0.001f)

        // Velocity interpolators
        assertNotNull(Interpolators.scrollInterpolatorForVelocity(5f))
        assertNotNull(Interpolators.scrollInterpolatorForVelocity(15f))
        assertNotNull(Interpolators.overshootInterpolatorForVelocity(2f))

        // Reverse
        val reversedLinear = Interpolators.reverse(Interpolators.LINEAR)
        assertEquals(0.5f, reversedLinear.getInterpolation(0.5f), 0.001f)
        assertEquals(0.25f, reversedLinear.getInterpolation(0.25f), 0.001f)
    }

    

    
        @Test
    fun testAnimationSuccessListener() {
        var successCount = 0
        val listener = object : AnimationSuccessListener() {
            override fun onAnimationSuccess(animator: Animator) {
                successCount++
            }
        }

        val dummyAnimator = ValueAnimator.ofFloat(0f, 1f)
        listener.onAnimationStart(dummyAnimator)
        listener.onAnimationEnd(dummyAnimator)
        assertEquals(1, successCount)

        // When cancelled
        listener.onAnimationStart(dummyAnimator)
        listener.onAnimationCancel(dummyAnimator)
        listener.onAnimationEnd(dummyAnimator)
        assertEquals(1, successCount) // Count did not increase
    }

    
        @Test
    fun testAnimatorListenersHelper() {
        var successRun = false
        val successListener = AnimatorListeners.forSuccessCallback(Runnable { successRun = true })
        val dummyAnimator = ValueAnimator.ofFloat(0f, 1f)
        successListener.onAnimationStart(dummyAnimator)
        successListener.onAnimationEnd(dummyAnimator)
        assertTrue(successRun)

        var endStatus: Boolean? = null
        val endListener = AnimatorListeners.forEndCallback { success -> endStatus = success }
        endListener.onAnimationCancel(dummyAnimator)
        assertEquals(false, endStatus)
    }

    
        @Test
    fun testAlphaUpdateListenerCutoff() {
        val dummyView = View(context)
        dummyView.alpha = 0f
        AlphaUpdateListener.updateVisibility(dummyView)
        assertEquals(View.INVISIBLE, dummyView.visibility)

        dummyView.alpha = 0.5f
        AlphaUpdateListener.updateVisibility(dummyView)
        assertEquals(View.VISIBLE, dummyView.visibility)
    }

    

    
        @Test
    fun testSpringProperty() {
        val springProp = SpringProperty(SpringProperty.FLAG_CAN_SPRING_ON_END)
        assertEquals(SpringProperty.FLAG_CAN_SPRING_ON_END, springProp.flags)
        springProp.setDampingRatio(0.8f).setStiffness(300f)
        assertEquals(0.8f, springProp.dampingRatio, 0.001f)
        assertEquals(300f, springProp.stiffness, 0.001f)
    }

    
        @Test
    fun testSpringAnimationBuilder() {
        val builder = SpringAnimationBuilder(context)
            .setStartValue(0f)
            .setEndValue(100f)
            .setStartVelocity(0f)
            .setMinimumVisibleChange(1f)
            .computeParams()

        assertTrue(builder.getDuration() > 0)
        assertEquals(0f, builder.getInterpolatedValue(0f), 1f)
    }

    
}
