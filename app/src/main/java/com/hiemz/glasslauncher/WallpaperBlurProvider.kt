package com.hiemz.glasslauncher

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.util.LruCache

/**
 * Holds a screen-sized copy of the current wallpaper and serves TRUE BLUR
 * tiles: for a tile's on-screen rect, the wallpaper region behind that rect
 * is cropped, downscaled, box-blurred and cached. This is real blur of the
 * wallpaper behind the tile — not just a translucent overlay.
 */
object WallpaperBlurProvider {

    private var wallpaper: Bitmap? = null

    // 4 MB of cached blurred tiles (sizeOf is in KB).
    private val cache = object : LruCache<String, Bitmap>(4 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }

    fun prepare(context: Context) {
        if (wallpaper != null) return
        val metrics = context.resources.displayMetrics
        val sw = metrics.widthPixels
        val sh = metrics.heightPixels
        val drawable = try {
            WallpaperManager.getInstance(context).drawable
        } catch (e: Exception) {
            null
        }
        val src: Bitmap? = when (drawable) {
            is BitmapDrawable -> drawable.bitmap
            null -> null
            else -> {
                val w = drawable.intrinsicWidth.takeIf { it > 0 } ?: sw
                val h = drawable.intrinsicHeight.takeIf { it > 0 } ?: sh
                Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also { bmp ->
                    val c = Canvas(bmp)
                    drawable.setBounds(0, 0, w, h)
                    drawable.draw(c)
                }
            }
        }
        wallpaper = if (src != null) centerCropToScreen(src, sw, sh) else fallbackGradient(sw, sh)
    }

    private fun centerCropToScreen(src: Bitmap, sw: Int, sh: Int): Bitmap {
        val srcAspect = src.width.toFloat() / src.height
        val dstAspect = sw.toFloat() / sh
        val crop = if (srcAspect > dstAspect) {
            val cw = (src.height * dstAspect).toInt()
            Rect((src.width - cw) / 2, 0, (src.width - cw) / 2 + cw, src.height)
        } else {
            val ch = (src.width / dstAspect).toInt()
            Rect(0, (src.height - ch) / 2, src.width, (src.height - ch) / 2 + ch)
        }
        val cropped = Bitmap.createBitmap(src, crop.left, crop.top, crop.width(), crop.height())
        return Bitmap.createScaledBitmap(cropped, sw, sh, true)
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

    /** Blurred bitmap of the wallpaper region behind [rect] (screen coordinates). */
    fun blurredTile(rect: Rect): Bitmap? {
        val wp = wallpaper ?: return null
        val clamped = Rect(
            rect.left.coerceIn(0, wp.width - 1),
            rect.top.coerceIn(0, wp.height - 1),
            rect.right.coerceIn(1, wp.width),
            rect.bottom.coerceIn(1, wp.height)
        )
        if (clamped.width() <= 0 || clamped.height() <= 0) return null
        val key = "${clamped.left},${clamped.top},${clamped.right},${clamped.bottom}"
        cache.get(key)?.let { return it }
        val crop = Bitmap.createBitmap(
            wp, clamped.left, clamped.top, clamped.width(), clamped.height()
        )
        val smallW = maxOf(1, crop.width / 4)
        val smallH = maxOf(1, crop.height / 4)
        val small = Bitmap.createScaledBitmap(crop, smallW, smallH, true)
        val blurred = Blur.blur(small, radius = 6, iterations = 3)
        cache.put(key, blurred)
        return blurred
    }

    fun clearCache() {
        cache.evictAll()
    }
}
