package org.cis_india.wsreader.reader

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout
import kotlin.math.abs

class HorizontalSwipeDisableLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    var disableHorizontalSwipe: Boolean = false
    private var startX: Float = 0f
    private var startY: Float = 0f

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (!disableHorizontalSwipe) return super.onInterceptTouchEvent(ev)

        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startX = ev.x
                startY = ev.y
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = abs(ev.x - startX)
                val dy = abs(ev.y - startY)
                // If the user scrolls horizontally more than vertically, intercept!
                if (dx > dy && dx > 20) {
                    return true 
                }
            }
        }
        return super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (disableHorizontalSwipe) {
            // We intercepted it, now we just consume all subsequent moves for this gesture
            return true
        }
        return super.onTouchEvent(event)
    }
}
