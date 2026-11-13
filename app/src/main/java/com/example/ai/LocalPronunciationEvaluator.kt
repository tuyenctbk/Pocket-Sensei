package com.example.ai

import android.content.Context
import com.example.R
import kotlin.math.max
import kotlin.math.min

/**
 * Pure Kotlin, zero-API-cost on-device evaluator used when:
 * 1) User selects 100% Offline ($0) Coaching Mode,
 * 2) Daily free cloud quota is exhausted, or
 * 3) The device is offline / playing the zero-cost Dojo Blitz speaking game.
 */
object LocalPronunciationEvaluator {

    fun normalizeForComparison(text: String): String {
        return text.lowercase()
            .replace(Regex("[\\p{Punct}\\s、。！？「」『』・]+"), " ")
            .trim()
    }

    fun computeSimilarityPercent(expected: String, actual: String): Int {
        val a = normalizeForComparison(expected).replace(" ", "")
        val b = normalizeForComparison(actual).replace(" ", "")
        if (a.isEmpty() || b.isEmpty()) return 0
        if (a == b) return 98

        val dist = levenshteinDistance(a, b)
        val maxLen = max(a.length, b.length)
        val ratio = 1.0 - (dist.toDouble() / maxLen.toDouble())
        return (ratio * 100.0).toInt().coerceIn(25, 97)
    }

    fun evaluate(
        targetLanguage: String,
        targetPhrase: String,
        spokenTranscript: String,
        measuredPitchContour: List<Int>? = null,
        context: Context? = null
    ): PronunciationResult {
        val similarity = computeSimilarityPercent(targetPhrase, spokenTranscript)
        val expectedWords = normalizeForComparison(targetPhrase).split(" ").filter { it.isNotBlank() }
        val actualNorm = normalizeForComparison(spokenTranscript)

        val missed = expectedWords.filter { token ->
            token.length > 1 && !actualNorm.contains(token)
        }.take(4)

        val intonationScore = when {
            similarity >= 90 -> 91
            similarity >= 75 -> 82
            else -> (similarity + 5).coerceIn(40, 85)
        }
        val fluencyScore = when {
            actualNorm.length >= normalizeForComparison(targetPhrase).length * 0.8 -> (similarity + 3).coerceIn(50, 98)
            else -> (similarity - 4).coerceIn(35, 90)
        }

        val targetContour = when {
            targetLanguage.equals("Japanese", ignoreCase = true) -> listOf(50, 82, 68, 42)
            targetLanguage.equals("Mandarin", ignoreCase = true) -> listOf(78, 45, 84, 52)
            else -> listOf(52, 74, 64, 48)
        }
        val learnerContour = measuredPitchContour?.takeIf { it.size >= 3 } ?: if (similarity >= 85) {
            targetContour.map { (it - 3).coerceIn(20, 90) }
        } else {
            targetContour.mapIndexed { idx, pt ->
                val shift = if (idx % 2 == 0) -12 else 10
                (pt + shift).coerceIn(20, 90)
            }
        }

        val tip = when {
            similarity >= 90 -> context?.getString(R.string.eval_tip_clean)
                ?: "Clean vowel length and crisp consonant onset! Keep this relaxed pace."
            missed.isNotEmpty() -> {
                val joined = missed.joinToString(", ")
                context?.getString(R.string.eval_tip_missed_format, joined)
                    ?: "Focus clearly on enunciating: $joined. Keep your jaw steady on stressed vowels."
            }
            else -> context?.getString(R.string.eval_tip_slow_down)
                ?: "Slow down slightly between syllables and project stressed vowels cleanly."
        }

        val encouragement = when {
            similarity >= 90 -> context?.getString(R.string.eval_encourage_high)
                ?: "Sugoi! Evaluated on-device ($0 offline mode)."
            similarity >= 75 -> context?.getString(R.string.eval_encourage_mid)
                ?: "Good rhythm! A second slower pass will sharpen your accent."
            else -> context?.getString(R.string.eval_encourage_low)
                ?: "Nice try — tap 'Slow' audio to hear every syllable clearly."
        }

        val contourType = context?.getString(R.string.eval_contour_type_format, targetLanguage)
            ?: "$targetLanguage On-Device Contour"
        val pitchGuidance = context?.getString(R.string.eval_pitch_guidance_default)
            ?: "Match the dashed green native contour rise and fall cleanly."

        return PronunciationResult(
            score = similarity,
            phoneticAccuracy = similarity,
            intonationPitch = intonationScore,
            fluency = fluencyScore,
            phoneticBreakdown = targetPhrase,
            mispronouncedParts = missed,
            coachingTip = tip,
            nativePhrasings = listOf(targetPhrase),
            encouragement = encouragement,
            pitchContourType = contourType,
            targetPitchPoints = targetContour,
            learnerPitchPoints = learnerContour,
            pitchGuidance = pitchGuidance,
            hasMeasuredAudioContour = measuredPitchContour != null && measuredPitchContour.size >= 3
        )
    }

