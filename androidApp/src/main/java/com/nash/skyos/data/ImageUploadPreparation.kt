package com.nash.skyos.data

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

data class PreparedImageUpload(
    val data: ByteArray,
    val mimeType: String = "image/jpeg",
    val fileExtension: String = "jpg",
)

object ImageUploadPreparation {
    private const val MAX_DIMENSION = 2048
    private const val JPEG_QUALITY = 86

    fun prepare(
        contentResolver: ContentResolver,
        uri: Uri,
    ): PreparedImageUpload {
        val bitmap = decodeBitmap(contentResolver, uri)
        return try {
            ByteArrayOutputStream().use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)) {
                    "Bild konnte nicht komprimiert werden."
                }
                PreparedImageUpload(data = output.toByteArray())
            }
        } finally {
            bitmap.recycle()
        }
    }

    private fun decodeBitmap(
        contentResolver: ContentResolver,
        uri: Uri,
    ): Bitmap {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val size = info.size
                val largestSide = max(size.width, size.height)
                if (largestSide > MAX_DIMENSION) {
                    val scale = MAX_DIMENSION.toFloat() / largestSide.toFloat()
                    decoder.setTargetSize(
                        (size.width * scale).roundToInt().coerceAtLeast(1),
                        (size.height * scale).roundToInt().coerceAtLeast(1),
                    )
                }
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.isMutableRequired = false
            }
        } else {
            val bounds = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            contentResolver.openInputStream(uri).use { input ->
                BitmapFactory.decodeStream(input, null, bounds)
            }
            val options = BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight)
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            contentResolver.openInputStream(uri).use { input ->
                BitmapFactory.decodeStream(input, null, options)
            } ?: error("Bild konnte nicht gelesen werden.")
        }
    }

    private fun calculateInSampleSize(
        width: Int,
        height: Int,
    ): Int {
        var sampleSize = 1
        var currentWidth = width
        var currentHeight = height
        while (max(currentWidth, currentHeight) > MAX_DIMENSION) {
            sampleSize *= 2
            currentWidth /= 2
            currentHeight /= 2
        }
        return sampleSize.coerceAtLeast(1)
    }
}
