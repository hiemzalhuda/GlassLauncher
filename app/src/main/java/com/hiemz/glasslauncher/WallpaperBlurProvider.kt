package com.hiemz.glasslauncher

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable

/**
 * Single source of truth for the wallpaper.
 *
 * [wallpaperBitmap] is the system wallpaper rendered ONCE into a screen-sized
 * bitmap with center-crop-to-fill scaling — exactly the scaling the launcher
 * background uses (the background ImageView shows this very bitmap, so no
 * crop/zoom mismatch is possible). [blurredQuarter] is that same bitmap
 * downscaled to 1/4 and box-blurred once. Every glass squircle crops its
 * region live from [blurredQuarter] using its current screen position, so
 * the blur always stays glued to the wallpaper — including mid-scroll in the
 * drawer. No per-tile blur, no scroll-idle-only refresh, no stale crops.
 */
object WallpaperBlurProvider {

    /** Screen-sized wallpaper bitmap, also used as the background image. */
    var wallpaperBitmap: Bitmap? = null
        private set

    /** The same wallpaper, downscaled to 1/4 and blurred. */
    var blurredQuarter: Bitmap? = null
        private set

    var screenWidth = 0
        private set
    var screenHeight = 0
        private set

    private var preparedForW = 0
    private var preparedForH = 0

    fun prepare(context: Context) {
        val metrics = context.resources.displayMetrics
        val sw = metrics.widthPixels
        val sh = metrics.heightPixels
        if (wallpaperBitmap != null && preparedForW == sw && preparedForH == sh) return

        val drawable = try {
            WallpaperManager.getInstance(context).drawable
        } catch (e: Exception) {
            null
        }
        val bmp = if (drawable != null) {
            val srcW = (drawable as? BitmapDrawable)?.bitmap?.width
                ?: drawable.intrinsicWidth.takeIf { it > 0 } ?: sw
            val srcH = (drawable as? BitmapDrawable)?.bitmap?.height
                ?: drawable.intrinsicHeight.takeIf { it > 0 } ?: sh
            // Center-crop-to-fill: the same scaling an ImageView with
            // CENTER_CROP applies to this drawable at screen size.
            Bitmap.createBitmap(sw, sh, Bitmap.Config.ARGB_8888).also { out ->
                val scale = maxOf(sw / srcW.toFloat(), sh / srcH.toFloat())
                val dx = (sw - srcW * scale) / 2f
                val dy = (sh - srcH * scale) / 2f
                val canvas = Canvas(out)
                canvas.save()
                canvas.translate(dx, dy)
                canvas.scale(scale, scale)
                drawable.setBounds(0, 0, srcW, srcH)
                drawable.draw(canvas)
                canvas.restore()
            }
        } else {
            fallbackGradient(sw, sh)
        }

        wallpaperBitmap?.takeIf { it !== bmp }?.recycle()
        blurredQuarter?.recycle()
        wallpaperBitmap = bmp
        screenWidth = sw
        screenHeight = sh
        val qw = maxOf(1, sw / 4)
        val qh = maxOf(1, sh / 4)
        val small = Bitmap.createScaledBitmap(bmp, qw, qh, true)
        blurredQuarter = Blur.blur(small, radius = 6, iterations = 3)
        if (small !== blurredQuarter) small.recycle()
        preparedForW = sw
        preparedForH = sh
    }

    private fun fallbackGradient(sw: Int, sh: Int): Bitmap {
        val bmp = Bitmap.createBitmap(sw, sh, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint()
        paint.shader = LinearGradient(
            0f, 0f, 0f, sh.toFloat(),
            intArrayOf(Color.rgb(27, 27, 47), Color.rgb(58, 48, 94)),
            null, Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, sw.toFloat(), sh.toFloat(), paint)
        return bmp
    }

    /** Drop cached bitmaps (e.g. after the system wallpaper changes). */
    fun invalidate() {
        wallpaperBitmap?.recycle()
        blurredQuarter?.recycle()
        wallpaperBitmap = null
        blurredQuarter = null
        preparedForW = 0
        preparedForH = 0
    }
}
