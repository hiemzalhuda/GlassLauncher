package com.hiemz.glasslauncher

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.text.TextPaint
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View

/**
 * One app icon as a liquid-glass squircle — NOT a tile. The glass IS the
 * icon's own adaptive shape at normal icon size, painted in this order
 * (mirroring PixelLauncherEnhanced's icon glass):
 *
 *   drop shadow -> blurred wallpaper through the squircle -> light tint
 *   -> rim light (top edge) -> specular highlight (top-left) -> bottom
 *   shade (depth) -> thin white outline -> full-color app glyph -> label
 *
 * The blurred wallpaper is cropped live from [WallpaperBlurProvider]'s
 * pre-blurred screen bitmap using this view's CURRENT screen position, so
 * the glass never goes stale mid-scroll: it stays fixed to the wallpaper.
 */
class GlassAppView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var app: AppInfo? = null

    private val density = context.resources.displayMetrics.density
    private val scaledDensity = context.resources.displayMetrics.scaledDensity

    // Squircle at normal icon size, ~30% corner radius (adaptive-icon look).
    private val squircleSize = 64f * density
    private val cornerRadius = squircleSize * 0.30f
    private val iconSize = 54f * density

    private val bitmapPaint = Paint().apply { isFilterBitmap = true }

    // Light tint so the wallpaper dominates — NOT the old flat 25% white.
    private val tintPaint = Paint().apply { color = Color.argb(46, 255, 255, 255) }

    // Thin white outline, ~15% opacity, ~1dp (PLE GlassStyle default).
    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f * density
        color = Color.argb(38, 255, 255, 255)
    }

    // Soft drop shadow under the squircle (opaque fill is covered by the
    // glass layers drawn after it; only the shadow bleeding outside remains).
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        setShadowLayer(6f * density, 0f, 2f * density, Color.argb(90, 0, 0, 0))
    }

    private val rimPaint = Paint()
    private val specularPaint = Paint()
    private val shadePaint = Paint()

    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 12f * scaledDensity
        textAlign = Paint.Align.CENTER
        setShadowLayer(4f, 0f, 2f, Color.BLACK)
    }

    private val squircleRect = RectF()
    private val clipPath = Path()
    private val outlinePath = Path()
    private val outlineRect = RectF()

    init {
        // setShadowLayer renders reliably on a software layer.
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    fun setApp(app: AppInfo) {
        this.app = app
        invalidate()
    }

    private fun dp(v: Float): Float = v * density

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val left = (w - squircleSize) / 2f
        val top = dp(4f)
        squircleRect.set(left, top, left + squircleSize, top + squircleSize)

        clipPath.reset()
        val radii = FloatArray(8) { cornerRadius }
        clipPath.addRoundRect(squircleRect, radii, Path.Direction.CW)

        // Outline inset by half its stroke so it sits inside the shape.
        val inset = outlinePaint.strokeWidth / 2f
        outlineRect.set(squircleRect)
        outlineRect.inset(inset, inset)
        val outlineRadii = FloatArray(8) { (cornerRadius - inset).coerceAtLeast(0f) }
        outlinePath.reset()
        outlinePath.addRoundRect(outlineRect, outlineRadii, Path.Direction.CW)

        // Rim light: bright gradient along the top edge, fading by ~45%.
        rimPaint.shader = LinearGradient(
            0f, top, 0f, top + squircleSize * 0.45f,
            intArrayOf(Color.argb(90, 255, 255, 255), Color.argb(0, 255, 255, 255)),
            null, Shader.TileMode.CLAMP
        )
        // Specular highlight: soft white radial at the top-left.
        specularPaint.shader = RadialGradient(
            left + squircleSize * 0.30f, top + squircleSize * 0.22f,
            squircleSize * 0.45f,
            intArrayOf(Color.argb(80, 255, 255, 255), Color.argb(0, 255, 255, 255)),
            null, Shader.TileMode.CLAMP
        )
        // Bottom shade: subtle depth darkening over the bottom ~30%.
        shadePaint.shader = LinearGradient(
            0f, top + squircleSize * 0.70f, 0f, top + squircleSize,
            intArrayOf(Color.argb(0, 0, 0, 0), Color.argb(30, 0, 0, 0)),
            null, Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val app = app ?: return

        // 1. Drop shadow (fill is covered by the glass layers below).
        canvas.drawPath(clipPath, shadowPaint)

        // 2-6. Glass, clipped to the squircle.
        val blurred = WallpaperBlurProvider.blurredQuarter
            ?: WallpaperBlurProvider.prepare(context).let { WallpaperBlurProvider.blurredQuarter }
        val sw = WallpaperBlurProvider.screenWidth
        val sh = WallpaperBlurProvider.screenHeight
        if (blurred != null && sw > 0 && sh > 0) {
            val loc = IntArray(2)
            getLocationOnScreen(loc)
            val fullLeft = loc[0] + squircleRect.left
            val fullTop = loc[1] + squircleRect.top
            val fullRight = fullLeft + squircleRect.width()
            val fullBottom = fullTop + squircleRect.height()
            // Live crop: intersect with the screen, map the visible part
            // onto the quarter-resolution blurred bitmap.
            val visLeft = fullLeft.coerceIn(0f, sw.toFloat())
            val visTop = fullTop.coerceIn(0f, sh.toFloat())
            val visRight = fullRight.coerceIn(0f, sw.toFloat())
            val visBottom = fullBottom.coerceIn(0f, sh.toFloat())
            if (visRight > visLeft && visBottom > visTop) {
                val qw = blurred.width
                val qh = blurred.height
                val src = Rect(
                    (visLeft * qw / sw).toInt(),
                    (visTop * qh / sh).toInt(),
                    (visRight * qw / sw).toInt().coerceAtMost(qw),
                    (visBottom * qh / sh).toInt().coerceAtMost(qh)
                )
                val w = squircleRect.width()
                val h = squircleRect.height()
                val dst = RectF(
                    squircleRect.left + (visLeft - fullLeft) / w * w,
                    squircleRect.top + (visTop - fullTop) / h * h,
                    squircleRect.left + (visRight - fullLeft) / w * w,
                    squircleRect.top + (visBottom - fullTop) / h * h
                )
                canvas.save()
                canvas.clipPath(clipPath)
                canvas.drawBitmap(blurred, src, dst, bitmapPaint)
                canvas.drawRect(squircleRect, tintPaint)
                canvas.drawRect(squircleRect, rimPaint)
                canvas.drawRect(squircleRect, specularPaint)
                canvas.drawRect(squircleRect, shadePaint)
                canvas.restore()
            }
        } else {
            // No blur source: translucent fallback so the icon still has a shape.
            canvas.drawPath(clipPath, tintPaint)
        }

        // 7. Thin white outline.
        canvas.drawPath(outlinePath, outlinePaint)

        // 8. Full-color app glyph, centered on the glass squircle.
        val iconLeft = (squircleRect.left + (squircleRect.width() - iconSize) / 2f).toInt()
        val iconTop = (squircleRect.top + (squircleRect.height() - iconSize) / 2f).toInt()
        app.icon.setBounds(
            iconLeft, iconTop,
            (iconLeft + iconSize).toInt(), (iconTop + iconSize).toInt()
        )
        app.icon.draw(canvas)

        // 9. Label below the squircle, single line, ellipsized.
        val label = TextUtils.ellipsize(
            app.label, textPaint, width.toFloat(), TextUtils.TruncateAt.END
        )
        val baseline = squircleRect.bottom + dp(6f) - textPaint.fontMetrics.ascent
        canvas.drawText(label.toString(), width / 2f, baseline, textPaint)
    }
}
