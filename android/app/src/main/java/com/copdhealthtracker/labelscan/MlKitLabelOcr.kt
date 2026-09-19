package com.copdhealthtracker.labelscan

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.math.hypot
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MlKitLabelOcr(private val context: Context) : LabelOcr {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override suspend fun recognize(uri: Uri): String = recognizeLines(uri).joinToString("\n") { it.text }

    /** Every recognized line with its size, top to bottom as ML Kit reports them. */
    suspend fun recognizeLines(uri: Uri): List<OcrLine> = withContext(Dispatchers.IO) {
        val image = try {
            InputImage.fromFilePath(context, uri)
        } catch (e: IOException) {
            throw IllegalStateException("Could not load image: ${e.message}", e)
        }

        suspendCancellableCoroutine { continuation ->
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val lines = visionText.textBlocks.flatMap { it.lines }.map { line ->
                        // Measure along the line's own left edge: the bounding box of tilted text is
                        // taller than its letters, which would make small print look prominent.
                        val corners = line.cornerPoints
                        val height = if (corners != null && corners.size == 4) {
                            hypot((corners[3].x - corners[0].x).toDouble(), (corners[3].y - corners[0].y).toDouble()).toInt()
                        } else {
                            line.boundingBox?.height() ?: 0
                        }
                        OcrLine(line.text, height, line.boundingBox?.top ?: 0)
                    }
                    lines.forEach { Log.d(TAG, "h=${it.height} top=${it.top} ${it.text}") }
                    continuation.resume(lines)
                }
                .addOnFailureListener { e ->
                    continuation.resumeWithException(e)
                }
        }
    }

    companion object {
        private const val TAG = "MlKitLabelOcr"
    }
}