    fun conductLocalRoleplayTurn(
        language: String,
        scenarioTitle: String,
        userSpokenMessage: String,
        context: Context? = null
    ): RoleplayTurn {
        val (reply, phonetic, translation, suggestions) = when (language.lowercase()) {
            "japanese" -> Quad(
                "かしこまりました！他にご注文はございますか？",
                "Kashikomarimashita! Hoka ni go-chuumon wa gozaimasu ka?",
                context?.getString(R.string.roleplay_offline_reply_ja_translation)
                    ?: "Certainly! Would you like anything else?",
                listOf("大丈夫です、ありがとう。", "お会計をお願いします。", "おすすめのスイーツはありますか？")
            )
            "french" -> Quad(
                "Très bien ! Souhaitez-vous autre chose avec ceci ?",
                "Treh byahn ! Soo-eh-tay-voo oh-truh shohz ?",
                context?.getString(R.string.roleplay_offline_reply_fr_translation)
                    ?: "Very well! Would you like anything else with that?",
                listOf("Non merci, l'addition s'il vous plaît.", "Oui, un café crème !", "C'est tout pour aujourd'hui.")
            )
            "spanish" -> Quad(
                "¡Perfecto! Enseguida se lo traigo. ¿Algo más para tomar?",
                "Pehr-FEHK-toh! Ehn-seh-GEE-dah seh loh TRAI-goh.",
                context?.getString(R.string.roleplay_offline_reply_es_translation)
                    ?: "Perfect! I'll bring that right out. Anything else to drink?",
                listOf("No, muchas gracias.", "La cuenta, por favor.", "¿Qué postre recomienda?")
            )
            "korean" -> Quad(
                "네, 알겠습니다! 잠시만 기다려 주세요.",
                "Ne, al-get-seum-ni-da! Jam-si-man gi-da-ryeo ju-se-yo.",
                context?.getString(R.string.roleplay_offline_reply_ko_translation)
                    ?: "Yes, understood! Please wait just a moment.",
                listOf("네, 감사합니다!", "물 한 잔 주세요.", "얼마예요?")
            )
            "german" -> Quad(
                "Ausgezeichnet! Möchten Sie mir mehr darüber erzählen?",
                "OWS-geh-tsykh-net! MÖKH-ten zee meer mayr dah-RÜ-ber er-TSAY-len?",
                context?.getString(R.string.roleplay_offline_reply_de_translation)
                    ?: "Excellent! Would you like to tell me more about that?",
                listOf("Ja, sehr gerne!", "Können Sie das wiederholen?", "Vielen Dank!")
            )
            "italian" -> Quad(
                "Perfetto! Desidera qualcos'altro oggi?",
                "Pehr-FEHT-toh! Deh-zee-DEH-rah kwahl-koh-ZAHL-troh OHJ-jee?",
                context?.getString(R.string.roleplay_offline_reply_it_translation)
                    ?: "Perfect! Would you like anything else today?",
                listOf("Sì, grazie!", "Il conto, per favore.", "Cosa mi consiglia?")
            )
            else -> Quad(
                context?.getString(R.string.roleplay_offline_reply_default, scenarioTitle)
                    ?: "Wonderful! Let's keep practicing your natural cadence in $scenarioTitle.",
                userSpokenMessage,
                context?.getString(R.string.roleplay_offline_translation_fallback)
                    ?: "Offline Practice Mode response",
                listOf("Could you repeat that slowly?", "What do you recommend?", "Thank you very much!")
            )
        }

        val note = context?.getString(R.string.roleplay_offline_note_format, userSpokenMessage.take(40))
            ?: "Offline Coach ($0 mode): Received \"${userSpokenMessage.take(40)}\" smoothly. Keep your pacing natural!"

        return RoleplayTurn(
            senseiReply = reply,
            phoneticGuide = phonetic,
            englishTranslation = translation,
            accentAndGrammarNote = note,
            suggestedUserReplies = suggestions,
            senseiMood = "praising"
        )
    }

    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    private fun levenshteinDistance(s: String, t: String): Int {
        val m = s.length
        val n = t.length
        val dp = IntArray(n + 1) { it }
        for (i in 1..m) {
            var prev = dp[0]
            dp[0] = i
            for (j in 1..n) {
                val temp = dp[j]
                val cost = if (s[i - 1] == t[j - 1]) 0 else 1
                dp[j] = min(min(dp[j] + 1, dp[j - 1] + 1), prev + cost)
                prev = temp
            }
        }
        return dp[n]
    }
}
