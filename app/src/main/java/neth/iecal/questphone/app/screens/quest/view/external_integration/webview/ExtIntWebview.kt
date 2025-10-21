package neth.iecal.questphone.app.screens.quest.view.external_integration.webview

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.util.Log
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.keepScreenOn
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import neth.iecal.questphone.app.screens.quest.view.external_integration.ExternalIntegrationQuestViewVM
import neth.iecal.questphone.data.CommonQuestInfo
import org.json.JSONObject

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ExtIntWebview(
    commonQuestInfo: CommonQuestInfo,
    viewQuestVM: ExternalIntegrationQuestViewVM
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current

    var lastUrl by remember { mutableStateOf<String?>(null) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val allowedPages = remember { mutableStateMapOf<String, Boolean>() } // URL -> allowed
    var pendingPermissionRequest by remember { mutableStateOf<PermissionRequest?>(null) }
    var pendingRequestUrl by remember { mutableStateOf<String?>(null) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        pendingPermissionRequest?.let { request ->
            if (granted) {
//                request.grant(arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE))
            } else {
                pendingPermissionRequest?.deny()
                pendingPermissionRequest = null
                pendingRequestUrl = null
            }
        }
    }

    fun createWebView(): WebView {
        return WebView(context).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                loadWithOverviewMode = true
                useWideViewPort = true
            }
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            addJavascriptInterface(WebAppInterface(context, this, viewQuestVM), "WebAppInterface")

            webViewClient = object : WebViewClient() {

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    isLoading = true
                    isError = false
                }


                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    isLoading = false
                    url?.let { lastUrl = it }

                    val themeJson = JSONObject().apply {
                        put("primary", colors.primary.toColorHex())
                        put("onPrimary", colors.onPrimary.toColorHex())
                        put("secondary", colors.secondary.toColorHex())
                        put("onSecondary", colors.onSecondary.toColorHex())
                        put("tertiary", colors.tertiary.toColorHex())
                        put("onTertiary", colors.onTertiary.toColorHex())
                        put("background", colors.background.toColorHex())
                        put("onBackground", colors.onBackground.toColorHex())
                        put("surface", colors.surface.toColorHex())
                        put("onSurface", colors.onSurface.toColorHex())
                        put("error", colors.error.toColorHex())
                        put("onError", colors.onError.toColorHex())
                    }.toString()

                    view?.evaluateJavascript("applyTheme($themeJson);", null)
                    view?.evaluateJavascript("injectData(${commonQuestInfo.quest_json});", null)
                    Log.d("data", commonQuestInfo.quest_json)
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    super.onReceivedError(view, request, error)
                    isLoading = false
                    isError = true
                    errorMessage = error?.description?.toString() ?: "Unknown error"
                }

                // For older devices
                override fun onReceivedError(
                    view: WebView?,
                    errorCode: Int,
                    description: String?,
                    failingUrl: String?
                ) {
                    super.onReceivedError(view, errorCode, description, failingUrl)
                    isLoading = false
                    isError = true
                    errorMessage = description ?: "Unknown error"
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onPermissionRequest(request: PermissionRequest) {
                    val url = webView?.url ?: "Unknown site"

                    // If page is already allowed → grant immediately
                    if (allowedPages[url] == true) {
                        request.grant(request.resources)
                        return
                    }

                    pendingPermissionRequest = request
                    pendingRequestUrl = url

                    val hasCamera = ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) ==
                            PackageManager.PERMISSION_GRANTED

                    if (!hasCamera) {
                        // Ask for system permission first
                        cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                    }
                    // Else: system permission already granted → show per-page consent dialog automatically
                }
                override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                    consoleMessage?.let {
                        Log.d("WebViewConsole", "${it.message()} -- ${it.sourceId()}:${it.lineNumber()}")
                    }
                    return true
                }
            }

            val json = JSONObject(commonQuestInfo.quest_json)
            if (json.has("webviewUrl")) {
                val url = json.getString("webviewUrl")
                lastUrl = url
                loadUrl(url)
            }
        }
    }

    if (pendingPermissionRequest != null && pendingRequestUrl != null) {
        AlertDialog(
            onDismissRequest = {
                pendingPermissionRequest?.deny()
                pendingPermissionRequest = null
                pendingRequestUrl = null
            },
            title = { Text("Camera Access Request") },
            text = { Text("Do you want to allow ${pendingRequestUrl} to access your camera?") },
            confirmButton = {
                Button(onClick = {
                    pendingPermissionRequest?.grant(arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE))
                    pendingRequestUrl?.let { url -> allowedPages[url] = true }
                    pendingPermissionRequest = null
                    pendingRequestUrl = null
                }) {
                    Text("Allow")
                }
            },
            dismissButton = {
                Button(onClick = {
                    pendingPermissionRequest?.deny()
                    pendingPermissionRequest = null
                    pendingRequestUrl = null
                }) {
                    Text("Deny")
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize().keepScreenOn() ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { webView ?: createWebView().also { webView = it } }
        )

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x80000000)), // semi-transparent overlay
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = colors.primary)
            }
        }

        if (isError) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x80FF0000)), // semi-transparent red overlay
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Error loading page: $errorMessage",
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}
fun Color.toColorHex(): String {
    val r = (red * 255).toInt()
    val g = (green * 255).toInt()
    val b = (blue * 255).toInt()
    return String.format("#%02X%02X%02X", r, g, b)
}
