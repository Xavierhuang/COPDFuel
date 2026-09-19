package com.copdhealthtracker.labelscan

import java.io.Serializable

data class ParsedLabel(
    val productName: String? = null,
    val servingSize: ServingSize? = null,
    val calories: ValueWithConfidence? = null,
    val protein: ValueWithConfidence? = null,
    val carbs: ValueWithConfidence? = null,
    val fat: ValueWithConfidence? = null,
    val rawLines: List<String> = emptyList()
) : Serializable

data class ServingSize(
    val description: String,
    val grams: Double?
) : Serializable

data class ValueWithConfidence(
    val value: Double,
    val unit: String,
    val confidence: Confidence
) : Serializable

enum class Confidence {
    HIGH,
    MEDIUM,
    LOW
}
