package neth.iecal.questphone.app.screens.quest.view.ai_snap

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import neth.iecal.questphone.R
import neth.iecal.questphone.app.screens.quest.setup.ai_snap.model.ModelDownloadDialog
import java.io.File
import java.io.FileOutputStream

const val AI_SNAP_PIC = "ai_snap_captured_image_cropped.jpeg"

@Composable
fun CameraScreen(onPicClicked: ()->Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var cameraSelector by remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }
    var flashEnabled by remember { mutableStateOf(false) }
    val imageCapture = remember { ImageCapture.Builder().build() }
    val previewView = remember { PreviewView(context) }
    // Permission handling
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Log.e("CameraScreen", "Camera permission denied")
        }
    }
    var isModelDownloadDialogVisible = remember { mutableStateOf(false) }

    var imageBitmap = remember { mutableStateOf<ImageBitmap?>(null) }


    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    ModelDownloadDialog(
        allowSkipping = false,
        modelDownloadDialogVisible = isModelDownloadDialogVisible
    )
    BackHandler(imageBitmap.value!=null) {
        imageBitmap.value = null
    }
        Box(
            modifier = Modifier.fillMaxSize()
        ) {

            AndroidView(
                factory = { previewView },
                modifier = Modifier
                    .fillMaxSize()
                    .aspectRatio(4.3f),
                update = { view ->
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder()
                        .setResolutionSelector(
                            ResolutionSelector.Builder()
                                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                                .build()
                        )
                        .build().also {
                            it.surfaceProvider = view.surfaceProvider
                        }

                    try {
                        cameraProvider.unbindAll()
                        val camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageCapture
                        )
                        // Enable/disable flashlight
                        camera.cameraControl.enableTorch(flashEnabled)
                    } catch (e: Exception) {
                        Log.e("CameraScreen", "Camera binding failed", e)
                    }
                }
            )
            // Control Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter) // Position at bottom)
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), // Semi-transparent
                        shape = RoundedCornerShape(16.dp),
                    )
                    .padding(horizontal = 16.dp, vertical = 50.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if(neth.iecal.questphone.BuildConfig.IS_FDROID) {
                    IconButton(
                        onClick = {
                            isModelDownloadDialogVisible.value = true
                        },
                        modifier = Modifier
                            .size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            tint = MaterialTheme.colorScheme.onSurface,
                            contentDescription = "Choose Model"
                        )
                    }
                }
                IconButton(
                    onClick = {
                        // Switch between front and back camera
                        cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        } else {
                            CameraSelector.DEFAULT_BACK_CAMERA
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_cameraswitch_24),
                        tint = MaterialTheme.colorScheme.onSurface,
                        contentDescription = "Switch Camera"
                    )
                }

                IconButton(
                    onClick = {
                        // Toggle flashlight
                        flashEnabled = !flashEnabled
                    },
                    modifier = Modifier
                        .size(48.dp)
                ) {
                    Icon(
                        painter = painterResource(id = if (flashEnabled) R.drawable.baseline_flash_on_24 else R.drawable.baseline_flash_off_24),
                        tint = MaterialTheme.colorScheme.onSurface,
                        contentDescription = "Toggle Flash"
                    )
                }

                IconButton(
                    onClick = {
                        // Take picture
                        takePicture(context, imageCapture, {
                            imageBitmap.value = it
                            onPicClicked()
                        })
                    },
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_camera_24),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        contentDescription = "Capture"
                    )
                }

            }
        }
    }
private fun takePicture(
    context: Context,
    imageCapture: ImageCapture,
    onImageCaptured: (ImageBitmap) -> Unit
) {
    imageCapture.takePicture(
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(imageProxy: ImageProxy) {
                // Convert ImageProxy to Bitmap
                val bitmap = imageProxy.toBitmap()
                val file = File(context.filesDir, AI_SNAP_PIC)
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                }

                // Convert Bitmap to ImageBitmap (for Compose)
                val imageBitmap = bitmap.asImageBitmap()

                onImageCaptured(imageBitmap)

                imageProxy.close() // Always close to avoid memory leaks
            }

            override fun onError(exception: ImageCaptureException) {
                Log.d("Error Capturing img",exception.message.toString())
            }
        }
    )
}
