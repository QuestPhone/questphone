package neth.iecal.questphone.app.screens.quest.view.external_integration.webview

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.webkit.WebView
import nethical.questphone.data.json

object BroadcastCenter {
    private var receiver: BroadcastReceiver? = null

    fun register(context: Context, webView: WebView, actions: List<String>) {
        unregister(context)

        val filter = IntentFilter()
        actions.forEach { filter.addAction(it) }

        receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val action = intent?.action ?: return
                val data = intent.getStringExtra("payload") ?: "{}"

                // send to JS
                val jsCode = "window.onBroadcast && window.onBroadcast(${json.encodeToString(action)}, ${data})"
                webView.post { webView.evaluateJavascript(jsCode, null) }
            }
        }
        context.registerReceiver(receiver, filter)
    }

    fun unregister(context: Context) {
        receiver?.let {
            context.unregisterReceiver(it)
            receiver = null
        }
    }
}
