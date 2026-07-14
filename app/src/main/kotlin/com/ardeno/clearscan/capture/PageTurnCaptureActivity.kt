package com.ardeno.clearscan.capture

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ardeno.clearscan.R
import com.ardeno.clearscan.ui.theme.ClearScanTheme
import java.io.File
import java.util.concurrent.Executors

class PageTurnCaptureActivity : ComponentActivity() {
    private val capturedPaths = mutableListOf<String>()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(
                this,
                getString(R.string.page_turn_camera_permission),
                Toast.LENGTH_LONG
            ).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }

        setContent {
            ClearScanTheme {
                PageTurnCaptureScreen(
                    onFinish = { finishWithPages() },
                    onCancel = { finish() },
                    onPageCaptured = { path -> capturedPaths.add(path) },
                    capturedPageCount = capturedPaths.size
                )
            }
        }
    }

    private fun finishWithPages() {
        if (capturedPaths.isEmpty()) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }

        setResult(
            Activity.RESULT_OK,
            Intent().putStringArrayListExtra(EXTRA_PAGE_PATHS, ArrayList(capturedPaths))
        )
        finish()
    }

    companion object {
        const val EXTRA_PAGE_PATHS = "extra_page_paths"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PageTurnCaptureScreen(
    onFinish: () -> Unit,
    onCancel: () -> Unit,
    onPageCaptured: (String) -> Unit,
    capturedPageCount: Int
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var pageCount by remember { mutableIntStateOf(capturedPageCount) }
    var statusText by remember {
        mutableStateOf(context.getString(R.string.page_turn_hint))
    }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var isCapturing by remember { mutableStateOf(false) }
    val pageTurnDetector = remember { PageTurnDetector() }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            analysisExecutor.shutdown()
        }
    }

    fun capturePage(triggerResId: Int) {
        val capture = imageCapture ?: return
        if (isCapturing) return

        isCapturing = true
        val outputFile = File(context.cacheDir, "page-turn-${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()

        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    BitmapFactory.decodeFile(outputFile.absolutePath)?.recycle()
                    pageCount += 1
                    onPageCaptured(outputFile.absolutePath)
                    val pagesLabel = context.resources.getQuantityString(
                        R.plurals.document_page_count,
                        pageCount,
                        pageCount
                    )
                    statusText = context.getString(
                        R.string.page_turn_capture_status,
                        context.getString(triggerResId),
                        pagesLabel
                    )
                    pageTurnDetector.reset()
                    isCapturing = false
                }

                override fun onError(exception: ImageCaptureException) {
                    statusText = exception.localizedMessage
                        ?: context.getString(R.string.page_turn_capture_failed)
                    isCapturing = false
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.page_turn_title)) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_go_back)
                        )
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = onFinish,
                        enabled = pageCount > 0,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null)
                        Text(
                            modifier = Modifier.padding(start = 6.dp),
                            text = stringResource(R.string.action_done)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.55f),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                },
                update = { previewView ->
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }
                        val capture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .build()
                        imageCapture = capture

                        val analysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also { imageAnalysis ->
                                imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
                                    analyzeFrame(
                                        imageProxy = imageProxy,
                                        detector = pageTurnDetector,
                                        onPageSettled = {
                                            runOnUiThreadSafe(context) {
                                                if (!isCapturing) {
                                                    capturePage(R.string.page_turn_trigger_auto)
                                                }
                                            }
                                        },
                                        onStatus = { event ->
                                            runOnUiThreadSafe(context) {
                                                statusText = when (event) {
                                                    PageTurnEvent.Motion ->
                                                        context.getString(R.string.page_turn_motion)
                                                    PageTurnEvent.Settling ->
                                                        context.getString(R.string.page_turn_settling)
                                                    PageTurnEvent.PageSettled -> statusText
                                                    PageTurnEvent.None -> statusText
                                                }
                                            }
                                        }
                                    )
                                }
                            }

                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            capture,
                            analysis
                        )
                    }, ContextCompat.getMainExecutor(context))
                }
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = pluralStringResource(
                            R.plurals.document_page_count,
                            pageCount,
                            pageCount
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Button(
                        onClick = { capturePage(R.string.page_turn_trigger_manual) },
                        enabled = !isCapturing
                    ) {
                        Icon(Icons.Filled.Camera, contentDescription = null)
                        Text(
                            modifier = Modifier.padding(start = 8.dp),
                            text = stringResource(R.string.page_turn_capture_now)
                        )
                    }
                }
            }
        }
    }
}

private fun analyzeFrame(
    imageProxy: ImageProxy,
    detector: PageTurnDetector,
    onPageSettled: () -> Unit,
    onStatus: (PageTurnEvent) -> Unit
) {
    val plane = imageProxy.planes.firstOrNull()
    if (plane == null) {
        imageProxy.close()
        return
    }

    val buffer = plane.buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)

    val event = detector.analyze(
        yBuffer = bytes,
        width = imageProxy.width,
        height = imageProxy.height,
        rowStride = plane.rowStride
    )

    if (event == PageTurnEvent.Motion || event == PageTurnEvent.Settling) {
        onStatus(event)
    }
    if (event == PageTurnEvent.PageSettled) {
        onPageSettled()
    }

    imageProxy.close()
}

private fun runOnUiThreadSafe(context: android.content.Context, block: () -> Unit) {
    val activity = context as? Activity ?: return
    activity.runOnUiThread(block)
}
