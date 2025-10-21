package neth.iecal.questphone.app.screens.quest.view.external_integration.webview

import android.content.Context
import android.content.Intent
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.io.IOException
import neth.iecal.questphone.BuildConfig
import neth.iecal.questphone.app.screens.quest.view.external_integration.ExternalIntegrationQuestViewVM
import neth.iecal.questphone.core.utils.reminder.simpleAlarm.AlarmHelper
import nethical.questphone.data.json
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File


class WebAppInterface(private val context: Context, private val webView: WebView, private val viewQuestVM: ExternalIntegrationQuestViewVM) {

    private val client = OkHttpClient()

    @JavascriptInterface
    fun onQuestCompleted() {
        viewQuestVM.saveMarkedQuestToDb()
        Log.d("WebAppInterface", "Quest Completed")
        Toast.makeText(
            context,
            "Quest completed!",
            Toast.LENGTH_SHORT
        ).show()
    }

    @JavascriptInterface
    fun toast(msg: String) {
        Log.d("WebAppInterfaceToast",msg)
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }
    @JavascriptInterface
    fun sendBroadcast(action: String, jsonData: String?) {
        val intent = Intent(action)
        intent.putExtra("payload", jsonData ?: "{}")
        context.sendBroadcast(intent)
    }
    @JavascriptInterface
    fun getUserData(): String{
        return viewQuestVM.getUserData()
    }


    @JavascriptInterface
    fun registerReceiver(actionsJson: String) {
        val actions: List<String> = json.decodeFromString(actionsJson)
        BroadcastCenter.register(context, webView, actions)
    }

    @JavascriptInterface
    fun unregisterAll() {
        BroadcastCenter.unregister(context)
    }

    @JavascriptInterface
    fun isQuestCompleted():Boolean{
        return viewQuestVM.isQuestComplete.value
    }
    @JavascriptInterface
    fun enableFullScreen() {
        viewQuestVM.isFullScreen.value = true
    }
    @JavascriptInterface
    fun disableFullScreen(){
        viewQuestVM.isFullScreen.value = false
    }
    @JavascriptInterface
    fun getVersion(): Int {
        return BuildConfig.VERSION_CODE
    }
    
    @JavascriptInterface
    fun setAlarmedNotification(triggerMillis: Long,title:String,description: String){
        val alarmManager = AlarmHelper(context)
        alarmManager.setAlarm(triggerMillis,title,description)
    }
    @JavascriptInterface
    fun openApp(packageName: String) {
        val pm = context.packageManager
        val intent = pm.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } else {
            // App not installed, open Play Store
            val playIntent = Intent(Intent.ACTION_VIEW).apply {
                data = "https://play.google.com/store/apps/details?id=$packageName".toUri()
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(playIntent)
        }
    }
    @JavascriptInterface
    fun shareText(title: String, text: String) {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TITLE, title)
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        val chooser = Intent.createChooser(shareIntent, "Share via")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
    @JavascriptInterface
    fun shareImage(base64: String) {
        try {
            // Remove the header if present
            val base64Data = base64.substringAfter("base64,", base64)
            val bytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)

            // Save to cache directory
            val file = File(context.cacheDir, "shared_image.png")
            file.outputStream().use {
                it.write(bytes)
            }

            // Get content URI using FileProvider
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)

            // Share Intent
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, uri)
                type = "image/*"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, "Share Image via"))

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @JavascriptInterface
    fun getCoinRewardRatio():Int{
        val sp = context.getSharedPreferences("minutes_per_5", Context.MODE_PRIVATE)
        return sp.getInt("minutes_per_5",10)
    }
    @JavascriptInterface
    fun getAllStatsForQuest() {
        CoroutineScope(Dispatchers.IO).launch {
            val stats = viewQuestVM.getQuestStats {
                webView.post {
                    webView.evaluateJavascript("onStatsReceived(${JSONObject.quote(it)});", null)
                }
            }

        }
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
