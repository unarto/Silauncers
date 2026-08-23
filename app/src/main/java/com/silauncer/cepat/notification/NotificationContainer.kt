package com.silauncer.cepat.notification

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.util.FloatProperty
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import com.silauncer.cepat.R
import com.silauncer.cepat.anim.AnimationSuccessListener
import com.silauncer.cepat.anim.Interpolators
import com.silauncer.cepat.touch.BaseSwipeDetector
import com.silauncer.cepat.touch.OverScroll
import com.silauncer.cepat.touch.SingleAxisSwipeDetector

/**
 * NotificationContainer
 *
 * // [Jalur Class]: com.silauncer.cepat.notification.NotificationContainer
 * // [Penjelasan]: Kontainer FrameLayout yang mengelola tampilan dua NotificationMainView (primary dan secondary) dan gestur usap horizontal untuk dismiss notifikasi (adaptasi AOSP Launcher3 NotificationContainer).
 */
class NotificationContainer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), SingleAxisSwipeDetector.Listener {

    companion object {
        val DRAG_TRANSLATION_X = object : FloatProperty<NotificationContainer>("notificationProgress") {
            override fun setValue(view: NotificationContainer, transX: Float) {
                view.setDragTranslationX(transX)
            }

            override fun get(view: NotificationContainer): Float {
                return view.mDragTranslationX
            }
        }

        private val sTempRect = Rect()
    }

    private val mSwipeDetector: SingleAxisSwipeDetector
    private val mNotificationInfos = ArrayList<NotificationInfo>()
    private var mIgnoreTouch = false

    private val mContentTranslateAnimator: ObjectAnimator
    private var mDragTranslationX = 0f

    private val mPrimaryView: NotificationMainView
    private val mSecondaryView: NotificationMainView
    private var mPopupContainer: View? = null

    init {
        mSwipeDetector = SingleAxisSwipeDetector(
            context = context,
            listener = this,
            direction = SingleAxisSwipeDetector.HORIZONTAL
        )
        mSwipeDetector.setDetectableScrollConditions(SingleAxisSwipeDetector.DIRECTION_BOTH, false)
        mContentTranslateAnimator = ObjectAnimator.ofFloat(this, DRAG_TRANSLATION_X, 0f)

        mPrimaryView = View.inflate(context, R.layout.notification_content, null) as NotificationMainView
        mSecondaryView = View.inflate(context, R.layout.notification_content, null) as NotificationMainView
        mSecondaryView.alpha = 0f

        addView(mSecondaryView)
        addView(mPrimaryView)
    }

    fun setPopupView(popupView: View?) {
        mPopupContainer = popupView
    }

