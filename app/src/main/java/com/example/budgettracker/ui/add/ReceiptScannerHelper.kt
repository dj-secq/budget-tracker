package com.example.budgettracker.ui.add

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.net.Uri
import com.example.budgettracker.domain.ReceiptText
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream

class ReceiptScannerHelper(private val context: Context) {

    data class ScanResult(
        val amount: Double?,
        val note: String
    )

    suspend fun scanReceipt(uri: Uri): ScanResult? = withContext(Dispatchers.IO) {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        var bitmap: Bitmap? = null
        try {
            val decoded = decodeBounded(uri)
            bitmap = decoded.bitmap
            val result = recognizer.process(decoded.image).await()
            val text = result.text
            ScanResult(
                amount = ReceiptText.totalPesos(text),
                note = ReceiptText.merchantLine(text)
            )
        } catch (_: Exception) {
            null
        } finally {
            bitmap?.recycle()
            recognizer.close()
        }
    }

    private fun decodeBounded(uri: Uri): DecodedImage {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("Could not open receipt")
        if (bytes.size > MAX_BYTES) error("Receipt image is too large")
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, MAX_EDGE)
        }
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            ?: error("Could not decode receipt")
        val rotation = rotationDegrees(bytes)
        return DecodedImage(InputImage.fromBitmap(bitmap, rotation), bitmap)
    }

    private fun rotationDegrees(bytes: ByteArray): Int {
        val orientation = ExifInterface(ByteArrayInputStream(bytes)).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    }

    private fun sampleSize(width: Int, height: Int, maxEdge: Int): Int {
        var sample = 1
        val longest = maxOf(width, height)
        while (longest / sample > maxEdge) sample *= 2
        return sample.coerceAtLeast(1)
    }

    private class DecodedImage(val image: InputImage, val bitmap: Bitmap)

    private companion object {
        const val MAX_BYTES = 8 * 1024 * 1024
        const val MAX_EDGE = 1600
    }
}
