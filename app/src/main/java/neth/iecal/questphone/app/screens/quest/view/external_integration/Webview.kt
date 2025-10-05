package neth.iecal.questphone.app.screens.quest.view.external_integration

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import kotlinx.io.IOException
import neth.iecal.questphone.app.screens.quest.view.ExternalIntegrationQuestViewVM
import neth.iecal.questphone.core.utils.reminder.simpleAlarm.AlarmHelper
import neth.iecal.questphone.data.CommonQuestInfo
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebView(
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

            val json = JSONObject(commonQuestInfo.quest_json)
            if (json.has("webviewUrl")) {
                val url = json.getString("webviewUrl")
                lastUrl = url
                loadUrl(url)
            }
        }
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

class WebAppInterface(private val context: Context, private val webView: WebView, private val viewQuestVM: ExternalIntegrationQuestViewVM) {

    private val client = OkHttpClient()

    @JavascriptInterface
    fun onQuestCompleted() {
        viewQuestVM.saveMarkedQuestToDb()
        Log.d("WebAppInterface", "Quest Completed")
        android.widget.Toast.makeText(
            context,
            "Quest completed!",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }

    @JavascriptInterface
    fun toast(msg: String) {
        Log.d("WebAppInterfaceToast",msg)
        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
    }
    @JavascriptInterface
    fun isQuestCompleted():Boolean{
        return viewQuestVM.isQuestComplete.value
    }
    @JavascriptInterface
    fun enableFullScreen() {
        viewQuestVM.isFullScreen.value = true
    }
    fun disableFullScreen(){
        viewQuestVM.isFullScreen.value = false
    }
    @JavascriptInterface
    fun setAlarmedNotification(triggerMillis: Long,title:String,description: String){
        val alarmManager = AlarmHelper(context)
        alarmManager.setAlarm(triggerMillis,title,description)
    }

    @JavascriptInterface
    fun getCoinRewardRatio():Int{
        val sp = context.getSharedPreferences("minutes_per_5", Context.MODE_PRIVATE)
        return sp.getInt("minutes_per_5",10)
    }
    @JavascriptInterface
    fun fetchDataWithoutCorsAsync(url: String, headersJson: String?, callback: String) {
        Log.d("Webview","Fetching without cors")
        Thread {
            val result = try {
                val builder = Request.Builder().url(url)

                // Parse headers JSON from JS
                headersJson?.let {
                    val json = JSONObject(it)
                    val headersBuilder = Headers.Builder()
                    json.keys().forEach { key ->
                        headersBuilder.add(key, json.getString(key))
                    }
                    builder.headers(headersBuilder.build())
                }

                val request = builder.build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        "{\"error\":\"${response.code}\"}"
                    } else {
                        response.body?.string() ?: "{}"
                    }
                }
            } catch (e: IOException) {
                "{\"error\":\"${e.message}\"}"
            }

            // Post result back to JS callback on UI thread
            webView.post {
                webView.evaluateJavascript("$callback(${JSONObject.quote(result)});", null)
            }
        }.start()
    }
}
fun Color.toColorHex(): String {
    val r = (red * 255).toInt()
    val g = (green * 255).toInt()
    val b = (blue * 255).toInt()
    return String.format("#%02X%02X%02X", r, g, b)
}