    fun onInterceptSwipeEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            sTempRect.set(left, top, right, bottom)
            mIgnoreTouch = !sTempRect.contains(ev.x.toInt(), ev.y.toInt())
            if (!mIgnoreTouch) {
                mPopupContainer?.parent?.requestDisallowInterceptTouchEvent(true)
            }
        }
        if (mIgnoreTouch) {
            return false
        }
        if (mPrimaryView.getNotificationInfo() == null) {
            return false
        }

        mSwipeDetector.onTouchEvent(ev)
        return mSwipeDetector.isDraggingOrSettling()
    }

    fun onSwipeEvent(ev: MotionEvent): Boolean {
        if (mIgnoreTouch) {
            return false
        }
        if (mPrimaryView.getNotificationInfo() == null) {
            return false
        }
        return mSwipeDetector.onTouchEvent(ev)
    }

    fun applyNotificationInfos(notificationInfos: List<NotificationInfo>) {
        mNotificationInfos.clear()
        if (notificationInfos.isEmpty()) {
            mPrimaryView.applyNotificationInfo(null)
            mSecondaryView.applyNotificationInfo(null)
            return
        }
        mNotificationInfos.addAll(notificationInfos)

        val mainNotification = notificationInfos[0]
        mPrimaryView.applyNotificationInfo(mainNotification)
        mSecondaryView.applyNotificationInfo(if (notificationInfos.size > 1) notificationInfos[1] else null)
    }

    fun trimNotifications(notificationKeys: List<String>) {
        val iterator = mNotificationInfos.iterator()
        while (iterator.hasNext()) {
            if (!notificationKeys.contains(iterator.next().notificationKey)) {
                iterator.remove()
            }
        }

        val primaryInfo = if (mNotificationInfos.isNotEmpty()) mNotificationInfos[0] else null
        val secondaryInfo = if (mNotificationInfos.size > 1) mNotificationInfos[1] else null

        mPrimaryView.applyNotificationInfo(primaryInfo)
        mSecondaryView.applyNotificationInfo(secondaryInfo)

        mPrimaryView.onPrimaryDrag(0f)
        mSecondaryView.onSecondaryDrag(0f)
    }

    fun setDragTranslationX(translationX: Float) {
        mDragTranslationX = translationX
        val progress = if (width > 0) translationX / width.toFloat() else 0f
        mPrimaryView.onPrimaryDrag(progress)
        if (mSecondaryView.getNotificationInfo() == null) {
            mSecondaryView.alpha = 0f
        } else {
            mSecondaryView.onSecondaryDrag(progress)
        }
    }

    override fun onDragStart(start: Boolean, startDisplacement: Float) {
        // Disembunyikan panah/arrow popup jika ada
    }

    override fun onDrag(displacement: Float): Boolean {
        var dragDisplacement = displacement
        if (!mPrimaryView.canChildBeDismissed()) {
            dragDisplacement = OverScroll.dampedScroll(dragDisplacement, width)
        }

        val progress = if (width > 0) dragDisplacement / width.toFloat() else 0f
        mPrimaryView.onPrimaryDrag(progress)
        if (mSecondaryView.getNotificationInfo() == null) {
            mSecondaryView.alpha = 0f
        } else {
            mSecondaryView.onSecondaryDrag(progress)
        }
        mContentTranslateAnimator.cancel()
        return true
    }

    override fun onDragEnd(velocity: Float) {
        val willExit: Boolean
        val endTranslation: Float
        val startTranslation = mPrimaryView.translationX
        val w = width.toFloat()

        if (!mPrimaryView.canChildBeDismissed()) {
            willExit = false
            endTranslation = 0f
        } else if (mSwipeDetector.isFling(velocity)) {
            willExit = true
            endTranslation = if (velocity < 0) -w else w
        } else if (Math.abs(startTranslation) > w / 2f) {
            willExit = true
            endTranslation = if (startTranslation < 0) -w else w
        } else {
            willExit = false
            endTranslation = 0f
        }

        val duration = BaseSwipeDetector.calculateDuration(
            velocity,
            if (w > 0) (endTranslation - startTranslation) / w else 0f
        )

        mContentTranslateAnimator.removeAllListeners()
        mContentTranslateAnimator.duration = duration
        mContentTranslateAnimator.interpolator = Interpolators.scrollInterpolatorForVelocity(velocity)
        mContentTranslateAnimator.setFloatValues(startTranslation, endTranslation)

        val current = mPrimaryView
        mContentTranslateAnimator.addListener(object : AnimationSuccessListener() {
            override fun onAnimationSuccess(animator: Animator) {
                mSwipeDetector.finishedScrolling()
                if (willExit) {
                    current.onChildDismissed()
                }
            }
        })
        mContentTranslateAnimator.start()
    }

    fun updateBackgroundColor(color: Int, animatorSetOut: AnimatorSet) {
        mPrimaryView.updateBackgroundColor(color, animatorSetOut)
        mSecondaryView.updateBackgroundColor(color, animatorSetOut)
    }

    fun updateHeader(notificationCount: Int) {
        mPrimaryView.updateHeader(notificationCount)
        mSecondaryView.updateHeader(notificationCount - 1)
    }

    fun getPrimaryView(): NotificationMainView = mPrimaryView
    fun getSecondaryView(): NotificationMainView = mSecondaryView
    fun getNotificationInfos(): List<NotificationInfo> = ArrayList(mNotificationInfos)
}
