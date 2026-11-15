package com.example.ai

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.local.CachedEvaluationDao
import com.example.data.local.CachedEvaluationEntity
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit

private const val TAG = "AiSenseiCoach"

data class PronunciationResult(
    val score: Int,
    val phoneticAccuracy: Int,
    val intonationPitch: Int,
    val fluency: Int,
    val phoneticBreakdown: String,
    val mispronouncedParts: List<String>,
    val coachingTip: String,
    val nativePhrasings: List<String>,
    val encouragement: String,
    val pitchContourType: String = "Natural Accent Contour",
    val targetPitchPoints: List<Int> = listOf(55, 80, 65, 40),
    val learnerPitchPoints: List<Int> = listOf(50, 70, 60, 45),
    val pitchGuidance: String = "Slightly elevate pitch on key stressed vowels.",
    val engineSource: String = "Cloud AI",
    val hasMeasuredAudioContour: Boolean = false
)

data class RoleplayTurn(
    val senseiReply: String,
    val phoneticGuide: String,
    val englishTranslation: String,
    val accentAndGrammarNote: String,
    val suggestedUserReplies: List<String>,
    val senseiMood: String
)

data class GeneratedTopicPhrase(
    val targetText: String,
    val phonetic: String,
    val translation: String,
    val accentFocus: String
)

data class GeneratedTopicPack(
    val topic: String,
    val language: String,
    val phrases: List<GeneratedTopicPhrase>,
    val scenarioTitle: String,
    val senseiRole: String,
    val userRole: String,
    val openingLine: String,
    val usedByokCloud: Boolean
)

