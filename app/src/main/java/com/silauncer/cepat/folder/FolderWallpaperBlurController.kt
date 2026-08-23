package com.silauncer.cepat.folder

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.View
import android.widget.ImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.isActive

/**
 * FolderWallpaperBlurController
 *
 * [Jalur Class]: com.silauncer.cepat.folder.FolderWallpaperBlurController
 * [Penjelasan]: Mengambil wallpaper dinamis yang sedang aktif dari WallpaperManager,
 * menerapkan efek blur (menggunakan RenderEffect pada API 31+ atau fast stack blur pada API lebih lama),
 * serta penyesuaian kecerahan (dimmed overlay 30-40%) untuk modal folder.
 */
class FolderWallpaperBlurController(private val context: Context) {
    // [Jalur Class]: com.silauncer.cepat.folder.FolderWallpaperBlurController
    // [Penjelasan]: Membuat instance CoroutineScope level class dengan SupervisorJob untuk menghentikan instansiasi scope ganda yang menjadi root cause memory leak.
    private var controllerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var activeJob: Job? = null
    private var cachedBlurredBitmap: Bitmap? = null

    /**
     * Menerapkan wallpaper aktif dengan efek blur dan dimming ke view yang disediakan.
     */
    fun applyWallpaperBackground(
        imageView: ImageView,
        dimOverlay: View
    ) {
        // [Jalur Class]: com.silauncer.cepat.folder.FolderWallpaperBlurController
        // [Penjelasan]: Memastikan scope aktif (karena dapat di-reuse setelah clear() membatalkan scope sebelumnya).
        if (!controllerScope.isActive) {
            controllerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        }

        // [Jalur Class]: com.silauncer.cepat.folder.FolderWallpaperBlurController
        // [Penjelasan]: Setel lapisan dimmed scrim transparan tipis 10% hitam (0x1A000000) agar wallpaper asli perangkat terekspos sempurna dan jernih
        dimOverlay.visibility = View.VISIBLE
        dimOverlay.setBackgroundColor(0x1A000000)
        dimOverlay.alpha = 1.0f

        activeJob?.cancel()
        activeJob = controllerScope.launch {
            val wallpaperBitmap = withContext(Dispatchers.IO) {
                loadAndProcessWallpaper()
            }

            if (wallpaperBitmap != null) {
                imageView.setImageBitmap(wallpaperBitmap)
                imageView.visibility = View.VISIBLE
                
                // [Jalur Class]: com.silauncer.cepat.folder.FolderWallpaperBlurController
                // [Penjelasan]: Menerapkan hardware-accelerated RenderEffect blur halus pada Android 12+ (API 31+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    try {
                        val blurEffect = RenderEffect.createBlurEffect(
                            18f,
                            18f,
                            Shader.TileMode.CLAMP
                        )
                        imageView.setRenderEffect(blurEffect)
                    } catch (e: Throwable) {
                        // Fallback stack blur jika RenderEffect gagal
                        withContext(Dispatchers.IO) {
                            try {
                                val blurred = fastStackBlur(wallpaperBitmap, 10)
                                withContext(Dispatchers.Main) {
                                    imageView.setImageBitmap(blurred)
                                }
                            } catch (t: Throwable) {}
                        }
                    }
                }
            } else {
                // [Jalur Class]: com.silauncer.cepat.folder.FolderWallpaperBlurController
                // [Penjelasan]: Jika bitmap tidak dapat diakses langsung, sembunyikan imageView dan biarkan dimmed overlay transparan tipis 10% hitam sehingga wallpaper sistem tembus pandang secara jernih
                imageView.visibility = View.GONE
                dimOverlay.setBackgroundColor(0x1A000000)
            }
        }
    }

    /**
     * Membersihkan resource bitmap dan efek render saat folder ditutup.
     */
    fun clear() {
        // [Jalur Class]: com.silauncer.cepat.folder.FolderWallpaperBlurController
        // [Penjelasan]: Membatalkan scope level class beserta seluruh children jobs di dalamnya untuk mencegah memory leak dan callback setelah controller tidak digunakan.
        controllerScope.cancel()
        activeJob = null
        cachedBlurredBitmap?.let {
            if (!it.isRecycled) {
                // Biarkan garbage collector membersihkan jika masih ada referensi aktif
            }
        }
    }

    /**
     * Mengambil wallpaper dari WallpaperManager dan memproses bitmap.
     */
    private fun loadAndProcessWallpaper(): Bitmap? {
        return try {
            val wallpaperManager = WallpaperManager.getInstance(context)
            val drawable: Drawable? = try {
                wallpaperManager.drawable ?: wallpaperManager.peekDrawable() ?: wallpaperManager.fastDrawable
            } catch (e: Throwable) {
                try {
                    wallpaperManager.peekDrawable()
                } catch (e2: Throwable) {
                    null
                }
            }

            if (drawable == null) return null

            val originalBitmap = drawableToBitmap(drawable) ?: return null

            // [Jalur Class]: com.silauncer.cepat.folder.FolderWallpaperBlurController
            // [Penjelasan]: Pada API < 31, terapkan algoritma FastStackBlur pada bitmap beresolusi terukur
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                val scaled = Bitmap.createScaledBitmap(
                    originalBitmap,
                    (originalBitmap.width / 4).coerceAtLeast(100),
                    (originalBitmap.height / 4).coerceAtLeast(100),
                    true
                )
                val blurred = fastStackBlur(scaled, 15)
                cachedBlurredBitmap = blurred
                blurred
            } else {
                // Pada API 31+, RenderEffect menangani blur secara langsung di GPU
                val scaled = Bitmap.createScaledBitmap(
                    originalBitmap,
                    (originalBitmap.width / 2).coerceAtLeast(200),
                    (originalBitmap.height / 2).coerceAtLeast(200),
                    true
                )
                cachedBlurredBitmap = scaled
                scaled
            }
        } catch (e: Throwable) {
            null
        }
    }

    /**
     * Mengonversi Drawable menjadi Bitmap.
     */
    private fun drawableToBitmap(drawable: Drawable): Bitmap? {
        if (drawable is BitmapDrawable && drawable.bitmap != null && !drawable.bitmap.isRecycled) {
            return drawable.bitmap
        }

        val displayMetrics = context.resources.displayMetrics
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else displayMetrics.widthPixels.coerceAtLeast(720)
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else displayMetrics.heightPixels.coerceAtLeast(1280)

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    /**
     * Algoritma Fast Stack Blur in-memory murni untuk perangkat tanpa RenderEffect API 31+.
     */
    internal fun fastStackBlur(sentBitmap: Bitmap, radius: Int): Bitmap {
        val bitmap = sentBitmap.copy(sentBitmap.config ?: Bitmap.Config.ARGB_8888, true)
        if (radius < 1) return bitmap

        val w = bitmap.width
        val h = bitmap.height
        val pix = IntArray(w * h)
        bitmap.getPixels(pix, 0, w, 0, 0, w, h)

        val wm = w - 1
        val hm = h - 1
        val wh = w * h
        val div = radius + radius + 1

        val r = IntArray(wh)
        val g = IntArray(wh)
        val b = IntArray(wh)
        var rsum: Int
        var gsum: Int
        var bsum: Int
        var x: Int
        var y: Int
        var i: Int
        var p: Int
        var yp: Int
        var yi: Int
        var yw: Int
        val vmin = IntArray(Math.max(w, h))

        var divsum = (div + 1) shr 1
        divsum *= divsum
        val dv = IntArray(256 * divsum)
        for (idx in 0 until 256 * divsum) {
            dv[idx] = (idx / divsum)
        }

        yw = 0
        yi = 0

        val stack = Array(div) { IntArray(3) }
        var stackpointer: Int
        var stackstart: Int
        var rbs: Int
        val routsum: Int
        val goutsum: Int
        val boutsum: Int
        val rinsum: Int
        val ginsum: Int
        val binsum: Int

        for (curY in 0 until h) {
            rsum = 0
            gsum = 0
            bsum = 0
            for (curI in -radius..radius) {
                p = pix[yi + Math.min(wm, Math.max(curI, 0))]
                val sir = stack[curI + radius]
                sir[0] = (p and 0xff0000) shr 16
                sir[1] = (p and 0x00ff00) shr 8
                sir[2] = (p and 0x0000ff)
                val rbsVal = radius + 1 - Math.abs(curI)
                rsum += sir[0] * rbsVal
                gsum += sir[1] * rbsVal
                bsum += sir[2] * rbsVal
            }
            stackpointer = radius

            for (curX in 0 until w) {
                r[yi] = dv[rsum]
                g[yi] = dv[gsum]
                b[yi] = dv[bsum]

                rsum -= stack[((stackpointer - radius + div) % div)][0]
                gsum -= stack[((stackpointer - radius + div) % div)][1]
                bsum -= stack[((stackpointer - radius + div) % div)][2]

                p = pix[yw + Math.min(curX + radius + 1, wm)]
                val sir = stack[((stackpointer + 1) % div)]
                sir[0] = (p and 0xff0000) shr 16
                sir[1] = (p and 0x00ff00) shr 8
                sir[2] = (p and 0x0000ff)

                rsum += sir[0]
                gsum += sir[1]
                bsum += sir[2]

                stackpointer = (stackpointer + 1) % div
                yi++
            }
            yw += w
        }

        for (curX in 0 until w) {
            rsum = 0
            gsum = 0
            bsum = 0
            yp = -radius * w
            for (curI in -radius..radius) {
                yi = Math.max(0, yp) + curX
                val sir = stack[curI + radius]
                sir[0] = r[yi]
                sir[1] = g[yi]
                sir[2] = b[yi]
                val rbsVal = radius + 1 - Math.abs(curI)
                rsum += r[yi] * rbsVal
                gsum += g[yi] * rbsVal
                bsum += b[yi] * rbsVal
                if (curI < hm) {
                    yp += w
                }
            }
            yi = curX
            stackpointer = radius
            for (curY in 0 until h) {
                pix[yi] = (-0x1000000 and pix[yi]) or (dv[rsum] shl 16) or (dv[gsum] shl 8) or dv[bsum]

                rsum -= stack[((stackpointer - radius + div) % div)][0]
                gsum -= stack[((stackpointer - radius + div) % div)][1]
                bsum -= stack[((stackpointer - radius + div) % div)][2]

                p = curX + (((curY + radius + 1).coerceAtMost(hm)) * w)
                val sir = stack[((stackpointer + 1) % div)]
                sir[0] = r[p]
                sir[1] = g[p]
                sir[2] = b[p]

                rsum += sir[0]
                gsum += sir[1]
                bsum += sir[2]

                stackpointer = (stackpointer + 1) % div
                yi += w
            }
        }

        bitmap.setPixels(pix, 0, w, 0, 0, w, h)
        return bitmap
    }
}
