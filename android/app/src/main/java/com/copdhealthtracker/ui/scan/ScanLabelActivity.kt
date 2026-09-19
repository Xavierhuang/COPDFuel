package com.copdhealthtracker.ui.scan

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.copdhealthtracker.BuildConfig
import com.copdhealthtracker.R
import com.copdhealthtracker.databinding.ActivityScanLabelBinding
import com.copdhealthtracker.labelscan.MlKitBarcodeScanner
import com.copdhealthtracker.labelscan.MlKitLabelOcr
import com.copdhealthtracker.labelscan.NutritionLabelParser
import com.copdhealthtracker.labelscan.UsdaGtinLookup
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScanLabelActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScanLabelBinding
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var viewModel: LabelCaptureViewModel

    private var imageCapture: ImageCapture? = null
    private var isCapturingFront = true
    private var isQrLookupActive = false

    private val scanMode: Int by lazy {
        intent.getIntExtra(EXTRA_SCAN_MODE, SCAN_MODE_LABEL)
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(this, "Camera permission is required to scan labels", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private val reviewLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        setResult(result.resultCode, result.data)
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanLabelBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = androidx.lifecycle.ViewModelProvider(this)[LabelCaptureViewModel::class.java]
        cameraExecutor = Executors.newSingleThreadExecutor()

        configureUiForScanMode()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        binding.captureButton.setOnClickListener { takePhoto() }
        binding.skipFrontButton.setOnClickListener { proceedToNutritionLabel() }
        binding.switchToLabelButton.setOnClickListener { switchToLabelMode() }
    }

    private fun configureUiForScanMode() {
        if (scanMode == SCAN_MODE_QR) {
            binding.captureButton.visibility = View.GONE
            binding.skipFrontButton.visibility = View.GONE
            binding.switchToLabelButton.visibility = View.VISIBLE
            binding.instructionText.text = getString(R.string.scan_qr_code_instruction)
        } else {
            binding.captureButton.visibility = View.VISIBLE
            binding.skipFrontButton.visibility = View.VISIBLE
            binding.switchToLabelButton.visibility = View.GONE
            binding.instructionText.text = getString(R.string.scan_front_label_instruction)
        }
    }

    private fun switchToLabelMode() {
        val intent = Intent(this, ScanLabelActivity::class.java).apply {
            putExtra(EXTRA_SCAN_MODE, SCAN_MODE_LABEL)
        }
        startActivity(intent)
        finish()
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        this, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                if (scanMode == SCAN_MODE_QR) {
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { it.setAnalyzer(cameraExecutor, QrCodeAnalyzer()) }
                    cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)
                } else {
                    imageCapture = ImageCapture.Builder().build()
                    cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Camera failed to start", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        val photoFile = File(
            cacheDir,
            SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis()) + ".jpg"
        )
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Toast.makeText(baseContext, "Photo capture failed", Toast.LENGTH_SHORT).show()
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    if (isCapturingFront) {
                        viewModel.frontLabelUri = savedUri
                        proceedToNutritionLabel()
                    } else {
                        viewModel.nutritionLabelUri = savedUri
                        processLabel()
                    }
                }
            }
        )
    }

    private fun proceedToNutritionLabel() {
        isCapturingFront = false
        binding.instructionText.text = getString(R.string.scan_nutrition_label_instruction)
        binding.skipFrontButton.visibility = View.GONE
    }

    private fun processLabel() {
        val uri = viewModel.nutritionLabelUri ?: return
        binding.instructionText.text = getString(R.string.reading_label)
        binding.captureButton.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val ocr = MlKitLabelOcr(this@ScanLabelActivity)
                val text = ocr.recognize(uri)
                val parsed = NutritionLabelParser.parse(text)
                viewModel.parsedLabel = parsed

                withContext(Dispatchers.Main) {
                    if (parsed.calories == null && parsed.protein == null && parsed.carbs == null && parsed.fat == null) {
                        Toast.makeText(this@ScanLabelActivity, "Could not read nutrition label. Try again.", Toast.LENGTH_LONG).show()
                        binding.captureButton.isEnabled = true
                        binding.instructionText.text = getString(R.string.scan_nutrition_label_instruction)
                    } else {
                        val intent = Intent(this@ScanLabelActivity, LabelReviewActivity::class.java).apply {
                            putExtra(LabelReviewActivity.EXTRA_FRONT_LABEL_URI, viewModel.frontLabelUri?.toString())
                            putExtra(LabelReviewActivity.EXTRA_NUTRITION_LABEL_URI, uri.toString())
                            putExtra(LabelReviewActivity.EXTRA_DATE, intent.getLongExtra(EXTRA_DATE, System.currentTimeMillis()))
                        }
                        reviewLauncher.launch(intent)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ScanLabelActivity, "Could not read label: ${e.message}", Toast.LENGTH_LONG).show()
                    binding.captureButton.isEnabled = true
                }
            }
        }
    }

    private inner class QrCodeAnalyzer : ImageAnalysis.Analyzer {

        private var isProcessing = false

        @androidx.camera.core.ExperimentalGetImage
        override fun analyze(imageProxy: ImageProxy) {
            if (isProcessing || isQrLookupActive) {
                imageProxy.close()
                return
            }
            isProcessing = true

            val mediaImage = imageProxy.image ?: run {
                imageProxy.close()
                isProcessing = false
                return
            }

            val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            val scanner = BarcodeScanning.getClient()
            scanner.process(inputImage)
                .addOnSuccessListener { barcodes ->
                    val gtin = barcodes.firstNotNullOfOrNull { MlKitBarcodeScanner.extractGtin(it.rawValue) }
                    if (gtin != null) {
                        isQrLookupActive = true
                        lookupGtin(gtin)
                    }
                }
                .addOnCompleteListener {
                    imageProxy.close()
                    isProcessing = false
                }
        }

        private fun lookupGtin(gtin: String) {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val apiKey = BuildConfig.USDA_FDC_API_KEY
                    if (apiKey.isBlank()) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@ScanLabelActivity, "USDA API key not configured", Toast.LENGTH_LONG).show()
                        }
                        return@launch
                    }
                    val parsed = UsdaGtinLookup().lookup(gtin, apiKey)
                    withContext(Dispatchers.Main) {
                        if (parsed != null) {
                            val intent = Intent(this@ScanLabelActivity, LabelReviewActivity::class.java).apply {
                                putExtra(LabelReviewActivity.EXTRA_PARSED_LABEL, parsed)
                                putExtra(LabelReviewActivity.EXTRA_DATE, intent.getLongExtra(EXTRA_DATE, System.currentTimeMillis()))
                            }
                            reviewLauncher.launch(intent)
                        } else {
                            isQrLookupActive = false
                            Toast.makeText(this@ScanLabelActivity, "Product not found. Try scanning the label.", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        isQrLookupActive = false
                        Toast.makeText(this@ScanLabelActivity, "Lookup failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        deleteCachedImage(viewModel.frontLabelUri)
        deleteCachedImage(viewModel.nutritionLabelUri)
        cameraExecutor.shutdown()
    }

    private fun deleteCachedImage(uri: Uri?) {
        uri?.path?.let { path ->
            try {
                File(path).delete()
            } catch (_: Exception) {
            }
        }
    }

    companion object {
        const val EXTRA_SCAN_MODE = "extra_scan_mode"
        const val EXTRA_DATE = "extra_date"
        const val SCAN_MODE_LABEL = 0
        const val SCAN_MODE_QR = 1
    }
}
