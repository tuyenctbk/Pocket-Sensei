package com.example.ui.view

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

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

    private val _mood = MutableStateFlow("idle")
    val mood: StateFlow<String> = _mood.asStateFlow()

    private val _moodLabel = MutableStateFlow(context.getString(com.example.R.string.mood_idle))
    val moodLabel: StateFlow<String> = _moodLabel.asStateFlow()

    private val _costume = MutableStateFlow("traditional")
    val costume: StateFlow<String> = _costume.asStateFlow()

    private val _speakingUntilMs = MutableStateFlow(0L)
    val speakingUntilMs: StateFlow<Long> = _speakingUntilMs.asStateFlow()

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
            if (pendingScripts.size > 24) {
                pendingScripts.poll()
            }
            pendingScripts.offer(script)
        }
    }

    fun setMood(mood: String, localizedLabel: String? = null) {
        _mood.value = mood
        if (!localizedLabel.isNullOrBlank()) {
            _moodLabel.value = localizedLabel
        }
        val quotedMood = JSONObject.quote(mood)
        if (localizedLabel != null) {
            val quotedLabel = JSONObject.quote(localizedLabel)
            enqueueOrRun("window.Sensei && window.Sensei.setMood($quotedMood, $quotedLabel)")
        } else {
            enqueueOrRun("window.Sensei && window.Sensei.setMood($quotedMood)")
        }
    }

    fun speak(durationMs: Long, text: String) {
        _mood.value = "speaking"
        _moodLabel.value = context.getString(com.example.R.string.mood_speaking)
        _speakingUntilMs.value = System.currentTimeMillis() + durationMs.coerceAtLeast(600L)
        val quoted = JSONObject.quote(text.replace("\n", " "))
        val speakingLabel = JSONObject.quote(context.getString(com.example.R.string.mood_speaking))
        enqueueOrRun("window.Sensei && window.Sensei.speak($durationMs, $quoted, $speakingLabel)")
    }

    fun praise() {
        _mood.value = "praising"
        _moodLabel.value = context.getString(com.example.R.string.mood_praising)
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
        _costume.value = costume
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
    val mood by controller.mood.collectAsState()
    val moodLabel by controller.moodLabel.collectAsState()
    val costume by controller.costume.collectAsState()
    val speakingUntilMs by controller.speakingUntilMs.collectAsState()

    var isSpeakingNow by remember { mutableStateOf(false) }
    LaunchedEffect(speakingUntilMs) {
        val remaining = speakingUntilMs - System.currentTimeMillis()
        if (remaining > 0L) {
            isSpeakingNow = true
            delay(remaining)
        }
        isSpeakingNow = false
    }

    val webInterface = remember {
        SenseiWebInterface {
            controller.markReady()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Always-on 60fps Compose Sensei Avatar (guarantees crisp rendering even if WebView WebGL stalls)
        SenseiNativeStage(
            mood = mood,
            moodLabel = moodLabel,
            costume = costume,
            isSpeaking = isSpeakingNow,
            modifier = Modifier.fillMaxSize()
        )

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
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
    }

    DisposableEffect(Unit) {
        onDispose {
            controller.webView?.stopLoading()
            controller.webView = null
        }
    }
}

@Composable
private fun SenseiNativeStage(
    mood: String,
    moodLabel: String,
    costume: String,
    isSpeaking: Boolean,
    modifier: Modifier = Modifier
) {
    val giTargetColor = when (costume) {
        "crimson" -> Color(0xFF991B1B)
        "cyber" -> Color(0xFF0F172A)
        "sakura" -> Color(0xFFBE185D)
        else -> Color(0xFF1E3A8A)
    }
    val accentTargetColor = when (costume) {
        "crimson" -> Color(0xFFF87171)
        "cyber" -> Color(0xFF22D3EE)
        "sakura" -> Color(0xFFF472B6)
        else -> Color(0xFF818CF8)
    }
    val obiTargetColor = when (costume) {
        "crimson" -> Color(0xFFFBBF24)
        "cyber" -> Color(0xFF22D3EE)
        "sakura" -> Color(0xFFFDE68A)
        else -> Color(0xFFF59E0B)
    }

    val giColor by animateColorAsState(giTargetColor, tween(320), label = "giColor")
    val accentColor by animateColorAsState(accentTargetColor, tween(320), label = "accentColor")
    val obiColor by animateColorAsState(obiTargetColor, tween(320), label = "obiColor")

    val infiniteTransition = rememberInfiniteTransition(label = "senseiStageAnim")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "senseiPhase"
    )
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "senseiPulse"
    )

    Box(
        modifier = modifier.background(
            Brush.radialGradient(
                colors = listOf(Color(0xFF1E1B4B), Color(0xFF0F0E26))
            )
        )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h * 0.54f
            val s = (minOf(w, h) / 210f).coerceIn(0.65f, 2.4f)

            // Ambient aura glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accentColor.copy(alpha = 0.28f), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = 115f * s
                ),
                radius = 115f * s,
                center = Offset(cx, cy)
            )

            // Visualizer rings
            val ringAmp = if (isSpeaking) 7f else if (mood == "listening") 5f else 2.5f
            drawCircle(
                color = accentColor.copy(alpha = 0.35f + pulse * 0.2f),
                radius = (68f + pulse * ringAmp) * s,
                center = Offset(cx, cy + 4f * s),
                style = Stroke(width = 2.2f * s)
            )
            drawCircle(
                color = Color(0xFFF472B6).copy(alpha = 0.25f + (1f - pulse) * 0.15f),
                radius = (86f + (1f - pulse) * ringAmp) * s,
                center = Offset(cx, cy + 4f * s),
                style = Stroke(width = 1.6f * s)
            )

            // Floating dojo particles
            for (i in 0 until 8) {
                val angle = phase + i * (PI.toFloat() / 4f)
                val px = cx + cos(angle) * (92f + (i % 3) * 14f) * s
                val py = cy + sin(angle * 0.8f) * 46f * s - (pulse * 8f * s)
                drawCircle(
                    color = if (i % 2 == 0) accentColor.copy(alpha = 0.5f) else Color(0xFFFBBF24).copy(alpha = 0.45f),
                    radius = (2.2f + (i % 2)) * s,
                    center = Offset(px, py)
                )
            }

            val bobY = sin(phase * 2f) * 3.5f * s
            val headCy = cy - 12f * s + bobY

            // Gi / Shoulders
            drawRoundRect(
                color = giColor,
                topLeft = Offset(cx - 34f * s, cy + 20f * s + bobY),
                size = Size(68f * s, 46f * s),
                cornerRadius = CornerRadius(18f * s, 18f * s)
            )
            // Gi collar V
            val collarPath = Path().apply {
                moveTo(cx - 12f * s, cy + 20f * s + bobY)
                lineTo(cx, cy + 38f * s + bobY)
                lineTo(cx + 12f * s, cy + 20f * s + bobY)
            }
            drawPath(collarPath, color = Color(0xFFF8FAFC), style = Stroke(width = 2.8f * s))

            // Obi belt
            drawRoundRect(
                color = obiColor,
                topLeft = Offset(cx - 34f * s, cy + 44f * s + bobY),
                size = Size(68f * s, 9f * s),
                cornerRadius = CornerRadius(3f * s, 3f * s)
            )

            // Hair bun
            drawCircle(
                color = Color(0xFFE2E8F0),
                radius = 14f * s,
                center = Offset(cx, headCy - 40f * s)
            )

            // Head
            drawCircle(
                color = Color(0xFFFED7AA),
                radius = 35f * s,
                center = Offset(cx, headCy)
            )

            // Headband
            drawRoundRect(
                color = Color(0xFFF8FAFC),
                topLeft = Offset(cx - 35f * s, headCy - 24f * s),
                size = Size(70f * s, 12f * s),
                cornerRadius = CornerRadius(6f * s, 6f * s)
            )
            // Gold emblem on headband
            drawCircle(
                color = Color(0xFFF59E0B),
                radius = 4.2f * s,
                center = Offset(cx, headCy - 18f * s)
            )

            // Rosy cheeks
            drawCircle(
                color = Color(0xFFFB7185).copy(alpha = 0.35f),
                radius = 5.5f * s,
                center = Offset(cx - 21f * s, headCy + 5f * s)
            )
            drawCircle(
                color = Color(0xFFFB7185).copy(alpha = 0.35f),
                radius = 5.5f * s,
                center = Offset(cx + 21f * s, headCy + 5f * s)
            )

            // Glasses
            val eyeY = headCy - 2f * s
            drawCircle(
                color = Color(0xFFF59E0B),
                radius = 10f * s,
                center = Offset(cx - 13f * s, eyeY),
                style = Stroke(width = 2.2f * s)
            )
            drawCircle(
                color = Color(0xFFF59E0B),
                radius = 10f * s,
                center = Offset(cx + 13f * s, eyeY),
                style = Stroke(width = 2.2f * s)
            )
            drawLine(
                color = Color(0xFFF59E0B),
                start = Offset(cx - 3f * s, eyeY),
                end = Offset(cx + 3f * s, eyeY),
                strokeWidth = 2.2f * s
            )

            // Eyes (blink periodically)
            val isBlinking = (phase > 5.95f)
            if (isBlinking) {
                drawLine(Color(0xFF1E1B4B), Offset(cx - 17f * s, eyeY), Offset(cx - 9f * s, eyeY), 2.5f * s)
                drawLine(Color(0xFF1E1B4B), Offset(cx + 9f * s, eyeY), Offset(cx + 17f * s, eyeY), 2.5f * s)
            } else {
                drawCircle(Color(0xFF1E1B4B), 3.6f * s, Offset(cx - 13f * s, eyeY))
                drawCircle(Color(0xFF1E1B4B), 3.6f * s, Offset(cx + 13f * s, eyeY))
                drawCircle(Color.White, 1.3f * s, Offset(cx - 14f * s, eyeY - 1.2f * s))
                drawCircle(Color.White, 1.3f * s, Offset(cx + 12f * s, eyeY - 1.2f * s))
            }

            // Beard
            val beardPath = Path().apply {
                moveTo(cx - 22f * s, headCy + 14f * s)
                quadraticTo(cx, headCy + 46f * s, cx + 22f * s, headCy + 14f * s)
                close()
            }
            drawPath(beardPath, color = Color(0xFFE2E8F0))

            // Mouth (lip-sync when speaking)
            val mouthOpen = if (isSpeaking) (4f + abs(sin(phase * 10f)) * 6.5f) * s else 2.5f * s
            drawOval(
                color = Color(0xFF991B1B),
                topLeft = Offset(cx - 6.5f * s, headCy + 17f * s - mouthOpen / 2f),
                size = Size(13f * s, mouthOpen)
            )
        }

        // Mood HUD Pill (top-start)
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1E1B4B).copy(alpha = 0.8f))
                .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(16.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(
                        when (mood) {
                            "speaking" -> Color(0xFF34D399)
                            "listening" -> Color(0xFFF472B6)
                            "praising" -> Color(0xFFFBBF24)
                            else -> accentColor
                        }
                    )
            )
            Text(
                text = moodLabel.uppercase(),
                color = Color(0xFFA5B4FC),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            )
        }
    }
}
