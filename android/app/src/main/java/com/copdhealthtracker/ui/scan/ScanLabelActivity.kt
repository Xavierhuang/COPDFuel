package com.copdhealthtracker.ui.scan

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.copdhealthtracker.R

/**
 * CameraX activity for scanning nutrition labels or QR codes.
 *
 * This is a placeholder so the AddFoodDialog integration can compile.
 * The full implementation will capture front/nutrition label photos and
 * detect QR codes.
 */
class ScanLabelActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan_label)
    }

    companion object {
        const val EXTRA_SCAN_MODE = "extra_scan_mode"
        const val SCAN_MODE_LABEL = 0
        const val SCAN_MODE_QR = 1
    }
}
