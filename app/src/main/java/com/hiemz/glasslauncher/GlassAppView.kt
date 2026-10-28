package com.hiemz.glasslauncher

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.text.TextPaint
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

/**
 * One app icon on a frosted-glass tile: the tile background is a TRUE BLUR
 * of the wallpaper region behind the tile, plus a translucent white overlay
 * and a thin light border. The app icon sits centered on the tile, the label
 * below it.
 */
class GlassAppView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var app: AppInfo? = null
    private var tileBitmap: Bitmap? = null
    private var lastRect: Rect? = null

    private val density = context.resources.displayMetrics.density
    private val scaledDensity = context.resources.displayMetrics.scaledDensity
    private val cornerRadius = 20f * density

    private val tilePaint = Paint().apply { isFilterBitmap = true }
    private val overlayPaint = Paint().apply {
        color = Color.argb(64, 255, 255, 255)
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f * density
        color = Color.argb(150, 255, 255, 255)
    }
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 12f * scaledDensity
        textAlign = Paint.Align.CENTER
        setShadowLayer(4f, 0f, 2f, Color.BLACK)
    }

    private val tileRect = RectF()
    private val clipPath = Path()

    fun setApp(app: AppInfo) {
        this.app = app
        // Recycled views may sit elsewhere on screen: force a blur recompute.
        lastRect = null
        invalidate()
    }

    private fun dp(v: Float): Float = v * density

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val app = app ?: return

        val tileSize = min(width.toFloat(), dp(76f))
        val left = (width - tileSize) / 2f
        val top = dp(6f)
        tileRect.set(left, top, left + tileSize, top + tileSize)

        // Wallpaper region behind this tile, in screen coordinates.
        val loc = IntArray(2)
        getLocationOnScreen(loc)
        val screenRect = Rect(
            (loc[0] + left).toInt(),
            (loc[1] + top).toInt(),
            (loc[0] + left + tileSize).toInt(),
            (loc[1] + top + tileSize).toInt()
        )
        if (lastRect != screenRect) {
            tileBitmap = WallpaperBlurProvider.blurredTile(screenRect)
            lastRect = screenRect
        }

        val bmp = tileBitmap
        if (bmp != null) {
            clipPath.reset()
            clipPath.addRoundRect(tileRect, cornerRadius, cornerRadius, Path.Direction.CW)
            canvas.save()
            canvas.clipPath(clipPath)
            canvas.drawBitmap(bmp, Rect(0, 0, bmp.width, bmp.height), tileRect, tilePaint)
            canvas.drawRect(tileRect, overlayPaint)
            canvas.restore()
        } else {
            canvas.drawRoundRect(tileRect, cornerRadius, cornerRadius, overlayPaint)
        }
        canvas.drawRoundRect(tileRect, cornerRadius, cornerRadius, borderPaint)

        // App icon centered on the tile.
        val iconSize = dp(48f)
        val iconLeft = (left + (tileSize - iconSize) / 2f).toInt()
        val iconTop = (top + (tileSize - iconSize) / 2f).toInt()
        app.icon.setBounds(
            iconLeft, iconTop,
            (iconLeft + iconSize).toInt(), (iconTop + iconSize).toInt()
        )
        app.icon.draw(canvas)

        // Label below the tile, single line, ellipsized.
        val label = TextUtils.ellipsize(
            app.label, textPaint, width.toFloat(), TextUtils.TruncateAt.END
        )
        val baseline = top + tileSize + dp(6f) - textPaint.fontMetrics.ascent
        canvas.drawText(label.toString(), width / 2f, baseline, textPaint)
    }
}
