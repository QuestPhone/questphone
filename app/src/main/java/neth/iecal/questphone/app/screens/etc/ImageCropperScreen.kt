package neth.iecal.questphone.app.screens.etc

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.min

data class CropRect(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
) {
    fun toRect() = Rect(x, y, x + width, y + height)

    fun getHandles(): List<Handle> {
        val handleSize = 24f
        return listOf(
            Handle(HandleType.TOP_LEFT, Offset(x - handleSize/2, y - handleSize/2)),
            Handle(HandleType.TOP_RIGHT, Offset(x + width - handleSize/2, y - handleSize/2)),
            Handle(HandleType.BOTTOM_LEFT, Offset(x - handleSize/2, y + height - handleSize/2)),
            Handle(HandleType.BOTTOM_RIGHT, Offset(x + width - handleSize/2, y + height - handleSize/2)),
            Handle(HandleType.TOP, Offset(x + width/2 - handleSize/2, y - handleSize/2)),
            Handle(HandleType.BOTTOM, Offset(x + width/2 - handleSize/2, y + height - handleSize/2)),
            Handle(HandleType.LEFT, Offset(x - handleSize/2, y + height/2 - handleSize/2)),
            Handle(HandleType.RIGHT, Offset(x + width - handleSize/2, y + height/2 - handleSize/2))
        )
    }
}

enum class HandleType {
    TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT,
    TOP, BOTTOM, LEFT, RIGHT, MOVE
}

data class Handle(
    val type: HandleType,
    val position: Offset
)

@Composable
fun ImageCropperScreen(imageBitmap: MutableState<ImageBitmap?>, outputImg: String, onCropped: () -> Unit) {
    val context = LocalContext.current
    val density = LocalDensity.current

    var croppedImageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Container and image dimensions
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var imageDisplaySize by remember { mutableStateOf(Size.Zero) }
    var imageOffset by remember { mutableStateOf(Offset.Zero) }

    // Crop rectangle state
    var cropRect by remember {
        mutableStateOf(CropRect(50f, 50f, 200f, 200f))
    }

    // Interaction state
    var activeHandle by remember { mutableStateOf<HandleType?>(null) }
    var isDragging by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // Image cropping area
        Box(
            modifier = Modifier
                .safeDrawingPadding()
                .padding(16.dp)
                .weight(1f)
                .fillMaxWidth()
                .onGloballyPositioned { layoutCoordinates ->
                    containerSize = layoutCoordinates.size

                    // Calculate image display dimensions
                    imageBitmap.value?.let { bitmap ->
                        val imageAspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
                        val containerAspectRatio = containerSize.width.toFloat() / containerSize.height.toFloat()

                        if (imageAspectRatio > containerAspectRatio) {
                            // Image is wider than container
                            imageDisplaySize = Size(
                                containerSize.width.toFloat(),
                                containerSize.width.toFloat() / imageAspectRatio
                            )
                        } else {
                            // Image is taller than container
                            imageDisplaySize = Size(
                                containerSize.height.toFloat() * imageAspectRatio,
                                containerSize.height.toFloat()
                            )
                        }

                        imageOffset = Offset(
                            (containerSize.width - imageDisplaySize.width) / 2,
                            (containerSize.height - imageDisplaySize.height) / 2
                        )

                        // Initialize crop rect to center of image
                        val initialSize = min(imageDisplaySize.width, imageDisplaySize.height) * 0.6f
                        cropRect = CropRect(
                            imageOffset.x + (imageDisplaySize.width - initialSize) / 2,
                            imageOffset.y + (imageDisplaySize.height - initialSize) / 2,
                            initialSize,
                            initialSize
                        )
                    }
                }
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (imageBitmap.value != null) {
                // Display the image
                Image(
                    bitmap = imageBitmap.value!!,
                    contentDescription = "Image to crop",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )

                // Cropping overlay
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    isDragging = true
                                    activeHandle = getActiveHandle(offset, cropRect)
                                },
                                onDragEnd = {
                                    isDragging = false
                                    activeHandle = null
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()

                                    activeHandle?.let { handle ->
                                        cropRect = updateCropRect(
                                            cropRect,
                                            handle,
                                            dragAmount,
                                            imageOffset,
                                            imageDisplaySize
                                        )
                                    }
                                }
                            )
                        }
                ) {
                    drawCropOverlay(cropRect, imageOffset, imageDisplaySize)
                }
            }
        }

        // Control buttons
        Row(
            horizontalArrangement = Arrangement.End,
        ) {
            IconButton(
                onClick = {
                    // Reset crop to center
                    val initialSize = min(imageDisplaySize.width, imageDisplaySize.height) * 0.6f
                    cropRect = CropRect(
                        imageOffset.x + (imageDisplaySize.width - initialSize) / 2,
                        imageOffset.y + (imageDisplaySize.height - initialSize) / 2,
                        initialSize,
                        initialSize
                    )
                },
                enabled = imageBitmap.value != null && !isLoading,
                modifier = Modifier.size(50.dp)

            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset Crop"
                )
            }

            Spacer(modifier = Modifier.size(16.dp))
            IconButton(
                onClick = {
                    imageBitmap.value?.let { bitmap ->
                        isLoading = true
                        errorMessage = null

                        try {
                            val croppedBmp = performCrop(bitmap, cropRect, imageOffset, imageDisplaySize)
                            croppedImageBitmap = croppedBmp.asImageBitmap()
                            saveBitmapToFile(context, croppedBmp, outputImg){
                                onCropped()
                            }
                        } catch (e: Exception) {
                            errorMessage = "Failed to crop image: ${e.message}"
                        } finally {
                            isLoading = false
                        }
                    }
                },
                enabled = imageBitmap.value != null && !isLoading,
                modifier = Modifier.size(50.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Done,
                    contentDescription = "Done Crop",
                )
            }

        }

        Spacer(
            Modifier.windowInsetsBottomHeight(
                WindowInsets.systemBars
            )
        )
    }
}

