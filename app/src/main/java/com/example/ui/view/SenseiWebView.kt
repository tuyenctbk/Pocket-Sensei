package com.example.ui.view

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONObject
import java.util.concurrent.ConcurrentLinkedQueue

class SenseiWebInterface(private val onReady: () -> Unit) {
    @JavascriptInterface
    fun onSceneReady() {
        onReady()
    }
}

class SenseiViewController(private val context: Context) {
    @Volatile
    var webView: WebView? = null

    @Volatile
    private var isReady = false

    private val pendingScripts = ConcurrentLinkedQueue<String>()

    fun attachWebView(view: WebView) {
        this.webView = view
    }

    fun markReady() {
        isReady = true
        val readyLabel = JSONObject.quote(context.getString(com.example.R.string.mood_idle))
        val speakingLabel = JSONObject.quote(context.getString(com.example.R.string.mood_speaking))
        val view = webView ?: return
        view.post {
            view.evaluateJavascript(
                "window.Sensei && window.Sensei.setDefaultLabels && window.Sensei.setDefaultLabels($readyLabel, $speakingLabel)",
                null
            )
            while (true) {
                val script = pendingScripts.poll() ?: break
                view.evaluateJavascript(script, null)
            }
        }
    }

    fun enqueueOrRun(script: String) {
        val view = webView
        if (isReady && view != null) {
            view.post {
                view.evaluateJavascript(script, null)
            }
        } else {
            // Bound queue size to avoid unbounded buildup before page loads
            if (pendingScripts.size > 24) {
                pendingScripts.poll()
            }
            pendingScripts.offer(script)
        }
    }

    fun setMood(mood: String, localizedLabel: String? = null) {
        val quotedMood = JSONObject.quote(mood)
        if (localizedLabel != null) {
            val quotedLabel = JSONObject.quote(localizedLabel)
            enqueueOrRun("window.Sensei && window.Sensei.setMood($quotedMood, $quotedLabel)")
        } else {
            enqueueOrRun("window.Sensei && window.Sensei.setMood($quotedMood)")
        }
    }

    fun speak(durationMs: Long, text: String) {
        val quoted = JSONObject.quote(text.replace("\n", " "))
        val speakingLabel = JSONObject.quote(context.getString(com.example.R.string.mood_speaking))
        enqueueOrRun("window.Sensei && window.Sensei.speak($durationMs, $quoted, $speakingLabel)")
    }

    fun praise() {
        enqueueOrRun("window.Sensei && window.Sensei.praise()")
    }

    fun playChime(kind: String = "success") {
        val quoted = JSONObject.quote(kind)
        enqueueOrRun("window.Sensei && window.Sensei.playChime && window.Sensei.playChime($quoted)")
    }

    fun setMouthOpen(value: Float) {
        val clamped = value.coerceIn(0f, 1f)
        enqueueOrRun("window.Sensei && window.Sensei.setMouthOpen($clamped)")
    }

    fun setCostume(costume: String) {
        val quoted = JSONObject.quote(costume)
        enqueueOrRun("window.Sensei && window.Sensei.setCostume($quoted)")
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun Sensei3DView(
    controller: SenseiViewController,
    modifier: Modifier = Modifier
) {
    val webInterface = remember {
        SenseiWebInterface {
            controller.markReady()
        }
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            WebView(ctx).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    allowFileAccess = true
                    allowContentAccess = true
                    cacheMode = WebSettings.LOAD_NO_CACHE
                }
                webChromeClient = WebChromeClient()
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        controller.markReady()
                    }
                }
                addJavascriptInterface(webInterface, "SenseiBridge")
                loadUrl("file:///android_asset/sensei_3d.html")
                controller.attachWebView(this)
            }
        },
        update = { webView ->
            controller.attachWebView(webView)
        }
    )

    DisposableEffect(Unit) {
        onDispose {
            controller.webView?.stopLoading()
            controller.webView = null
        }
    }
}
