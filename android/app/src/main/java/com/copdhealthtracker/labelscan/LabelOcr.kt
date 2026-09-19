package com.copdhealthtracker.labelscan

import android.net.Uri

interface LabelOcr {
    suspend fun recognize(uri: Uri): String
}