/**
 * Determines which handle (if any) is being touched
 */
fun getActiveHandle(touchPoint: Offset, cropRect: CropRect): HandleType? {
    val handles = cropRect.getHandles()
    val touchRadius = 40f // Generous touch area

    // Check if touching inside crop rect for moving
    val cropRectArea = cropRect.toRect()
    if (cropRectArea.contains(touchPoint)) {
        // Check if it's close to any handle first
        for (handle in handles) {
            val distance = (touchPoint - handle.position - Offset(12f, 12f)).getDistance()
            if (distance <= touchRadius) {
                return handle.type
            }
        }
        // If not on a handle, return move
        return HandleType.MOVE
    }

    // Check handles even outside crop rect
    for (handle in handles) {
        val handleCenter = handle.position + Offset(12f, 12f)
        val distance = (touchPoint - handleCenter).getDistance()
        if (distance <= touchRadius) {
            return handle.type
        }
    }

    return null
}

/**
 * Updates the crop rectangle based on handle drag
 */
fun updateCropRect(
    current: CropRect,
    handleType: HandleType,
    dragAmount: Offset,
    imageOffset: Offset,
    imageSize: Size
): CropRect {
    val minSize = 50f
    val imageRect = Rect(imageOffset, imageSize)

    return when (handleType) {
        HandleType.MOVE -> {
            val newX = (current.x + dragAmount.x).coerceIn(
                imageRect.left,
                imageRect.right - current.width
            )
            val newY = (current.y + dragAmount.y).coerceIn(
                imageRect.top,
                imageRect.bottom - current.height
            )
            current.copy(x = newX, y = newY)
        }

        HandleType.TOP_LEFT -> {
            val newX = (current.x + dragAmount.x).coerceIn(
                imageRect.left,
                current.x + current.width - minSize
            )
            val newY = (current.y + dragAmount.y).coerceIn(
                imageRect.top,
                current.y + current.height - minSize
            )
            current.copy(
                x = newX,
                y = newY,
                width = current.width - (newX - current.x),
                height = current.height - (newY - current.y)
            )
        }

        HandleType.TOP_RIGHT -> {
            val newY = (current.y + dragAmount.y).coerceIn(
                imageRect.top,
                current.y + current.height - minSize
            )
            val newWidth = (current.width + dragAmount.x).coerceAtLeast(minSize)
                .coerceAtMost(imageRect.right - current.x)
            current.copy(
                y = newY,
                width = newWidth,
                height = current.height - (newY - current.y)
            )
        }

        HandleType.BOTTOM_LEFT -> {
            val newX = (current.x + dragAmount.x).coerceIn(
                imageRect.left,
                current.x + current.width - minSize
            )
            val newHeight = (current.height + dragAmount.y).coerceAtLeast(minSize)
                .coerceAtMost(imageRect.bottom - current.y)
            current.copy(
                x = newX,
                width = current.width - (newX - current.x),
                height = newHeight
            )
        }

        HandleType.BOTTOM_RIGHT -> {
            val newWidth = (current.width + dragAmount.x).coerceAtLeast(minSize)
                .coerceAtMost(imageRect.right - current.x)
            val newHeight = (current.height + dragAmount.y).coerceAtLeast(minSize)
                .coerceAtMost(imageRect.bottom - current.y)
            current.copy(width = newWidth, height = newHeight)
        }

        HandleType.TOP -> {
            val newY = (current.y + dragAmount.y).coerceIn(
                imageRect.top,
                current.y + current.height - minSize
            )
            current.copy(
                y = newY,
                height = current.height - (newY - current.y)
            )
        }

        HandleType.BOTTOM -> {
            val newHeight = (current.height + dragAmount.y).coerceAtLeast(minSize)
                .coerceAtMost(imageRect.bottom - current.y)
            current.copy(height = newHeight)
        }

        HandleType.LEFT -> {
            val newX = (current.x + dragAmount.x).coerceIn(
                imageRect.left,
                current.x + current.width - minSize
            )
            current.copy(
                x = newX,
                width = current.width - (newX - current.x)
            )
        }

        HandleType.RIGHT -> {
            val newWidth = (current.width + dragAmount.x).coerceAtLeast(minSize)
                .coerceAtMost(imageRect.right - current.x)
            current.copy(width = newWidth)
        }
    }
}

