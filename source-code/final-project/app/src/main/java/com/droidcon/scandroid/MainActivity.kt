package com.droidcon.scandroid

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.CameraController.COORDINATE_SYSTEM_VIEW_REFERENCED
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.droidcon.scandroid.ui.theme.ScandroidTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ScandroidTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
                    CameraPreview()
                }
            }
        }
    }

    @OptIn(ExperimentalPermissionsApi::class)
    @Composable
    fun CameraPreview() {
        val cameraPermissionState = rememberPermissionState(
            android.Manifest.permission.CAMERA
        )
        if (!cameraPermissionState.status.isGranted) {
            LaunchedEffect(Unit) {
                cameraPermissionState.launchPermissionRequest()
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { getPreview() }
                )
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun getPreview(): PreviewView {
        val cameraController = LifecycleCameraController(this)
        val previewView = PreviewView(this)
        cameraController.cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        val barcodeScanner = BarcodeScanning.getClient(options)

        cameraController.setImageAnalysisAnalyzer(
            Executors.newSingleThreadExecutor(),
            MlKitAnalyzer(
                listOf(barcodeScanner),
                COORDINATE_SYSTEM_VIEW_REFERENCED,
                ContextCompat.getMainExecutor(this)
            ) { result: MlKitAnalyzer.Result? ->
                val barcodeResult = result?.getValue(barcodeScanner)?.firstOrNull()
                if (barcodeResult == null) {
                    previewView.overlay.clear()
                    previewView.setOnTouchListener { _, _ -> false } //no-op
                    return@MlKitAnalyzer
                }

                val drawable = QrCodeHighlightDrawable(barcodeResult.boundingBox!!)
                previewView.overlay.clear()
                previewView.overlay.add(drawable)
                previewView.setOnTouchListener { _, event ->
                    if (event.action == MotionEvent.ACTION_DOWN) {
                        if (barcodeResult.boundingBox!!.contains(event.x.toInt(), event.y.toInt())) {
                            if (barcodeResult.valueType == Barcode.TYPE_URL) {
                                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(barcodeResult.url!!.url))
                                startActivity(browserIntent)
                            }
                        }
                    }
                    true
                }
            }
        )

        cameraController.bindToLifecycle(this)
        previewView.controller = cameraController
        return previewView
    }
}





