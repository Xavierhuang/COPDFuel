package com.copdhealthtracker.ui.scan

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.copdhealthtracker.labelscan.ParsedLabel

class LabelCaptureViewModel : ViewModel() {
    var frontLabelUri: Uri? = null
    var nutritionLabelUri: Uri? = null
    var parsedLabel: ParsedLabel? = null
}
