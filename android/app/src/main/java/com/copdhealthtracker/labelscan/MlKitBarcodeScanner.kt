package com.copdhealthtracker.labelscan

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MlKitBarcodeScanner(private val context: Context) {

    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
    )

    suspend fun scanFromUri(uri: Uri): String? = withContext(Dispatchers.IO) {
        val image = try {
            InputImage.fromFilePath(context, uri)
        } catch (e: IOException) {
            return@withContext null
        }
        suspendCancellableCoroutine { continuation ->
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    continuation.resume(barcodes.firstNotNullOfOrNull { extractGtin(it.rawValue) })
                }
                .addOnFailureListener { e ->
                    continuation.resumeWithException(e)
                }
        }
    }

    companion object {
        fun extractGtin(value: String?): String? {
            if (value.isNullOrBlank()) return null

            // GS1 Digital Link: https://id.gs1.org/gtin/014200000036
            val gs1Regex = Regex("https?://[^/]+/gtin/(\\d+)", RegexOption.IGNORE_CASE)
            gs1Regex.find(value)?.groupValues?.get(1)?.let { return it }

            // Raw numeric GTIN/UPC/EAN (8, 12, 13, or 14 digits)
            val numeric = value.replace(Regex("[^\\d]"), "")
            return if (numeric.length in listOf(8, 12, 13, 14)) numeric else null
        }
    }
}