class AiSenseiCoach(
    private val cacheDao: CachedEvaluationDao? = null,
    private val aiSettingsManager: AiSettingsManager? = null,
    private val appContext: Context? = null
) {

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .build()
    }

    // Schema for Pronunciation Evaluation
    private val pronunciationSchema = Schema.obj(
        properties = mapOf(
            "score" to Schema.integer(description = "Overall pronunciation accuracy score from 0 to 100"),
            "phoneticAccuracy" to Schema.integer(description = "Phonetic accuracy score 0 to 100"),
            "intonationPitch" to Schema.integer(description = "Pitch accent or tonal accuracy score 0 to 100"),
            "fluency" to Schema.integer(description = "Speaking rhythm and fluency score 0 to 100"),
            "phoneticBreakdown" to Schema.string(description = "IPA, romaji, pinyin or syllable breakdown with stress markers"),
            "mispronouncedParts" to Schema.array(items = Schema.string(), description = "List of specific phonemes or syllables needing adjustment"),
            "coachingTip" to Schema.string(description = "Actionable physical mouth and tongue placement instructions"),
            "nativePhrasings" to Schema.array(items = Schema.string(), description = "1-2 natural native alternatives"),
            "encouragement" to Schema.string(description = "Warm teacher feedback from Pocket Sensei"),
            "pitchContourType" to Schema.string(description = "Pitch contour label, e.g. High-Low (Atamadaka), Rising, Falling, or Level"),
            "targetPitchPoints" to Schema.array(items = Schema.integer(), description = "4 normalized pitch keypoints (20 to 90) for native curve"),
            "learnerPitchPoints" to Schema.array(items = Schema.integer(), description = "4 normalized pitch keypoints (20 to 90) for user curve"),
            "pitchGuidance" to Schema.string(description = "Actionable guidance on intonation melody and pitch contour")
        ),
        optionalProperties = listOf("mispronouncedParts", "nativePhrasings", "pitchContourType", "targetPitchPoints", "learnerPitchPoints", "pitchGuidance")
    )

    // Schema for Conversational Roleplay
    private val roleplaySchema = Schema.obj(
        properties = mapOf(
            "senseiReply" to Schema.string(description = "Sensei's dialogue response in the target language"),
            "phoneticGuide" to Schema.string(description = "Pronunciation guide (Romaji, Pinyin, or phonetic transcript)"),
            "englishTranslation" to Schema.string(description = "English translation of Sensei's dialogue"),
            "accentAndGrammarNote" to Schema.string(description = "Accent corrections and feedback on what the user just said"),
            "suggestedUserReplies" to Schema.array(items = Schema.string(), description = "2 to 3 practical reply suggestions in target language"),
            "senseiMood" to Schema.string(description = "Sensei mood: praising, speaking, correcting, or thinking")
        )
    )

    // Schema for AI Topic Lesson Pack
    private val topicPackSchema = Schema.obj(
        properties = mapOf(
            "phrases" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "targetText" to Schema.string(description = "Phrase in target language"),
                        "phonetic" to Schema.string(description = "Romaji, Pinyin, IPA, or phonetic pronunciation guide"),
                        "translation" to Schema.string(description = "Natural translation in the learner's UI language"),
                        "accentFocus" to Schema.string(description = "Brief pronunciation focus tip")
                    )
                ),
                description = "3 practical speaking phrases for this topic"
            ),
            "scenarioTitle" to Schema.string(description = "Short title for a roleplay on this topic"),
            "senseiRole" to Schema.string(description = "Sensei's character role in the roleplay"),
            "userRole" to Schema.string(description = "Learner's character role in the roleplay"),
            "openingLine" to Schema.string(description = "Sensei's first dialogue line in target language")
        )
    )

    private val primaryPronunciationModel by lazy {
        Firebase.ai.generativeModel(
            modelName = AiModelConfig.PRIMARY_REASONING_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = pronunciationSchema
            }
        )
    }

    private val economyPronunciationModel by lazy {
        Firebase.ai.generativeModel(
            modelName = AiModelConfig.ECONOMY_LITE_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = pronunciationSchema
            }
        )
    }

    private val primaryRoleplayModel by lazy {
        Firebase.ai.generativeModel(
            modelName = AiModelConfig.PRIMARY_REASONING_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = roleplaySchema
            }
        )
    }

    private val economyRoleplayModel by lazy {
        Firebase.ai.generativeModel(
            modelName = AiModelConfig.ECONOMY_LITE_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = roleplaySchema
            }
        )
    }

    private val primaryTopicPackModel by lazy {
        Firebase.ai.generativeModel(
            modelName = AiModelConfig.PRIMARY_REASONING_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = topicPackSchema
            }
        )
    }

    private val economyTopicPackModel by lazy {
        Firebase.ai.generativeModel(
            modelName = AiModelConfig.ECONOMY_LITE_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = topicPackSchema
            }
        )
    }

    suspend fun evaluatePronunciation(
        targetLanguage: String,
        promptOrWord: String,
        spokenTranscript: String,
        measuredPitchContour: List<Int>? = null
    ): Result<PronunciationResult> = withContext(Dispatchers.IO) {
        val validContour = measuredPitchContour?.takeIf { it.size >= 3 }
        val cacheKey = buildCacheKey(targetLanguage, promptOrWord, spokenTranscript)

        // 1. Instant $0 Room cache lookup for repeat practice attempts (30-45s deduplication)
        cacheDao?.findByKey(cacheKey)?.let { cached ->
            val domain = cached.toDomain(engineSource = "Room Cache ($0)")
            return@withContext Result.success(applyMeasuredContour(domain, validContour))
        }

        val mode = aiSettingsManager?.getCurrentMode() ?: AiCoachingMode.HYBRID_FREE

        // 2. Explicit 100% Offline ($0) Mode
        if (mode == AiCoachingMode.LOCAL_OFFLINE_ONLY) {
            val local = LocalPronunciationEvaluator.evaluate(
                targetLanguage = targetLanguage,
                targetPhrase = promptOrWord,
                spokenTranscript = spokenTranscript,
                measuredPitchContour = validContour,
                context = appContext
            ).copy(
                engineSource = "Offline Coach ($0)",
                hasMeasuredAudioContour = validContour != null
            )
            return@withContext Result.success(local)
        }

        val responseLang = Locale.getDefault().displayLanguage.ifBlank { "English" }
        val prompt = """
            You are 'Pocket Sensei', a world-class 3D interactive language and accent coach.
            The student is learning $targetLanguage.
            The intended phrase/word is: "$promptOrWord".
            The student spoke and this was transcribed as: "$spokenTranscript".

            Analyze the pronunciation accuracy, phonetic precision, pitch accent/tonal contour, and fluency.
            Provide concrete, encouraging feedback with specific tongue and lip placement tips. Write coachingTip, encouragement, pitchContourType, and pitchGuidance in $responseLang.
            Respond as raw JSON matching keys: score, phoneticAccuracy, intonationPitch, fluency, phoneticBreakdown, mispronouncedParts, coachingTip, nativePhrasings, encouragement, pitchContourType, targetPitchPoints (4 ints 20..90), learnerPitchPoints (4 ints 20..90), pitchGuidance.
        """.trimIndent()

        // 3. User-funded BYOK Gemini Key path (zero developer liability; never leaks into Hybrid quota)
        if (mode == AiCoachingMode.BYOK_USER_KEY) {
            val userKey = aiSettingsManager?.getUserApiKey().orEmpty()
            if (userKey.isNotBlank()) {
                val byokResult = runCatching {
                    val jsonText = callByokGeminiJson(userKey, prompt)
                    parsePronunciationJson(jsonText, promptOrWord, engineSource = "Your Gemini Key (BYOK)")
                }
                byokResult.onSuccess { eval ->
                    cacheDao?.insertOrReplace(eval.toEntity(cacheKey, targetLanguage, promptOrWord, spokenTranscript))
                    return@withContext Result.success(applyMeasuredContour(eval, validContour))
                }
                byokResult.onFailure { e ->
                    if (e is CancellationException && e !is TimeoutCancellationException) throw e
                    Log.w(TAG, "BYOK pronunciation evaluation failed, falling back to Local Sensei", e)
                }
            }
            aiSettingsManager?.usageQuotaManager?.recordFallbackCascade()
            val byokFallback = LocalPronunciationEvaluator.evaluate(
                targetLanguage = targetLanguage,
                targetPhrase = promptOrWord,
                spokenTranscript = spokenTranscript,
                measuredPitchContour = validContour,
                context = appContext
            ).copy(
                engineSource = "Local Sensei ($0 • Streak Preserved)",
                hasMeasuredAudioContour = validContour != null
            )
            return@withContext Result.success(byokFallback)
        }

        // 4. Smart Hybrid: Atomic deduct-with-refund inside daily free cap, graceful $0 local fallback on cap/error
        val activeTier = aiSettingsManager?.usageQuotaManager?.resolveCurrentTier(QuotaFeature.PRONUNCIATION_EVALUATION)
            ?: AiModelConfig.AiCapabilityTier.TIER_1_PRIMARY
        val reserved = aiSettingsManager?.tryConsumeCloudQuota(QuotaFeature.PRONUNCIATION_EVALUATION) ?: true
        if (!reserved) {
            aiSettingsManager?.usageQuotaManager?.recordFallbackCascade()
            val fallback = LocalPronunciationEvaluator.evaluate(
                targetLanguage = targetLanguage,
                targetPhrase = promptOrWord,
                spokenTranscript = spokenTranscript,
                measuredPitchContour = validContour,
                context = appContext
            ).copy(
                engineSource = "Local Sensei ($0 • Streak Preserved)",
                hasMeasuredAudioContour = validContour != null
            )
            return@withContext Result.success(fallback)
        }

        val targetModel = if (activeTier == AiModelConfig.AiCapabilityTier.TIER_2_ECONOMY_LITE) {
            economyPronunciationModel
        } else {
            primaryPronunciationModel
        }
        val sourceLabel = if (activeTier == AiModelConfig.AiCapabilityTier.TIER_2_ECONOMY_LITE) {
            "Eco Lite AI"
        } else {
            "Standard AI"
        }

        try {
            val response = withTimeout(45_000L) {
                targetModel.generateContent(prompt)
            }
            val jsonText = response.text ?: throw IllegalStateException("Model returned empty text.")
            val parsed = parsePronunciationJson(jsonText, promptOrWord, engineSource = sourceLabel)
            cacheDao?.insertOrReplace(parsed.toEntity(cacheKey, targetLanguage, promptOrWord, spokenTranscript))
            Result.success(applyMeasuredContour(parsed, validContour))
        } catch (e: Exception) {
            if (e is CancellationException && e !is TimeoutCancellationException) {
                aiSettingsManager?.refundCloudQuota(QuotaFeature.PRONUNCIATION_EVALUATION)
                throw e
            }
            Log.w(TAG, "Cloud evaluation failed with $activeTier, cascading to economy or local coach", e)
            aiSettingsManager?.usageQuotaManager?.recordFallbackCascade()

            if (activeTier == AiModelConfig.AiCapabilityTier.TIER_1_PRIMARY) {
                val cascadeResult = runCatching {
                    val fallbackResponse = withTimeout(20_000L) {
                        economyPronunciationModel.generateContent(prompt)
                    }
                    val jsonText = fallbackResponse.text ?: throw IllegalStateException("Economy model empty text.")
                    val parsed = parsePronunciationJson(jsonText, promptOrWord, engineSource = "Eco Lite AI (Auto-Cascade)")
                    cacheDao?.insertOrReplace(parsed.toEntity(cacheKey, targetLanguage, promptOrWord, spokenTranscript))
                    applyMeasuredContour(parsed, validContour)
                }
                cascadeResult.onSuccess {
                    return@withContext Result.success(it)
                }.onFailure { cascadeErr ->
                    if (cascadeErr is CancellationException && cascadeErr !is TimeoutCancellationException) {
                        aiSettingsManager?.refundCloudQuota(QuotaFeature.PRONUNCIATION_EVALUATION)
                        throw cascadeErr
                    }
                }
            }

            // Refund reserved credit since cloud & auto-cascade both failed
            aiSettingsManager?.refundCloudQuota(QuotaFeature.PRONUNCIATION_EVALUATION)

            // Zero-Cost Local / Deterministic Fallback: Streak preserved, XP earned, $0 tokens
            Result.success(
                LocalPronunciationEvaluator.evaluate(
                    targetLanguage,
                    promptOrWord,
                    spokenTranscript,
                    measuredPitchContour = validContour,
                    context = appContext
                ).copy(
                    engineSource = "Local Sensei ($0 • Streak Preserved)",
                    hasMeasuredAudioContour = validContour != null
                )
            )
        }
    }

    private fun applyMeasuredContour(
        result: PronunciationResult,
        measuredContour: List<Int>?
    ): PronunciationResult {
        return if (measuredContour != null && measuredContour.size >= 3) {
            result.copy(
                learnerPitchPoints = measuredContour,
                hasMeasuredAudioContour = true
            )
        } else {
            result
        }
    }

    suspend fun conductRoleplayTurn(
        language: String,
        scenarioTitle: String,
        conversationHistory: List<Pair<String, String>>,
        userSpokenMessage: String
    ): Result<RoleplayTurn> = withContext(Dispatchers.IO) {
        val mode = aiSettingsManager?.getCurrentMode() ?: AiCoachingMode.HYBRID_FREE
        if (mode == AiCoachingMode.LOCAL_OFFLINE_ONLY) {
            return@withContext Result.success(
                LocalPronunciationEvaluator.conductLocalRoleplayTurn(language, scenarioTitle, userSpokenMessage, context = appContext)
            )
        }

        // Sliding context window: send only last 6 turns to stretch free tier 3x
        val historyBuilder = StringBuilder()
        conversationHistory.takeLast(AiModelConfig.SLIDING_CONTEXT_MAX_TURNS).forEach { (speaker, text) ->
            historyBuilder.append("$speaker: $text\n")
        }

        val responseLang = Locale.getDefault().displayLanguage.ifBlank { "English" }
        val prompt = """
            You are 'Pocket Sensei', playing a character in a voice roleplay conversation to teach $language.
            Scenario: "$scenarioTitle".

            Recent Conversation:
            $historyBuilder

            Student just said: "$userSpokenMessage".

            Respond in character as your role in this scenario as JSON with keys: senseiReply, phoneticGuide, englishTranslation (translation of senseiReply in $responseLang), accentAndGrammarNote (feedback in $responseLang), suggestedUserReplies (array in $language), senseiMood ('praising'|'correcting'|'speaking'|'thinking').
        """.trimIndent()

        if (mode == AiCoachingMode.BYOK_USER_KEY) {
            val userKey = aiSettingsManager?.getUserApiKey().orEmpty()
            if (userKey.isNotBlank()) {
                val byokResult = runCatching {
                    val jsonText = callByokGeminiJson(userKey, prompt)
                    parseRoleplayJson(jsonText)
                }
                byokResult.onSuccess { return@withContext Result.success(it) }
                byokResult.onFailure { e ->
                    if (e is CancellationException && e !is TimeoutCancellationException) throw e
                    Log.w(TAG, "BYOK roleplay turn failed, falling back to Local Sensei", e)
                }
            }
            aiSettingsManager?.usageQuotaManager?.recordFallbackCascade()
            return@withContext Result.success(
                LocalPronunciationEvaluator.conductLocalRoleplayTurn(language, scenarioTitle, userSpokenMessage, context = appContext)
            )
        }

        val activeTier = aiSettingsManager?.usageQuotaManager?.resolveCurrentTier(QuotaFeature.ROLEPLAY_CHAT)
            ?: AiModelConfig.AiCapabilityTier.TIER_1_PRIMARY
        val reserved = aiSettingsManager?.tryConsumeCloudQuota(QuotaFeature.ROLEPLAY_CHAT) ?: true
        if (!reserved) {
            aiSettingsManager?.usageQuotaManager?.recordFallbackCascade()
            return@withContext Result.success(
                LocalPronunciationEvaluator.conductLocalRoleplayTurn(language, scenarioTitle, userSpokenMessage, context = appContext)
            )
        }

        val targetRoleplayModel = if (activeTier == AiModelConfig.AiCapabilityTier.TIER_2_ECONOMY_LITE) {
            economyRoleplayModel
        } else {
            primaryRoleplayModel
        }

        try {
            val response = withTimeout(45_000L) {
                targetRoleplayModel.generateContent(prompt)
            }
            val jsonText = response.text ?: throw IllegalStateException("Model returned empty text.")
            val turn = parseRoleplayJson(jsonText)
            Result.success(turn)
        } catch (e: Exception) {
            if (e is CancellationException && e !is TimeoutCancellationException) {
                aiSettingsManager?.refundCloudQuota(QuotaFeature.ROLEPLAY_CHAT)
                throw e
            }
            Log.w(TAG, "Roleplay cloud failed with $activeTier, cascading to economy or local reflex engine", e)
            aiSettingsManager?.usageQuotaManager?.recordFallbackCascade()

            if (activeTier == AiModelConfig.AiCapabilityTier.TIER_1_PRIMARY) {
                val cascadeResult = runCatching {
                    val fallbackResponse = withTimeout(20_000L) {
                        economyRoleplayModel.generateContent(prompt)
                    }
                    val jsonText = fallbackResponse.text ?: throw IllegalStateException("Economy roleplay model empty text.")
                    parseRoleplayJson(jsonText)
                }
                cascadeResult.onSuccess {
                    return@withContext Result.success(it)
                }.onFailure { cascadeErr ->
                    if (cascadeErr is CancellationException && cascadeErr !is TimeoutCancellationException) {
                        aiSettingsManager?.refundCloudQuota(QuotaFeature.ROLEPLAY_CHAT)
                        throw cascadeErr
                    }
                }
            }

            aiSettingsManager?.refundCloudQuota(QuotaFeature.ROLEPLAY_CHAT)
            Result.success(
                LocalPronunciationEvaluator.conductLocalRoleplayTurn(language, scenarioTitle, userSpokenMessage, context = appContext)
            )
        }
    }

    private fun callByokGeminiJson(userApiKey: String, prompt: String): String {
        val bodyJson = JSONObject().apply {
            put(
                "contents",
                JSONArray().put(
                    JSONObject().put(
                        "parts",
                        JSONArray().put(JSONObject().put("text", prompt))
                    )
                )
            )
            put(
                "generationConfig",
                JSONObject().put("responseMimeType", "application/json")
            )
        }.toString()

        val request = Request.Builder()
            .url("${AiModelConfig.BYOK_ENDPOINT_BASE}${AiModelConfig.PRIMARY_REASONING_MODEL}:generateContent")
            .addHeader("x-goog-api-key", userApiKey)
            .post(bodyJson.toRequestBody("application/json".toMediaType()))
            .build()

        httpClient.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IllegalStateException("BYOK Gemini error (${response.code})")
            }
            val root = JSONObject(raw)
            return root.getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
        }
    }

    private fun parsePronunciationJson(
        jsonText: String,
        promptOrWord: String,
        engineSource: String
    ): PronunciationResult {
        val json = JSONObject(jsonText)
        val mispronouncedArray = json.optJSONArray("mispronouncedParts") ?: JSONArray()
        val mispronounced = (0 until mispronouncedArray.length()).map { mispronouncedArray.getString(it) }

        val nativeArray = json.optJSONArray("nativePhrasings") ?: JSONArray()
        val nativePhrasings = (0 until nativeArray.length()).map { nativeArray.getString(it) }

        val targetPointsArray = json.optJSONArray("targetPitchPoints")
        val targetPoints = if (targetPointsArray != null && targetPointsArray.length() >= 3) {
            (0 until targetPointsArray.length()).map { targetPointsArray.getInt(it).coerceIn(15, 90) }
        } else {
            listOf(55, 80, 65, 40)
        }

        val learnerPointsArray = json.optJSONArray("learnerPitchPoints")
        val learnerPoints = if (learnerPointsArray != null && learnerPointsArray.length() >= 3) {
            (0 until learnerPointsArray.length()).map { learnerPointsArray.getInt(it).coerceIn(15, 90) }
        } else {
            listOf(50, 70, 60, 45)
        }

        return PronunciationResult(
            score = json.optInt("score", 85).coerceIn(0, 100),
            phoneticAccuracy = json.optInt("phoneticAccuracy", 85).coerceIn(0, 100),
            intonationPitch = json.optInt("intonationPitch", 80).coerceIn(0, 100),
            fluency = json.optInt("fluency", 85).coerceIn(0, 100),
            phoneticBreakdown = json.optString("phoneticBreakdown", promptOrWord),
            mispronouncedParts = mispronounced,
            coachingTip = json.optString(
                "coachingTip",
                appContext?.getString(R.string.eval_tip_default) ?: "Keep your mouth relaxed and articulate clearly."
            ),
            nativePhrasings = nativePhrasings,
            encouragement = json.optString(
                "encouragement",
                appContext?.getString(R.string.eval_encourage_default) ?: "Great effort! Practice makes permanent."
            ),
            pitchContourType = json.optString(
                "pitchContourType",
                appContext?.getString(R.string.eval_contour_default) ?: "Natural Accent Contour"
            ),
            targetPitchPoints = targetPoints,
            learnerPitchPoints = learnerPoints,
            pitchGuidance = json.optString(
                "pitchGuidance",
                appContext?.getString(R.string.eval_pitch_guidance_cloud_default) ?: "Align stress rise with native speaker contour."
            ),
            engineSource = engineSource
        )
    }

    private fun parseRoleplayJson(jsonText: String): RoleplayTurn {
        val json = JSONObject(jsonText)
        val suggestionsArray = json.optJSONArray("suggestedUserReplies") ?: JSONArray()
        val suggestions = (0 until suggestionsArray.length()).map { suggestionsArray.getString(it) }

        return RoleplayTurn(
            senseiReply = json.optString("senseiReply", "Hai, kashikomarimashita!"),
            phoneticGuide = json.optString("phoneticGuide", ""),
            englishTranslation = json.optString("englishTranslation", ""),
            accentAndGrammarNote = json.optString(
                "accentAndGrammarNote",
                appContext?.getString(R.string.roleplay_cloud_note_default) ?: "Great conversational flow!"
            ),
            suggestedUserReplies = suggestions,
            senseiMood = json.optString("senseiMood", "speaking")
        )
    }

    suspend fun generateByokTopicPack(
        language: String,
        topic: String
    ): Result<GeneratedTopicPack> = withContext(Dispatchers.IO) {
        val cleanTopic = topic.trim().ifBlank { "Travel Conversation" }
        val mode = aiSettingsManager?.getCurrentMode() ?: AiCoachingMode.HYBRID_FREE

        if (mode == AiCoachingMode.LOCAL_OFFLINE_ONLY) {
            return@withContext Result.success(buildLocalTopicPackFallback(language, cleanTopic))
        }

        val responseLang = Locale.getDefault().displayLanguage.ifBlank { "English" }
        val prompt = """
            You are 'Pocket Sensei', an expert language coach.
            Create a custom speaking lesson pack in $language for the topic: "$cleanTopic".
            Write translations and pronunciation focus tips in $responseLang.
            Return raw JSON with keys:
            - "phrases": array of 3 objects, each with "targetText", "phonetic", "translation", "accentFocus"
            - "scenarioTitle": short title for a roleplay on this topic
            - "senseiRole": Sensei's role in the roleplay
            - "userRole": learner's role
            - "openingLine": Sensei's first line in $language
        """.trimIndent()

        if (mode == AiCoachingMode.BYOK_USER_KEY) {
            val userKey = aiSettingsManager?.getUserApiKey().orEmpty().trim()
            if (userKey.isNotBlank()) {
                val byokAttempt = runCatching {
                    val rawJson = callByokGeminiJson(userKey, prompt)
                    parseTopicPackJson(rawJson, cleanTopic, language, usedByokCloud = true)
                }
                byokAttempt.onSuccess { return@withContext Result.success(it) }
                byokAttempt.onFailure { e ->
                    if (e is CancellationException && e !is TimeoutCancellationException) throw e
                    Log.w(TAG, "BYOK topic pack generation failed, using local pack", e)
                }
            }
            aiSettingsManager?.usageQuotaManager?.recordFallbackCascade()
            return@withContext Result.success(buildLocalTopicPackFallback(language, cleanTopic))
        }

        val activeTier = aiSettingsManager?.usageQuotaManager?.resolveCurrentTier(QuotaFeature.TOPIC_PACK)
            ?: AiModelConfig.AiCapabilityTier.TIER_1_PRIMARY
        val reserved = aiSettingsManager?.tryConsumeCloudQuota(QuotaFeature.TOPIC_PACK) ?: true
        if (!reserved) {
            aiSettingsManager?.usageQuotaManager?.recordFallbackCascade()
            return@withContext Result.success(buildLocalTopicPackFallback(language, cleanTopic))
        }

        val targetTopicModel = if (activeTier == AiModelConfig.AiCapabilityTier.TIER_2_ECONOMY_LITE) {
            economyTopicPackModel
        } else {
            primaryTopicPackModel
        }

        try {
            val response = withTimeout(45_000L) {
                targetTopicModel.generateContent(prompt)
            }
            val rawJson = response.text ?: throw IllegalStateException("Topic pack model returned empty text.")
            Result.success(parseTopicPackJson(rawJson, cleanTopic, language, usedByokCloud = false))
        } catch (e: Exception) {
            if (e is CancellationException && e !is TimeoutCancellationException) {
                aiSettingsManager?.refundCloudQuota(QuotaFeature.TOPIC_PACK)
                throw e
            }
            Log.w(TAG, "Topic pack cloud failed with $activeTier, cascading to economy or local pack", e)
            aiSettingsManager?.usageQuotaManager?.recordFallbackCascade()

            if (activeTier == AiModelConfig.AiCapabilityTier.TIER_1_PRIMARY) {
                val cascadeAttempt = runCatching {
                    val fallbackResponse = withTimeout(20_000L) {
                        economyTopicPackModel.generateContent(prompt)
                    }
                    val rawJson = fallbackResponse.text ?: throw IllegalStateException("Economy topic pack model empty text.")
                    parseTopicPackJson(rawJson, cleanTopic, language, usedByokCloud = false)
                }
                cascadeAttempt.onSuccess {
                    return@withContext Result.success(it)
                }.onFailure { cascadeErr ->
                    if (cascadeErr is CancellationException && cascadeErr !is TimeoutCancellationException) {
                        aiSettingsManager?.refundCloudQuota(QuotaFeature.TOPIC_PACK)
                        throw cascadeErr
                    }
                }
            }

            aiSettingsManager?.refundCloudQuota(QuotaFeature.TOPIC_PACK)
            Result.success(buildLocalTopicPackFallback(language, cleanTopic))
        }
    }

    private fun parseTopicPackJson(
        rawJson: String,
        cleanTopic: String,
        language: String,
        usedByokCloud: Boolean
    ): GeneratedTopicPack {
        val root = JSONObject(rawJson)
        val arr = root.optJSONArray("phrases") ?: JSONArray()
        val parsedPhrases = (0 until arr.length()).mapNotNull { idx ->
            val item = arr.optJSONObject(idx) ?: return@mapNotNull null
            val target = item.optString("targetText").trim()
            if (target.isBlank()) return@mapNotNull null
            GeneratedTopicPhrase(
                targetText = target,
                phonetic = item.optString("phonetic", target).trim(),
                translation = item.optString("translation", cleanTopic).trim(),
                accentFocus = item.optString(
                    "accentFocus",
                    appContext?.getString(R.string.coach_default_accent_focus) ?: "Natural cadence"
                ).trim()
            )
        }
        if (parsedPhrases.isEmpty()) error("Empty phrases from AI topic pack")
        val defaultSenseiRole = appContext?.getString(R.string.topic_pack_sensei_role) ?: "Sensei"
        val defaultUserRole = appContext?.getString(R.string.topic_pack_user_role) ?: "Traveler"
        return GeneratedTopicPack(
            topic = cleanTopic,
            language = language,
            phrases = parsedPhrases,
            scenarioTitle = root.optString("scenarioTitle", cleanTopic).trim().ifBlank { cleanTopic },
            senseiRole = root.optString("senseiRole", defaultSenseiRole).trim().ifBlank { defaultSenseiRole },
            userRole = root.optString("userRole", defaultUserRole).trim().ifBlank { defaultUserRole },
            openingLine = root.optString("openingLine", parsedPhrases.first().targetText).trim()
                .ifBlank { parsedPhrases.first().targetText },
            usedByokCloud = usedByokCloud
        )
    }

    fun buildLocalTopicPackFallback(language: String, topic: String): GeneratedTopicPack {
        val cleanTopic = topic.trim().ifBlank { "Conversation" }
        val focusDefault = appContext?.getString(R.string.coach_default_accent_focus) ?: "Vowel & Consonant clarity"
        val trans1 = appContext?.getString(R.string.topic_pack_phrase1_trans, cleanTopic)
            ?: "Excuse me, please tell me about $cleanTopic."
        val trans2 = appContext?.getString(R.string.topic_pack_phrase2_trans)
            ?: "Which one do you recommend?"
        val trans3 = appContext?.getString(R.string.topic_pack_phrase3_trans)
            ?: "That was very helpful, thank you!"
        val roleSensei = appContext?.getString(R.string.topic_pack_sensei_role) ?: "Local Guide"
        val roleUser = appContext?.getString(R.string.topic_pack_user_role) ?: "Traveler"

        return when (language.lowercase()) {
            "japanese", "ja" -> GeneratedTopicPack(
                topic = cleanTopic,
                language = language,
                phrases = listOf(
                    GeneratedTopicPhrase("すみません、${cleanTopic}について教えてください。", "Sumimasen, $cleanTopic ni tsuite oshiete kudasai.", trans1, focusDefault),
                    GeneratedTopicPhrase("おすすめはどれですか？", "Osusume wa dore desu ka?", trans2, focusDefault),
                    GeneratedTopicPhrase("とても助かりました、ありがとうございます！", "Totemo tasukarimashita, arigatou gozaimasu!", trans3, focusDefault)
                ),
                scenarioTitle = cleanTopic,
                senseiRole = roleSensei,
                userRole = roleUser,
                openingLine = "こんにちは！${cleanTopic}ですね。どのようなご希望ですか？",
                usedByokCloud = false
            )
            "spanish", "es" -> GeneratedTopicPack(
                topic = cleanTopic,
                language = language,
                phrases = listOf(
                    GeneratedTopicPhrase("Disculpe, ¿me puede ayudar con $cleanTopic?", "dees-KOOL-peh, meh PWEH-deh ah-yoo-DAR", trans1, focusDefault),
                    GeneratedTopicPhrase("¿Cuál me recomienda hoy?", "kwahl meh rreh-koh-MYEHN-dah oy", trans2, focusDefault),
                    GeneratedTopicPhrase("¡Muchas gracias por su amabilidad!", "MOO-chahs GRAH-syahs", trans3, focusDefault)
                ),
                scenarioTitle = cleanTopic,
                senseiRole = roleSensei,
                userRole = roleUser,
                openingLine = "¡Hola! Bienvenido. ¿En qué puedo ayudarte hoy con $cleanTopic?",
                usedByokCloud = false
            )
            "french", "fr" -> GeneratedTopicPack(
                topic = cleanTopic,
                language = language,
                phrases = listOf(
                    GeneratedTopicPhrase("Bonjour, j'ai une question concernant $cleanTopic.", "Bohn-zhoor, zhay oon kehs-tyohn", trans1, focusDefault),
                    GeneratedTopicPhrase("Qu'est-ce que vous me conseillez ?", "Kess-kuh voo muh kohn-say-yay", trans2, focusDefault),
                    GeneratedTopicPhrase("Merci infiniment pour votre aide !", "Mehr-see ahn-fee-nee-mahn", trans3, focusDefault)
                ),
                scenarioTitle = cleanTopic,
                senseiRole = roleSensei,
                userRole = roleUser,
                openingLine = "Bonjour ! Comment puis-je vous aider pour $cleanTopic aujourd'hui ?",
                usedByokCloud = false
            )
            "german", "de" -> GeneratedTopicPack(
                topic = cleanTopic,
                language = language,
                phrases = listOf(
                    GeneratedTopicPhrase("Entschuldigung, können Sie mir bei $cleanTopic helfen?", "Ent-SHOOL-dee-goong, KUN-nen zee meer", trans1, focusDefault),
                    GeneratedTopicPhrase("Was würden Sie mir heute empfehlen?", "Vahs VOOR-den zee meer emp-FAY-len", trans2, focusDefault),
                    GeneratedTopicPhrase("Vielen herzlichen Dank für Ihre Hilfe!", "FEE-len HERTS-li-khen dahnk", trans3, focusDefault)
                ),
                scenarioTitle = cleanTopic,
                senseiRole = roleSensei,
                userRole = roleUser,
                openingLine = "Hallo! Wie kann ich Ihnen heute bei $cleanTopic weiterhelfen?",
                usedByokCloud = false
            )
            "korean", "ko" -> GeneratedTopicPack(
                topic = cleanTopic,
                language = language,
                phrases = listOf(
                    GeneratedTopicPhrase("실례합니다, ${cleanTopic}에 대해 알려주세요.", "Sil-lye-ham-ni-da, $cleanTopic-e dae-hae al-lyeo-ju-se-yo.", trans1, focusDefault),
                    GeneratedTopicPhrase("어떤 것을 추천해 주시겠어요?", "Eo-tteon geo-seul chu-cheon-hae ju-si-ge-sseo-yo?", trans2, focusDefault),
                    GeneratedTopicPhrase("큰 도움이 되었어요, 정말 감사합니다!", "Keun do-u-mi doe-eo-sseo-yo, jeong-mal gam-sa-ham-ni-da!", trans3, focusDefault)
                ),
                scenarioTitle = cleanTopic,
                senseiRole = roleSensei,
                userRole = roleUser,
                openingLine = "어서 오세요! 오늘 ${cleanTopic}에 대해 어떤 도움이 필요하신가요?",
                usedByokCloud = false
            )
            "mandarin", "chinese", "zh" -> GeneratedTopicPack(
                topic = cleanTopic,
                language = language,
                phrases = listOf(
                    GeneratedTopicPhrase("请问，能向我介绍一下${cleanTopic}吗？", "Qǐngwèn, néng xiàng wǒ jièshào yīxià $cleanTopic ma?", trans1, focusDefault),
                    GeneratedTopicPhrase("您有什么好的推荐吗？", "Nín yǒu shénme hǎo de tuījiàn ma?", trans2, focusDefault),
                    GeneratedTopicPhrase("非常有帮助，太感谢您了！", "Fēicháng yǒu bāngzhù, tài gǎnxiè nín le!", trans3, focusDefault)
                ),
                scenarioTitle = cleanTopic,
                senseiRole = roleSensei,
                userRole = roleUser,
                openingLine = "您好！关于${cleanTopic}，今天想了解些什么呢？",
                usedByokCloud = false
            )
            "italian", "it" -> GeneratedTopicPack(
                topic = cleanTopic,
                language = language,
                phrases = listOf(
                    GeneratedTopicPhrase("Mi scusi, può darmi informazioni su $cleanTopic?", "Mee SKOO-zee, pwoh DAR-mee", trans1, focusDefault),
                    GeneratedTopicPhrase("Che cosa mi consiglia oggi?", "Keh KOH-zah mee kohn-SEEL-yah", trans2, focusDefault),
                    GeneratedTopicPhrase("Grazie mille, è stato gentilissimo!", "GRAHT-syeh MEEL-leh", trans3, focusDefault)
                ),
                scenarioTitle = cleanTopic,
                senseiRole = roleSensei,
                userRole = roleUser,
                openingLine = "Buongiorno! Come posso aiutarla oggi con $cleanTopic?",
                usedByokCloud = false
            )
            else -> GeneratedTopicPack(
                topic = cleanTopic,
                language = language,
                phrases = listOf(
                    GeneratedTopicPhrase("Could you help me with $cleanTopic, please?", "kʊd juː hɛlp miː", trans1, focusDefault),
                    GeneratedTopicPhrase("What would you recommend for this situation?", "wɒt wʊd juː ˌrɛkəˈmɛnd", trans2, focusDefault),
                    GeneratedTopicPhrase("Thank you, that makes everything clear!", "θæŋk juː", trans3, focusDefault)
                ),
                scenarioTitle = cleanTopic,
                senseiRole = roleSensei,
                userRole = roleUser,
                openingLine = "Welcome! Let's practice talking about $cleanTopic together.",
                usedByokCloud = false
            )
        }
    }

    companion object {
        fun buildCacheKey(language: String, targetPhrase: String, spokenTranscript: String): String {
            val l = language.trim().lowercase()
            val p = LocalPronunciationEvaluator.normalizeForComparison(targetPhrase)
            val s = LocalPronunciationEvaluator.normalizeForComparison(spokenTranscript)
            return "$l|$p|$s"
        }
    }
}

