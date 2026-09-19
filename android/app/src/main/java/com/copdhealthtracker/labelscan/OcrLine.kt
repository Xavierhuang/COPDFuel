package com.copdhealthtracker.labelscan

/** One line of recognized text. [height] and [top] are in image pixels. */
data class OcrLine(val text: String, val height: Int, val top: Int)
