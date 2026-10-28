package com.hiemz.glasslauncher

import android.graphics.Bitmap

/**
 * Separable sliding-window box blur (pure Kotlin, no RenderScript).
 * Three iterations of horizontal+vertical passes approximate a Gaussian blur
 * and are fast on the small downscaled tiles used for the glass effect.
 */
object Blur {

    fun blur(src: Bitmap, radius: Int, iterations: Int = 3): Bitmap {
        val w = src.width
        val h = src.height
        var current = IntArray(w * h)
        src.getPixels(current, 0, w, 0, 0, w, h)
        var next = IntArray(w * h)
        repeat(iterations) {
            boxBlurHorizontal(current, next, w, h, radius)
            boxBlurVertical(next, current, w, h, radius)
        }
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        out.setPixels(current, 0, w, 0, 0, w, h)
        return out
    }

    private fun boxBlurHorizontal(src: IntArray, dst: IntArray, w: Int, h: Int, r: Int) {
        val div = 2 * r + 1
        for (y in 0 until h) {
            val row = y * w
            var a = 0
            var rr = 0
            var g = 0
            var b = 0
            for (i in -r..r) {
                val c = src[row + i.coerceIn(0, w - 1)]
                a += (c ushr 24) and 0xff
                rr += (c ushr 16) and 0xff
                g += (c ushr 8) and 0xff
                b += c and 0xff
            }
            for (x in 0 until w) {
                dst[row + x] =
                    (a / div shl 24) or (rr / div shl 16) or (g / div shl 8) or (b / div)
                val cOut = src[row + (x - r).coerceIn(0, w - 1)]
                val cIn = src[row + (x + r + 1).coerceIn(0, w - 1)]
                a += ((cIn ushr 24) and 0xff) - ((cOut ushr 24) and 0xff)
                rr += ((cIn ushr 16) and 0xff) - ((cOut ushr 16) and 0xff)
                g += ((cIn ushr 8) and 0xff) - ((cOut ushr 8) and 0xff)
                b += (cIn and 0xff) - (cOut and 0xff)
            }
        }
    }

    private fun boxBlurVertical(src: IntArray, dst: IntArray, w: Int, h: Int, r: Int) {
        val div = 2 * r + 1
        for (x in 0 until w) {
            var a = 0
            var rr = 0
            var g = 0
            var b = 0
            for (i in -r..r) {
                val c = src[i.coerceIn(0, h - 1) * w + x]
                a += (c ushr 24) and 0xff
                rr += (c ushr 16) and 0xff
                g += (c ushr 8) and 0xff
                b += c and 0xff
            }
            for (y in 0 until h) {
                dst[y * w + x] =
                    (a / div shl 24) or (rr / div shl 16) or (g / div shl 8) or (b / div)
                val cOut = src[(y - r).coerceIn(0, h - 1) * w + x]
                val cIn = src[(y + r + 1).coerceIn(0, h - 1) * w + x]
                a += ((cIn ushr 24) and 0xff) - ((cOut ushr 24) and 0xff)
                rr += ((cIn ushr 16) and 0xff) - ((cOut ushr 16) and 0xff)
                g += ((cIn ushr 8) and 0xff) - ((cOut ushr 8) and 0xff)
                b += (cIn and 0xff) - (cOut and 0xff)
            }
        }
    }
}