private fun CachedEvaluationEntity.toDomain(engineSource: String): PronunciationResult {
    val mispronounced = runCatching {
        val arr = JSONArray(mispronouncedJson)
        (0 until arr.length()).map { arr.getString(it) }
    }.getOrDefault(emptyList())

    val nativeList = runCatching {
        val arr = JSONArray(nativePhrasingsJson)
        (0 until arr.length()).map { arr.getString(it) }
    }.getOrDefault(emptyList())

    val targetPts = runCatching {
        val arr = JSONArray(targetPitchJson)
        (0 until arr.length()).map { arr.getInt(it) }
    }.getOrDefault(listOf(55, 80, 65, 40))

    val learnerPts = runCatching {
        val arr = JSONArray(learnerPitchJson)
        (0 until arr.length()).map { arr.getInt(it) }
    }.getOrDefault(listOf(50, 70, 60, 45))

    return PronunciationResult(
        score = score,
        phoneticAccuracy = phoneticAccuracy,
        intonationPitch = intonationPitch,
        fluency = fluency,
        phoneticBreakdown = phoneticBreakdown,
        mispronouncedParts = mispronounced,
        coachingTip = coachingTip,
        nativePhrasings = nativeList,
        encouragement = encouragement,
        pitchContourType = pitchContourType,
        targetPitchPoints = targetPts,
        learnerPitchPoints = learnerPts,
        pitchGuidance = pitchGuidance,
        engineSource = engineSource
    )
}

private fun PronunciationResult.toEntity(
    cacheKey: String,
    language: String,
    targetPhrase: String,
    spokenTranscript: String
): CachedEvaluationEntity {
    return CachedEvaluationEntity(
        cacheKey = cacheKey,
        language = language,
        targetPhrase = targetPhrase,
        spokenTranscript = spokenTranscript,
        score = score,
        phoneticAccuracy = phoneticAccuracy,
        intonationPitch = intonationPitch,
        fluency = fluency,
        phoneticBreakdown = phoneticBreakdown,
        mispronouncedJson = JSONArray(mispronouncedParts).toString(),
        coachingTip = coachingTip,
        nativePhrasingsJson = JSONArray(nativePhrasings).toString(),
        encouragement = encouragement,
        pitchContourType = pitchContourType,
        targetPitchJson = JSONArray(targetPitchPoints).toString(),
        learnerPitchJson = JSONArray(learnerPitchPoints).toString(),
        pitchGuidance = pitchGuidance
    )
}
