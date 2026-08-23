package com.silauncer.cepat.touch

import android.content.Context
import android.graphics.PointF
import android.view.MotionEvent
import android.view.ViewConfiguration

/**
 * One dimensional scroll/drag/swipe gesture detector (HORIZONTAL or VERTICAL).
 */
class SingleAxisSwipeDetector @JvmOverloads constructor(
    context: Context,
    config: ViewConfiguration = ViewConfiguration.get(context),
    private val listener: Listener,
    private val direction: Direction,
    isRtl: Boolean = false
) : BaseSwipeDetector(context, config, isRtl) {

    companion object {
        const val DIRECTION_POSITIVE = 1 shl 0
        const val DIRECTION_NEGATIVE = 1 shl 1
        const val DIRECTION_BOTH = DIRECTION_NEGATIVE or DIRECTION_POSITIVE

        @JvmField
        val VERTICAL: Direction = object : Direction() {
            override fun isPositive(displacement: Float): Boolean = displacement < 0 // Up
            override fun isNegative(displacement: Float): Boolean = displacement > 0 // Down
            override fun extractDirection(point: PointF): Float = point.y
            override fun extractOrthogonalDirection(point: PointF): Float = point.x
            override fun toString(): String = "VERTICAL"
        }

        @JvmField
        val HORIZONTAL: Direction = object : Direction() {
            override fun isPositive(displacement: Float): Boolean = displacement > 0 // Right
            override fun isNegative(displacement: Float): Boolean = displacement < 0 // Left
            override fun extractDirection(point: PointF): Float = point.x
            override fun extractOrthogonalDirection(point: PointF): Float = point.y
            override fun toString(): String = "HORIZONTAL"
        }
    }

    private var scrollDirections = 0
    private var touchSlopMultiplier = 1f

    fun setTouchSlopMultiplier(touchSlopMultiplier: Float) {
        this.touchSlopMultiplier = touchSlopMultiplier
    }

    fun setDetectableScrollConditions(scrollDirectionFlags: Int, ignoreSlop: Boolean) {
        this.scrollDirections = scrollDirectionFlags
        this.ignoreSlopWhenSettling = ignoreSlop
    }

    fun wasInitialTouchPositive(): Boolean {
        return direction.isPositive(direction.extractDirection(subtractDisplacement))
    }

    override fun shouldScrollStart(displacement: PointF): Boolean {
        val minDisplacement = Math.max(
            touchSlop * touchSlopMultiplier,
            Math.abs(direction.extractOrthogonalDirection(displacement))
        )
        if (Math.abs(direction.extractDirection(displacement)) < minDisplacement) {
            return false
        }

        val displacementComponent = direction.extractDirection(displacement)
        return canScrollNegative(displacementComponent) || canScrollPositive(displacementComponent)
    }

    private fun canScrollNegative(displacement: Float): Boolean {
        return (scrollDirections and DIRECTION_NEGATIVE) > 0 && direction.isNegative(displacement)
    }

    private fun canScrollPositive(displacement: Float): Boolean {
        return (scrollDirections and DIRECTION_POSITIVE) > 0 && direction.isPositive(displacement)
    }

    override fun reportDragStartInternal(recatch: Boolean) {
        val startDisplacement = direction.extractDirection(subtractDisplacement)
        listener.onDragStart(!recatch, startDisplacement)
    }

    override fun reportDraggingInternal(displacement: PointF, event: MotionEvent) {
        listener.onDrag(
            direction.extractDirection(displacement),
            direction.extractOrthogonalDirection(displacement),
            event
        )
    }

    override fun reportDragEndInternal(velocity: PointF) {
        val velocityComponent = direction.extractDirection(velocity)
        listener.onDragEnd(velocityComponent)
    }

    interface Listener {
        fun onDragStart(start: Boolean, startDisplacement: Float)
        fun onDrag(displacement: Float): Boolean
        fun onDrag(displacement: Float, event: MotionEvent): Boolean {
            return onDrag(displacement)
        }
        fun onDrag(displacement: Float, orthogonalDisplacement: Float, ev: MotionEvent): Boolean {
            return onDrag(displacement, ev)
        }
        fun onDragEnd(velocity: Float)
    }

    abstract class Direction {
        abstract fun isPositive(displacement: Float): Boolean
        abstract fun isNegative(displacement: Float): Boolean
        abstract fun extractDirection(point: PointF): Float
        abstract fun extractOrthogonalDirection(point: PointF): Float
    }
}
