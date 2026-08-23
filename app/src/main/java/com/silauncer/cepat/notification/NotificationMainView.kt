package com.silauncer.cepat.notification

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.LinearLayout
import android.widget.TextView
import com.silauncer.cepat.R
import com.silauncer.cepat.anim.Interpolators

/**
 * NotificationMainView
 *
 * // [Jalur Class]: com.silauncer.cepat.notification.NotificationMainView
 * // [Penjelasan]: Tampilan kartu baris notifikasi individual yang menampilkan ikon, header hitungan, judul, dan isi teks, serta mendukung animasi pergeseran usap (drag progress) dan pewarnaan dinamis (adaptasi AOSP Launcher3 NotificationMainView).
 */
class NotificationMainView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    companion object {
        private const val PRIMARY_GONE_PROGRESS = 0.7f
        private const val PRIMARY_MIN_PROGRESS = 0.40f
        private const val PRIMARY_MAX_PROGRESS = 0.60f
        private const val SECONDARY_MIN_PROGRESS = 0.30f
        private const val SECONDARY_MAX_PROGRESS = 0.50f
        private const val SECONDARY_CONTENT_MAX_PROGRESS = 0.6f
    }

    private var mNotificationInfo: NotificationInfo? = null
    private var mBackgroundColor: Int = Color.parseColor("#2C2C2E")
    private var mTitleView: TextView? = null
    private var mTextView: TextView? = null
    private var mIconView: View? = null

    private var mHeader: View? = null
    private var mMainView: View? = null
    private var mHeaderCount: TextView? = null

    private val mOutline = Rect()
    private val mNotificationSpace: Int
    private val mMaxTransX: Int
    private val mMaxElevation: Int
    private val mBackground: GradientDrawable

    init {
        val outlineRadius = resources.getDimension(R.dimen.dialog_corner_radius)

        mBackground = GradientDrawable().apply {
            setColor(mBackgroundColor)
            cornerRadius = outlineRadius
        }
        background = mBackground

        mMaxElevation = resources.getDimensionPixelSize(R.dimen.deep_shortcuts_elevation)
        elevation = mMaxElevation.toFloat()

        mMaxTransX = resources.getDimensionPixelSize(R.dimen.notification_max_trans)
        mNotificationSpace = resources.getDimensionPixelSize(R.dimen.notification_space)

        clipToOutline = true
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(mOutline, outlineRadius)
            }
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()

        val textAndBackground = findViewById<ViewGroup>(R.id.text_and_background)
        mTitleView = textAndBackground?.findViewById(R.id.title)
        mTextView = textAndBackground?.findViewById(R.id.text)
        mIconView = findViewById(R.id.popup_item_icon)
        mHeaderCount = findViewById(R.id.notification_count)

        mHeader = findViewById(R.id.header)
        mMainView = findViewById(R.id.main_view)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        mOutline.set(0, 0, width, height)
        invalidateOutline()
    }

    fun updateHeader(notificationCount: Int) {
        val countView = mHeaderCount ?: return
        if (notificationCount <= 1) {
            countView.text = ""
            countView.visibility = View.INVISIBLE
        } else {
            countView.text = notificationCount.toString()
            countView.visibility = View.VISIBLE
        }
    }

    fun updateBackgroundColor(color: Int) {
        mBackgroundColor = color
        mBackground.setColor(color)
        mNotificationInfo?.let { info ->
            mIconView?.background = info.getIconForBackground(context, mBackgroundColor)
        }
    }

    fun updateBackgroundColor(color: Int, animatorSetOut: AnimatorSet) {
        val oldColor = mBackgroundColor
        val colors = ValueAnimator.ofArgb(oldColor, color).apply {
            addUpdateListener { va ->
                val newColor = va.animatedValue as Int
                updateBackgroundColor(newColor)
            }
        }
        animatorSetOut.play(colors)
    }

    fun applyNotificationInfo(notificationInfo: NotificationInfo?) {
        mNotificationInfo = notificationInfo
        val info = notificationInfo ?: return

        NotificationListener.getInstanceIfConnected()?.setNotificationsShown(arrayOf(info.notificationKey))

        val title = info.title
        val text = info.text

        if (!TextUtils.isEmpty(title) && !TextUtils.isEmpty(text)) {
            mTitleView?.text = title
            mTextView?.text = text
            mTextView?.visibility = View.VISIBLE
        } else {
            mTitleView?.maxLines = 2
            mTitleView?.text = if (TextUtils.isEmpty(title)) text else title
            mTextView?.visibility = View.GONE
        }

        mIconView?.background = info.getIconForBackground(context, mBackgroundColor)
        if (info.intent != null) {
            setOnClickListener(info)
        }
    }

    fun setContentAlpha(alpha: Float) {
        mHeader?.alpha = alpha
        mMainView?.alpha = alpha
    }

    fun setContentTranslationX(transX: Float) {
        mHeader?.translationX = transX
        mMainView?.translationX = transX
    }

    fun onPrimaryDrag(progress: Float) {
        val absProgress = Math.abs(progress)
        val w = width.toFloat()

        val min = PRIMARY_MIN_PROGRESS
        val max = PRIMARY_MAX_PROGRESS

        if (absProgress < min) {
            alpha = 1f
            setContentAlpha(1f)
            elevation = mMaxElevation.toFloat()
        } else if (absProgress < max) {
            alpha = 1f
            setContentAlpha(Interpolators.mapToRange(absProgress, min, max, 1f, 0f, Interpolators.LINEAR))
            elevation = Interpolators.mapToRange(absProgress, min, max, mMaxElevation.toFloat(), 0f, Interpolators.LINEAR)
        } else {
            alpha = Interpolators.mapToRange(absProgress, max, PRIMARY_GONE_PROGRESS, 1f, 0f, Interpolators.LINEAR)
            setContentAlpha(0f)
            elevation = 0f
        }

        translationX = w * progress
    }

    fun onSecondaryDrag(progress: Float) {
        val absProgress = Math.abs(progress)

        val min = SECONDARY_MIN_PROGRESS
        val max = SECONDARY_MAX_PROGRESS
        val contentMax = SECONDARY_CONTENT_MAX_PROGRESS

        if (absProgress < min) {
            alpha = 0f
            setContentAlpha(0f)
            elevation = 0f
        } else if (absProgress < max) {
            alpha = Interpolators.mapToRange(absProgress, min, max, 0f, 1f, Interpolators.LINEAR)
            setContentAlpha(0f)
            elevation = 0f
        } else {
            alpha = 1f
            setContentAlpha(
                if (absProgress > contentMax) 1f
                else Interpolators.mapToRange(absProgress, max, contentMax, 0f, 1f, Interpolators.LINEAR)
            )
            elevation = Interpolators.mapToRange(absProgress, max, 1f, 0f, mMaxElevation.toFloat(), Interpolators.LINEAR)
        }

        val w = width
        val crop = (w * absProgress).toInt()
        val space = (if (absProgress > PRIMARY_GONE_PROGRESS) {
            Interpolators.mapToRange(absProgress, PRIMARY_GONE_PROGRESS, 1f, mNotificationSpace.toFloat(), 0f, Interpolators.LINEAR)
        } else {
            mNotificationSpace.toFloat()
        }).toInt()

        if (progress < 0) {
            mOutline.left = Math.max(0, width - crop + space)
            mOutline.right = width
        } else {
            mOutline.right = Math.min(width, crop - space)
            mOutline.left = 0
        }

        val contentTransX = mMaxTransX * (1f - absProgress)
        setContentTranslationX(if (progress < 0) contentTransX else -contentTransX)
        invalidateOutline()
    }

    fun getNotificationInfo(): NotificationInfo? = mNotificationInfo

    fun canChildBeDismissed(): Boolean {
        return mNotificationInfo?.dismissable == true
    }

    fun onChildDismissed() {
        val info = mNotificationInfo ?: return
        NotificationListener.getInstanceIfConnected()?.cancelNotificationFromLauncher(info.notificationKey)
    }
}