/**
 * Draws the crop overlay with handles
 */
fun DrawScope.drawCropOverlay(
    cropRect: CropRect,
    imageOffset: Offset,
    imageSize: Size
) {
    val imageRect = Rect(imageOffset, imageSize)

    // Draw dark overlay outside crop area
    // Top
    if (cropRect.y > imageRect.top) {
        drawRect(
            color = Color.Black.copy(alpha = 0.6f),
            topLeft = imageRect.topLeft,
            size = Size(imageRect.width, cropRect.y - imageRect.top)
        )
    }

    // Bottom
    if (cropRect.y + cropRect.height < imageRect.bottom) {
        drawRect(
            color = Color.Black.copy(alpha = 0.6f),
            topLeft = Offset(imageRect.left, cropRect.y + cropRect.height),
            size = Size(imageRect.width, imageRect.bottom - (cropRect.y + cropRect.height))
        )
    }

    // Left
    drawRect(
        color = Color.Black.copy(alpha = 0.6f),
        topLeft = Offset(imageRect.left, cropRect.y),
        size = Size(cropRect.x - imageRect.left, cropRect.height)
    )

    // Right
    drawRect(
        color = Color.Black.copy(alpha = 0.6f),
        topLeft = Offset(cropRect.x + cropRect.width, cropRect.y),
        size = Size(imageRect.right - (cropRect.x + cropRect.width), cropRect.height)
    )

    // Draw crop rectangle border
    drawRect(
        color = Color.White,
        topLeft = Offset(cropRect.x, cropRect.y),
        size = Size(cropRect.width, cropRect.height),
        style = Stroke(width = 3f)
    )

    // Draw grid lines (rule of thirds)
    val gridColor = Color.White.copy(alpha = 0.7f)
    val strokeWidth = 1f

    // Vertical lines
    for (i in 1..2) {
        val x = cropRect.x + (cropRect.width * i / 3)
        drawLine(
            color = gridColor,
            start = Offset(x, cropRect.y),
            end = Offset(x, cropRect.y + cropRect.height),
            strokeWidth = strokeWidth
        )
    }

    // Horizontal lines
    for (i in 1..2) {
        val y = cropRect.y + (cropRect.height * i / 3)
        drawLine(
            color = gridColor,
            start = Offset(cropRect.x, y),
            end = Offset(cropRect.x + cropRect.width, y),
            strokeWidth = strokeWidth
        )
    }

    // Draw handles
    val handles = cropRect.getHandles()
    for (handle in handles) {
        val handleSize = 24f
        val handleColor = Color.White
        val handleBorderColor = Color.Gray

        // Handle background
        drawRect(
            color = handleColor,
            topLeft = handle.position,
            size = Size(handleSize, handleSize)
        )

        // Handle border
        drawRect(
            color = handleBorderColor,
            topLeft = handle.position,
            size = Size(handleSize, handleSize),
            style = Stroke(width = 2f)
        )
    }
}

/**
 * Performs the actual cropping operation
 */
fun performCrop(
    imageBitmap: ImageBitmap,
    cropRect: CropRect,
    imageOffset: Offset,
    imageDisplaySize: Size
): Bitmap {
    val originalBitmap = imageBitmap.asAndroidBitmap()

    // Calculate scaling factors
    val scaleX = originalBitmap.width.toFloat() / imageDisplaySize.width
    val scaleY = originalBitmap.height.toFloat() / imageDisplaySize.height

    // Convert crop coordinates to original image coordinates
    val cropX = ((cropRect.x - imageOffset.x) * scaleX).toInt().coerceAtLeast(0)
    val cropY = ((cropRect.y - imageOffset.y) * scaleY).toInt().coerceAtLeast(0)
    val cropWidth = (cropRect.width * scaleX).toInt()
        .coerceAtMost(originalBitmap.width - cropX)
    val cropHeight = (cropRect.height * scaleY).toInt()
        .coerceAtMost(originalBitmap.height - cropY)

    return Bitmap.createBitmap(
        originalBitmap,
        cropX,
        cropY,
        cropWidth,
        cropHeight
    )
}

/**
 * Saves a Bitmap to a file in the app's internal storage.
 */
fun saveBitmapToFile(context: Context, bitmap: Bitmap, filename: String, onDone: ()-> Unit) {
    val file = File(context.filesDir, filename)
    try {
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        }
        onDone()
    } catch (e: IOException) {
        e.printStackTrace()
        // Handle error appropriately
        throw e
    }
}

/**
 * Extension function to convert Compose ImageBitmap to Android Bitmap
 */
fun ImageBitmap.toAndroidBitmap(): Bitmap {
    val softwareBitmap = this.asAndroidBitmap()
    return softwareBitmap.copy(Bitmap.Config.ARGB_8888, false)
}