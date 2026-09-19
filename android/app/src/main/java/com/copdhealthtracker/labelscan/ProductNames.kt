package com.copdhealthtracker.labelscan

internal object ProductNames {

    /** Re-cases shouty text ("QUAKER" -> "Quaker"); names already in mixed case are left alone. */
    fun readable(text: String): String {
        if (text.any { it.isLowerCase() }) return text
        val result = StringBuilder()
        for ((i, c) in text.withIndex()) {
            val previous = if (i > 0) text[i - 1] else ' '
            // A letter starts a word unless it follows a letter or an apostrophe (KELLOGG'S, M&M'S).
            val startsWord = !previous.isLetter() && previous != '\'' && previous != '\u2019'
            result.append(if (startsWord) c.uppercaseChar() else c.lowercaseChar())
        }
        return result.toString()
    }
}
