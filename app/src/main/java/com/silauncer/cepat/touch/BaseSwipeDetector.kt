package com.silauncer.cepat.touch

import android.content.Context
import android.graphics.PointF
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.ViewConfiguration
import java.util.ArrayDeque
import java.util.Queue

/**
 * Scroll/drag/swipe gesture detector.
 * Abstract base class for calculating touch displacement, velocity tracking, and touch slop.
 */
abstract class BaseSwipeDetector @JvmOverloads constructor(
    context: Context,
    config: ViewConfiguration,
    protected val isRtl: Boolean = false
) {
    companion object {
        private const val ANIMATION_DURATION = 1200f
        const val INVALID_POINTER_ID = -1

        @JvmStatic
        fun calculateDuration(velocity: Float, progressNeeded: Float): Long {
            val velocityDivisor = Math.max(2f, Math.abs(0.5f * velocity))
            val travelDistance = Math.max(0.2f, progressNeeded)
            return Math.max(100f, (ANIMATION_DURATION / velocityDivisor) * travelDistance).toLong()
        }
    }

    enum class ScrollState {
        IDLE,
        DRAGGING,
        SETTLING
    }

    protected val context: Context = context.applicationContext ?: context
    protected val touchSlop: Float = config.scaledTouchSlop.toFloat()
    protected val maxVelocity: Float = config.scaledMaximumFlingVelocity.toFloat()
    private val releaseVelocity: Float = touchSlop * 10f

    val downPos = PointF()
    val lastPos = PointF()
    private val lastDisplacement = PointF()
    private val displacement = PointF()
    protected val subtractDisplacement = PointF()

    var scrollState = ScrollState.IDLE
        protected set

    var ignoreSlopWhenSettling: Boolean = false

    private var activePointerId = INVALID_POINTER_ID
    private var velocityTracker: VelocityTracker? = null
    private var isSettingState = false
    private val setStateQueue: Queue<Runnable> = ArrayDeque()

    fun getDownX(): Int = downPos.x.toInt()
    fun getDownY(): Int = downPos.y.toInt()

    fun isIdleState(): Boolean = scrollState == ScrollState.IDLE
    fun isSettlingState(): Boolean = scrollState == ScrollState.SETTLING
    fun isDraggingState(): Boolean = scrollState == ScrollState.DRAGGING
    fun isDraggingOrSettling(): Boolean = scrollState == ScrollState.DRAGGING || scrollState == ScrollState.SETTLING

    fun finishedScrolling() {
        setState(ScrollState.IDLE)
    }

    fun isFling(velocity: Float): Boolean = Math.abs(velocity) > releaseVelocity

    open fun onTouchEvent(ev: MotionEvent): Boolean {
        val actionMasked = ev.actionMasked
        if (actionMasked == MotionEvent.ACTION_DOWN && velocityTracker != null) {
            velocityTracker?.clear()
        }
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain()
        }
        velocityTracker?.addMovement(ev)

        when (actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                activePointerId = ev.getPointerId(0)
                downPos.set(ev.x, ev.y)
                lastPos.set(downPos)
                lastDisplacement.set(0f, 0f)
                displacement.set(0f, 0f)

                if (scrollState == ScrollState.SETTLING && ignoreSlopWhenSettling) {
                    setState(ScrollState.DRAGGING)
                }
            }
            MotionEvent.ACTION_POINTER_UP -> {
                val ptrIdx = ev.actionIndex
                val ptrId = ev.getPointerId(ptrIdx)
                if (ptrId == activePointerId) {
                    val newPointerIdx = if (ptrIdx == 0) 1 else 0
                    if (newPointerIdx < ev.pointerCount) {
                        downPos.set(
                            ev.getX(newPointerIdx) - (lastPos.x - downPos.x),
                            ev.getY(newPointerIdx) - (lastPos.y - downPos.y)
                        )
                        lastPos.set(ev.getX(newPointerIdx), ev.getY(newPointerIdx))
                        activePointerId = ev.getPointerId(newPointerIdx)
                    }
                }
            }
            MotionEvent.ACTION_MOVE -> {
                val pointerIndex = ev.findPointerIndex(activePointerId)
                if (pointerIndex != INVALID_POINTER_ID && pointerIndex < ev.pointerCount) {
                    displacement.set(
                        ev.getX(pointerIndex) - downPos.x,
                        ev.getY(pointerIndex) - downPos.y
                    )
                    if (isRtl) {
                        displacement.x = -displacement.x
                    }

                    if (scrollState != ScrollState.DRAGGING && shouldScrollStart(displacement)) {
                        setState(ScrollState.DRAGGING)
                    }
                    if (scrollState == ScrollState.DRAGGING) {
                        reportDragging(ev)
                    }
                    lastPos.set(ev.getX(pointerIndex), ev.getY(pointerIndex))
                }
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                if (scrollState == ScrollState.DRAGGING) {
                    setState(ScrollState.SETTLING)
                }
                velocityTracker?.recycle()
                velocityTracker = null
            }
        }
        return true
    }

    private fun setState(newState: ScrollState) {
        if (isSettingState) {
            setStateQueue.add(Runnable { setState(newState) })
            return
        }
        isSettingState = true

        if (newState == ScrollState.DRAGGING) {
            initializeDragging()
            if (scrollState == ScrollState.IDLE) {
                reportDragStart(recatch = false)
            } else if (scrollState == ScrollState.SETTLING) {
                reportDragStart(recatch = true)
            }
        }
        if (newState == ScrollState.SETTLING) {
            reportDragEnd()
        }

        scrollState = newState
        isSettingState = false
        if (!setStateQueue.isEmpty()) {
            setStateQueue.poll()?.run()
        }
    }

    private fun initializeDragging() {
        if (scrollState == ScrollState.SETTLING && ignoreSlopWhenSettling) {
            subtractDisplacement.set(0f, 0f)
        } else {
            subtractDisplacement.x = if (displacement.x > 0) touchSlop else -touchSlop
            subtractDisplacement.y = if (displacement.y > 0) touchSlop else -touchSlop
        }
    }

    protected abstract fun shouldScrollStart(displacement: PointF): Boolean

    private fun reportDragStart(recatch: Boolean) {
        reportDragStartInternal(recatch)
    }

    protected abstract fun reportDragStartInternal(recatch: Boolean)

    private fun reportDragging(event: MotionEvent) {
        if (displacement != lastDisplacement) {
            lastDisplacement.set(displacement)
            val tempPoint = PointF(
                displacement.x - subtractDisplacement.x,
                displacement.y - subtractDisplacement.y
            )
            reportDraggingInternal(tempPoint, event)
        }
    }

    protected abstract fun reportDraggingInternal(displacement: PointF, event: MotionEvent)

    private fun reportDragEnd() {
        val tracker = velocityTracker
        val velocity = if (tracker != null) {
            tracker.computeCurrentVelocity(1000, maxVelocity)
            PointF(tracker.xVelocity / 1000f, tracker.yVelocity / 1000f)
        } else {
            PointF(0f, 0f)
        }
        if (isRtl) {
            velocity.x = -velocity.x
        }
        reportDragEndInternal(velocity)
    }

    protected abstract fun reportDragEndInternal(velocity: PointF)
}
