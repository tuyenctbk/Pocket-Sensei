package com.example.analytics

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.example.R
import java.io.File
import java.io.FileOutputStream

private const val TAG = "MilestoneShareCard"

/**
 * Generates an on-device PNG achievement card for sharing pronunciation scores or daily streaks.
 * Writes strictly into app cacheDir/shares/ and exposes via FileProvider (zero storage permissions).
 */
object MilestoneShareCardGenerator {

    fun generateCardFile(
        context: Context,
        headline: String,
        phraseText: String,
        phoneticText: String,
        score: Int?,
        streakDays: Int
    ): File {
        val width = 1080
        val height = 620
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Rich indigo -> violet card backdrop
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                intArrayOf(Color.parseColor("#0F0E26"), Color.parseColor("#1E1B4B"), Color.parseColor("#312E81")),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(RectF(0f, 0f, width.toFloat(), height.toFloat()), 36f, 36f, bgPaint)

        // Subtle outer border stroke
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4f
            color = Color.parseColor("#4F46E5")
        }
        canvas.drawRoundRect(RectF(8f, 8f, width - 8f, height - 8f), 32f, 32f, borderPaint)

        // Decorative glowing orb behind right gauge
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#2234D399")
        }
        canvas.drawCircle(width - 190f, 300f, 155f, glowPaint)

        // Top brand pill
        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#34D399")
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(context.getString(R.string.app_name), 64f, 86f, brandPaint)

        // Headline
        val headlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#A5B4FC")
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(headline.take(40), 64f, 150f, headlinePaint)

        // Target Phrase (large focal typography)
        val phrasePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 48f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val clippedPhrase = if (phraseText.length > 26) phraseText.take(25) + "…" else phraseText
        canvas.drawText(clippedPhrase, 64f, 250f, phrasePaint)

        if (phoneticText.isNotBlank()) {
            val phoneticPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#C7D2FE")
                textSize = 30f
            }
            val clippedPhonetic = if (phoneticText.length > 38) phoneticText.take(37) + "…" else phoneticText
            canvas.drawText(clippedPhonetic, 64f, 305f, phoneticPaint)
        }

        // Streak pill at bottom left
        val pillBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#431407")
        }
        val pillBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = Color.parseColor("#F97316")
        }
        val streakRect = RectF(64f, 440f, 390f, 520f)
        canvas.drawRoundRect(streakRect, 40f, 40f, pillBg)
        canvas.drawRoundRect(streakRect, 40f, 40f, pillBorder)

        val streakTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FDBA74")
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val streakLabel = context.getString(R.string.share_card_streak_format, streakDays.coerceAtLeast(0))
        canvas.drawText("🔥 $streakLabel", 92f, 491f, streakTextPaint)

        // Score Arc Gauge on Right (if score present, else Streak Star Gauge)
        val cx = width - 190f
        val cy = 300f
        val radius = 110f
        val arcRect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 22f
            color = Color.parseColor("#312E81")
        }
        canvas.drawArc(arcRect, -90f, 360f, false, trackPaint)

        val pct = (score ?: (streakDays * 10).coerceIn(25, 100)).coerceIn(0, 100)
        val arcColor = when {
            score == null -> Color.parseColor("#F97316")
            score >= 85 -> Color.parseColor("#34D399")
            score >= 70 -> Color.parseColor("#FBBF24")
            else -> Color.parseColor("#818CF8")
        }
        val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 22f
            strokeCap = Paint.Cap.ROUND
            color = arcColor
        }
        canvas.drawArc(arcRect, -90f, (pct / 100f) * 360f, false, progressPaint)

        val bigValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 54f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val displayVal = if (score != null) {
            context.getString(R.string.percent_format, pct)
        } else {
            context.getString(R.string.streak_day_format, streakDays)
        }
        canvas.drawText(displayVal, cx, cy + 18f, bigValuePaint)

        val sharesDir = File(context.cacheDir, "shares").apply { mkdirs() }
        val outFile = File(sharesDir, "sensei_milestone.png")
        FileOutputStream(outFile).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 95, stream)
            stream.flush()
        }
        bitmap.recycle()
        return outFile
    }

    fun generateAndGetUri(
        context: Context,
        headline: String,
        phraseText: String,
        phoneticText: String,
        score: Int?,
        streakDays: Int
    ): Uri? {
        return try {
            val file = generateCardFile(context, headline, phraseText, phoneticText, score, streakDays)
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            Log.w(TAG, "Unable to generate or resolve share card URI", e)
            null
        }
    }
}
