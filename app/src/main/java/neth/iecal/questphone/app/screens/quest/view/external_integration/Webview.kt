package neth.iecal.questphone.app.screens.quest.view.external_integration

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.io.IOException
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebView() {
    val colors = MaterialTheme.colorScheme // use MaterialTheme.colors for M2
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->

        WebView(context).apply {
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
            addJavascriptInterface(WebAppInterface(context, this), "WebAppInterface")


            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)

                    // Page is fully loaded, now you can run JS
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
                }
            }

            // so links open inside WebView
            loadUrl("http://192.168.31.15:8000/test-test.html")
        }

    })
}


class WebAppInterface(private val context: Context, private val webView: WebView) {

    private val client = OkHttpClient()

    // --- Example method to mark quest as complete ---
    @JavascriptInterface
    fun markQuestAsComplete(questId: String) {
        // Your app logic here
        // Toast or store quest completion
        android.widget.Toast.makeText(context, "Quest $questId completed!", android.widget.Toast.LENGTH_SHORT).show()
    }

    // --- Async fetch method with headers injected from JS ---
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
