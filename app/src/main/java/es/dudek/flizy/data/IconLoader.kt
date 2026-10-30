package es.dudek.flizy.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

/** Ikony aplikacji: kolorowe (lista) i białe monochromatyczne (kafelki), z pamięcią podręczną. */
class IconLoader(context: Context) {
    private val densityDpi = context.resources.displayMetrics.densityDpi

    private class Mono(val bitmap: ImageBitmap?)

    private val colorCache = LruCache<AppKey, ImageBitmap>(400)
    private val monoCache = LruCache<AppKey, Mono>(200)

    fun cachedIcon(app: AppEntry): ImageBitmap? = colorCache.get(app.key)

    fun cachedMono(app: AppEntry): ImageBitmap? = monoCache.get(app.key)?.bitmap

    suspend fun icon(app: AppEntry, sizePx: Int): ImageBitmap =
        colorCache.get(app.key) ?: withContext(Dispatchers.Default) {
            val drawable = app.info.getBadgedIcon(densityDpi)
            render(drawable, sizePx).asImageBitmap().also { colorCache.put(app.key, it) }
        }

    /** Biała wersja ikony albo null, gdy po przefiltrowaniu wyszłaby nieczytelna plama. */
    suspend fun monoIcon(app: AppEntry): ImageBitmap? {
        monoCache.get(app.key)?.let { return it.bitmap }
        return withContext(Dispatchers.Default) {
            // Bez runCatching: połknąłby anulowanie korutyny (np. przebudowa kafelka)
            // i "nieczytelna ikona" zostałaby zapamiętana na stałe.
            val bitmap = try {
                MonoIcons.create(app.info.getIcon(densityDpi), MONO_SIZE)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("Flizy", "Nie udało się utworzyć ikony ${app.key}", e)
                return@withContext null // nie zapamiętujemy - następnym razem spróbuje ponownie
            }
            bitmap?.asImageBitmap().also { monoCache.put(app.key, Mono(it)) }
        }
    }

    fun evict(packageName: String) {
        colorCache.snapshot().keys.filter { it.packageName == packageName }.forEach { colorCache.remove(it) }
        monoCache.snapshot().keys.filter { it.packageName == packageName }.forEach { monoCache.remove(it) }
    }

    companion object {
        const val MONO_SIZE = 144
    }
}

internal fun render(drawable: Drawable, size: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, size, size)
    drawable.draw(canvas)
    return bitmap
}

/**
 * Warstwa ikony adaptacyjnej ma 108dp, z czego widoczne jest środkowe 72dp (2/3),
 * więc rysujemy ją powiększoną o połowę i przesuniętą.
 */
private fun renderAdaptiveLayer(layer: Drawable, size: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val inset = size / 4
    layer.setBounds(-inset, -inset, size + inset, size + inset)
    layer.draw(canvas)
    return bitmap
}

internal object MonoIcons {
    /** Poniżej tego pokrycia kształt traktujemy jako glif na przezroczystym tle. */
    private const val SPARSE_COVERAGE = 0.35f
    private const val MIN_COVERAGE = 0.012f
    private const val MAX_COVERAGE = 0.42f

    /** Tło musi być w miarę jednolite, inaczej (gradient, zdjęcie) nie da się wyciąć glifu. */
    private const val MIN_DOMINANT_SHARE = 0.5f

    fun create(drawable: Drawable, size: Int): Bitmap? {
        if (drawable is AdaptiveIconDrawable) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                drawable.monochrome?.let { mono ->
                    return finish(alphaMask(renderAdaptiveLayer(mono, size)))
                }
            }
            drawable.foreground?.let { fg ->
                val pixels = renderAdaptiveLayer(fg, size).pixels()
                if (coverage(pixels) < SPARSE_COVERAGE) return finish(alphaMask(pixels))
                contrastMask(pixels)?.let { return finish(it) }
            }
        }
        val pixels = render(drawable, size).pixels()
        return finish(if (coverage(pixels) < SPARSE_COVERAGE) alphaMask(pixels) else contrastMask(pixels))
    }

    private fun Bitmap.pixels(): IntArray = IntArray(width * height).also { getPixels(it, 0, width, 0, 0, width, height) }

    private fun coverage(pixels: IntArray): Float = pixels.count { (it ushr 24) > 128 }.toFloat() / pixels.size

    private fun alphaMask(bitmap: Bitmap): IntArray = alphaMask(bitmap.pixels())

    private fun alphaMask(pixels: IntArray): IntArray = IntArray(pixels.size) { i -> (pixels[i] and 0xFF000000.toInt()) or 0xFFFFFF }

    /**
     * Wypełniony kształt (np. kolorowe kółko z symbolem): znajdujemy dominujący kolor tła
     * i zostawiamy tylko piksele, które wyraźnie się od niego różnią.
     */
    private fun contrastMask(pixels: IntArray): IntArray? {
        val buckets = HashMap<Int, IntArray>() // kubełek -> [liczba, sumaR, sumaG, sumaB]
        var opaque = 0
        for (p in pixels) {
            if ((p ushr 24) <= 128) continue
            opaque++
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            val acc = buckets.getOrPut(((r shr 6) shl 4) or ((g shr 6) shl 2) or (b shr 6)) { IntArray(4) }
            acc[0]++; acc[1] += r; acc[2] += g; acc[3] += b
        }
        if (opaque == 0) return null
        val dominant = buckets.values.maxBy { it[0] }
        if (dominant[0].toFloat() / opaque < MIN_DOMINANT_SHARE) return null
        val dr = dominant[1] / dominant[0]
        val dg = dominant[2] / dominant[0]
        val db = dominant[3] / dominant[0]

        return IntArray(pixels.size) { i ->
            val p = pixels[i]
            val a = p ushr 24
            val r = ((p shr 16) and 0xFF) - dr
            val g = ((p shr 8) and 0xFF) - dg
            val b = (p and 0xFF) - db
            val distance = sqrt((r * r + g * g + b * b).toFloat())
            val strength = ((distance - 45f) / 45f).coerceIn(0f, 1f)
            ((a * strength).toInt() shl 24) or 0xFFFFFF
        }
    }

    private fun finish(mask: IntArray?): Bitmap? {
        if (mask == null) return null
        val c = coverage(mask)
        if (c < MIN_COVERAGE || c > MAX_COVERAGE) return null
        val size = sqrt(mask.size.toFloat()).toInt()
        return Bitmap.createBitmap(mask, size, size, Bitmap.Config.ARGB_8888)
    }
}
