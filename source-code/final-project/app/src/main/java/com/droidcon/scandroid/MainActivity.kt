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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
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

	private lateinit var cameraController: LifecycleCameraController

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContent {
			ScandroidTheme {
				Surface(
					modifier = Modifier.fillMaxSize(),
					color = MaterialTheme.colors.background
				) {
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

				var enableFlash by remember { mutableStateOf(false) }
				val vectorAsset = if (enableFlash) {
					R.drawable.flash_off
				} else {
					R.drawable.flash_on
				}
				Icon(
					painterResource(id = vectorAsset), contentDescription = "Flash", modifier = Modifier
						.align(Alignment.BottomCenter)
						.padding(bottom = 24.dp)
						.size(64.dp)
						.clickable {
							enableFlash = !enableFlash
							cameraController.cameraControl?.enableTorch(enableFlash)
						})
			}
		}
	}

	@SuppressLint("ClickableViewAccessibility")
	private fun getPreview(): PreviewView {

		val options = BarcodeScannerOptions.Builder()
			.setBarcodeFormats(Barcode.FORMAT_QR_CODE)
			.build()
		val barcodeScanner = BarcodeScanning.getClient(options)

		cameraController = LifecycleCameraController(this)
		val previewView = PreviewView(this)

		cameraController.cameraSelector = CameraSelector.Builder()
			.requireLensFacing(CameraSelector.LENS_FACING_BACK)
			.build()

		cameraController.setImageAnalysisAnalyzer(
			Executors.newSingleThreadExecutor(),
			MlKitAnalyzer(
				listOf(barcodeScanner),
				COORDINATE_SYSTEM_VIEW_REFERENCED,
				ContextCompat.getMainExecutor(this)
			) { result ->
				val barcodeResults = result?.getValue(barcodeScanner)
				if (barcodeResults?.firstOrNull() == null) {
					previewView.overlay.clear()
					previewView.setOnTouchListener { _, _ -> false } //no-op
					return@MlKitAnalyzer
				}

				val firstResult = barcodeResults.first()
				val drawable = QrCodeHighlightDrawable(firstResult.boundingBox!!)
				previewView.overlay.clear()
				previewView.overlay.add(drawable)
				previewView.setOnTouchListener { _, event ->
					if (event.action == MotionEvent.ACTION_DOWN) {
						if (firstResult.boundingBox!!.contains(event.x.toInt(), event.y.toInt())) {
							val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(firstResult.url!!.url))
							startActivity(browserIntent)
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
