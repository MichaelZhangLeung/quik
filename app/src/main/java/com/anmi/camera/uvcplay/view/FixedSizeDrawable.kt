package com.anmi.camera.uvcplay.view

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.DrawableCompat

/**
 * 一个 Wrapper Drawable：
 *  - 固定 intrinsic size 为指定像素
 *  - 转发/保留内部 Drawable 的 state (isStateful / setState / onStateChange)
 *  - 取消 tint（可按需修改）
 */
class FixedSizeDrawable(
    private val innerOrig: Drawable,
    private val widthPx: Int,
    private val heightPx: Int
) : Drawable() {

    private val inner: Drawable = DrawableCompat.wrap(innerOrig).mutate()

    init {
        // 取消任何外部 tint（如果你希望保留 tint 可去掉）
        DrawableCompat.setTintList(inner, null)
        // 保证 callback 可接收
        inner.callback = object : Drawable.Callback {
            override fun invalidateDrawable(who: Drawable) { this@FixedSizeDrawable.invalidateSelf() }
            override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) { scheduleSelf(what, `when`) }
            override fun unscheduleDrawable(who: Drawable, what: Runnable) { unscheduleSelf(what) }
        }
    }

    override fun draw(canvas: Canvas) {
        // 确保 inner 的 bounds 与外层一致
        inner.bounds = bounds
        inner.draw(canvas)
    }

    override fun setAlpha(alpha: Int) {
        inner.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        inner.colorFilter = colorFilter
        invalidateSelf()
    }

    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    override fun getIntrinsicWidth(): Int = widthPx

    override fun getIntrinsicHeight(): Int = heightPx

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        inner.bounds = bounds
    }

    // 状态转发：保留内部 drawable 的 statefulness
    override fun isStateful(): Boolean = inner.isStateful

    override fun setState(stateSet: IntArray): Boolean {
        val changedInner = inner.setState(stateSet)
        val changedThis = super.setState(stateSet)
        // 如果 inner 的状态变化会触发重绘
        if (changedInner || changedThis) {
            invalidateSelf()
            return true
        }
        return false
    }

    override fun onStateChange(state: IntArray): Boolean {
        val changed = inner.setState(state)
        if (changed) {
            invalidateSelf()
            return true
        }
        return false
    }

    // 转发 autoMirrored / layoutDirection
    override fun setAutoMirrored(mirrored: Boolean) {
        inner.isAutoMirrored = mirrored
    }

    override fun isAutoMirrored(): Boolean = inner.isAutoMirrored

    override fun setVisible(visible: Boolean, restart: Boolean): Boolean {
        inner.setVisible(visible, restart)
        return super.setVisible(visible, restart)
    }
}

