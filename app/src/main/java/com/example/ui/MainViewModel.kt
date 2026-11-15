package com.example.ui

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.ai.AiCoachingMode
import com.example.ai.AiSenseiCoach
import com.example.ai.AiSettingsManager
import com.example.ai.AiSettingsSnapshot
import com.example.ai.PronunciationResult
import com.example.ai.RoleplayTurn
import com.example.analytics.DailyStreakState
import com.example.analytics.EngagementManager
import com.example.analytics.EngagementPrompt
import com.example.analytics.PromptAction
import com.example.analytics.TriggerMoment
import com.example.data.local.AppEventLog
import com.example.data.local.CachedEvaluationDao
import com.example.data.local.CustomPhraseDao
import com.example.data.local.CustomPhraseEntity
import com.example.data.local.CustomScenarioDao
import com.example.data.local.CustomScenarioEntity
import com.example.data.local.SpacedRepetitionScheduler
import com.example.data.local.VocabularyWord
import com.example.data.model.LeaderboardEntry
import com.example.data.model.PracticeSession
import com.example.data.model.UserProfile
import com.example.data.model.VocabItem
import com.example.data.repository.SenseiRepository
import com.example.donation.DonationBillingManager
import com.example.donation.DonationUiState
import com.example.notifications.ReminderScheduler
import com.example.notifications.StreakReminderPreferences
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class LearningTab {
    COACH,
    ROLEPLAY,
    VOCAB,
    LEAGUE,
    PROFILE
}

data class PracticePhrase(
    val id: String,
    val targetText: String,
    val phonetic: String,
    val translation: String,
    val difficulty: String,
    val accentFocus: String,
    @param:StringRes val translationRes: Int = 0,
    @param:StringRes val accentFocusRes: Int = 0
)

data class RoleplayScenario(
    val id: String,
    val title: String,
    val language: String,
    val description: String,
    val senseiRole: String,
    val userRole: String,
    val initialMessage: String,
    val initialPhonetic: String,
    val initialTranslation: String,
    @param:StringRes val titleRes: Int = 0,
    @param:StringRes val senseiRoleRes: Int = 0,
    @param:StringRes val userRoleRes: Int = 0,
    @param:StringRes val initialTranslationRes: Int = 0
)

data class DialogMessage(
    val id: String,
    val speaker: String, // "Sensei" or "User"
    val text: String,
    val phonetic: String = "",
    val translation: String = "",
    val accentNote: String = "",
    val isUser: Boolean = false,
    @param:StringRes val speakerRes: Int = 0,
    @param:StringRes val translationRes: Int = 0
)

sealed interface EvaluationUiState {
    data object Idle : EvaluationUiState
    data object Evaluating : EvaluationUiState
    data class Success(val result: PronunciationResult) : EvaluationUiState
    data class Error(val message: String) : EvaluationUiState
}

sealed interface RoleplayUiState {
    data object Idle : RoleplayUiState
    data object Thinking : RoleplayUiState
    data class Error(val message: String) : RoleplayUiState
}

class MainViewModel(
    private val repository: SenseiRepository,
    private val engagementManager: EngagementManager? = null,
    private val donationBillingManager: DonationBillingManager? = null,
    private val aiSettingsManager: AiSettingsManager? = null,
    private val cacheDao: CachedEvaluationDao? = null,
    private val appContext: Context? = null,
    private val coach: AiSenseiCoach = AiSenseiCoach(cacheDao, aiSettingsManager, appContext),
    private val customPhraseDao: CustomPhraseDao? = null,
    private val customScenarioDao: CustomScenarioDao? = null
) : ViewModel() {

    private val _currentTab = MutableStateFlow(LearningTab.COACH)
    val currentTab: StateFlow<LearningTab> = _currentTab.asStateFlow()

    val supportedLanguages = listOf(
        "Japanese",
        "Spanish",
        "French",
        "German",
        "Korean",
        "Mandarin",
        "Italian",
        "English"
    )

    private val initialLanguage: String = appContext
        ?.let { com.example.ui.screens.OnboardingPreferences.getPreferredLanguage(it) }
        ?.takeIf { lang -> supportedLanguages.any { it.equals(lang, ignoreCase = true) } }
        ?: "Japanese"

    private val _targetLanguage = MutableStateFlow(initialLanguage)
    val targetLanguage: StateFlow<String> = _targetLanguage.asStateFlow()

    val profileState: StateFlow<UserProfile?> = repository.observeProfile()
        .catch { emit(null) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = null
        )

    val vocabListState: StateFlow<List<VocabItem>> = repository.observeVocab()
        .catch { emit(emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val sessionsListState: StateFlow<List<PracticeSession>> = repository.observeSessions()
        .catch { emit(emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val leaderboardState: StateFlow<List<LeaderboardEntry>> = repository.observeLeaderboard()
        .catch { emit(emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val localVocabularyState: StateFlow<List<VocabularyWord>> = repository.observeLocalVocabulary()
        .catch { emit(emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val activeEngagementPrompt: StateFlow<EngagementPrompt?> =
        engagementManager?.activePrompt ?: MutableStateFlow<EngagementPrompt?>(null).asStateFlow()

    val recentAppEvents: StateFlow<List<AppEventLog>> =
        (engagementManager?.observeRecentEvents(15) ?: emptyFlow())
            .catch { emit(emptyList()) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = emptyList()
            )

    val totalAppEventCount: StateFlow<Int> =
        (engagementManager?.observeTotalEventCount() ?: emptyFlow())
            .catch { emit(0) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = 0
            )

    val donationUiState: StateFlow<DonationUiState> =
        donationBillingManager?.uiState ?: MutableStateFlow(DonationUiState()).asStateFlow()

    val isActiveSupporter: StateFlow<Boolean> =
        engagementManager?.isActiveSupporter ?: MutableStateFlow(false).asStateFlow()

    val shouldShowHomeDonationBanner: StateFlow<Boolean> =
        engagementManager?.shouldShowHomeDonationBanner ?: MutableStateFlow(false).asStateFlow()

    val successfulOperationsCount: StateFlow<Int> =
        engagementManager?.successfulOperationsCount ?: MutableStateFlow(0).asStateFlow()

    val openDonationSheetRequested: StateFlow<Boolean> =
        engagementManager?.openDonationSheetRequested ?: MutableStateFlow(false).asStateFlow()

    val dailyGoalProgress: StateFlow<Pair<Int, Int>> =
        engagementManager?.dailyGoalProgress ?: MutableStateFlow(0 to 3).asStateFlow()

    val dailyStreakState: StateFlow<DailyStreakState> =
        engagementManager?.dailyStreakState ?: MutableStateFlow(DailyStreakState()).asStateFlow()

    val aiSettingsState: StateFlow<AiSettingsSnapshot> =
        aiSettingsManager?.settings ?: MutableStateFlow(AiSettingsSnapshot()).asStateFlow()

    val quotaState: StateFlow<com.example.ai.QuotaSnapshot> =
        aiSettingsManager?.usageQuotaManager?.quotaState
            ?: MutableStateFlow(com.example.ai.QuotaSnapshot()).asStateFlow()

    val cachedEvalCount: StateFlow<Int> =
        (cacheDao?.observeCacheCount() ?: emptyFlow())
            .catch { emit(0) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = 0
            )

    val customPhrasesState: StateFlow<List<CustomPhraseEntity>> =
        (customPhraseDao?.observeAllPhrases() ?: emptyFlow())
            .catch { emit(emptyList()) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = emptyList()
            )

    val customScenariosState: StateFlow<List<CustomScenarioEntity>> =
        (customScenarioDao?.observeAllScenarios() ?: emptyFlow())
            .catch { emit(emptyList()) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = emptyList()
            )

    private val _streakReminderEnabled = MutableStateFlow(
        appContext?.let { StreakReminderPreferences.isEnabled(it) } ?: false
    )
    val streakReminderEnabled: StateFlow<Boolean> = _streakReminderEnabled.asStateFlow()

    private val _streakReminderHour = MutableStateFlow(
        appContext?.let { StreakReminderPreferences.getReminderHour(it) } ?: 20
    )
    val streakReminderHour: StateFlow<Int> = _streakReminderHour.asStateFlow()

    private val _leaderboardFilter = MutableStateFlow("All")
    val leaderboardFilter: StateFlow<String> = _leaderboardFilter.asStateFlow()

    fun setLeaderboardFilter(filter: String) {
        _leaderboardFilter.value = filter
        engagementManager?.trackEvent("leaderboard_filtered", "league", mapOf("filter" to filter))
    }

    private val _senseiCostume = MutableStateFlow("traditional")
    val senseiCostume: StateFlow<String> = _senseiCostume.asStateFlow()

    fun setSenseiCostume(costume: String) {
        _senseiCostume.value = costume
        engagementManager?.trackEvent("costume_changed", "customization", mapOf("costume" to costume))
    }

    // Accent Coach State
    private val _evaluationState = MutableStateFlow<EvaluationUiState>(EvaluationUiState.Idle)
    val evaluationState: StateFlow<EvaluationUiState> = _evaluationState.asStateFlow()

    private val _selectedPhrase = MutableStateFlow<PracticePhrase>(
        getPhrasesForLanguage(initialLanguage).firstOrNull() ?: defaultPhrasesJapanese.first()
    )
    val selectedPhrase: StateFlow<PracticePhrase> = _selectedPhrase.asStateFlow()

    // Roleplay State
    private val _selectedScenario = MutableStateFlow<RoleplayScenario>(
        defaultScenarios.firstOrNull { it.language.equals(initialLanguage, ignoreCase = true) }
            ?: defaultScenarios.first()
    )
    val selectedScenario: StateFlow<RoleplayScenario> = _selectedScenario.asStateFlow()

    private val _dialogMessages = MutableStateFlow<List<DialogMessage>>(emptyList())
    val dialogMessages: StateFlow<List<DialogMessage>> = _dialogMessages.asStateFlow()

    private val _suggestedReplies = MutableStateFlow<List<String>>(emptyList())
    val suggestedReplies: StateFlow<List<String>> = _suggestedReplies.asStateFlow()

    private val _roleplayUiState = MutableStateFlow<RoleplayUiState>(RoleplayUiState.Idle)
    val roleplayUiState: StateFlow<RoleplayUiState> = _roleplayUiState.asStateFlow()

    private val _senseiMood = MutableStateFlow("idle")
    val senseiMood: StateFlow<String> = _senseiMood.asStateFlow()

    init {
        aiSettingsManager?.let { settingsMgr ->
            viewModelScope.launch {
                settingsMgr.usageQuotaManager.quotaState.collect {
                    settingsMgr.refreshSettings()
                }
            }
        }
        val user = runCatching { Firebase.auth.currentUser }.getOrNull()
        if (user != null) {
            viewModelScope.launch {
                val profileResult = repository.getOrCreateProfile(
                    displayName = user.displayName?.takeIf { it.isNotBlank() }
                        ?: appContext?.getString(R.string.profile_default_name)
                        ?: "Sensei Student",
                    targetLanguage = _targetLanguage.value
                )
                profileResult.onSuccess { prof ->
                    engagementManager?.syncRemoteStreakIfHigher(
                        remoteStreak = prof.currentStreak.toInt(),
                        remoteUpdatedAtMs = prof.updatedAt?.toDate()?.time
                    )
                    val localStreak = engagementManager?.dailyStreakState?.value?.currentStreak?.toLong() ?: 0L
                    if (localStreak > prof.currentStreak) {
                        repository.updateStreak(localStreak)
                    }
                    repository.syncLeaderboardEntry(
                        prof.copy(currentStreak = maxOf(prof.currentStreak, localStreak)),
                        0L
                    )
                }
            }
        }
        viewModelScope.launch {
            profileState.collect { prof ->
                if (prof != null && prof.currentStreak > 0L) {
                    engagementManager?.syncRemoteStreakIfHigher(
                        remoteStreak = prof.currentStreak.toInt(),
                        remoteUpdatedAtMs = prof.updatedAt?.toDate()?.time
                    )
                }
            }
        }
        initScenario(_selectedScenario.value)
    }

    fun setTab(tab: LearningTab) {
        _currentTab.value = tab
        engagementManager?.trackEvent("tab_viewed", "navigation", mapOf("tab" to tab.name))
    }

    fun setTargetLanguage(language: String) {
        _targetLanguage.value = language
        engagementManager?.trackEvent("language_changed", "learning", mapOf("language" to language))
        viewModelScope.launch {
            repository.updateLearningLanguage(language)
        }
        // Update selected phrase
        val phraseList = getPhrasesForLanguage(language)
        if (phraseList.isNotEmpty()) {
            _selectedPhrase.value = phraseList.first()
        }
        // Update selected scenario
        val matchingScenario = defaultScenarios.firstOrNull { it.language.equals(language, ignoreCase = true) }
            ?: defaultScenarios.first()
        initScenario(matchingScenario)
    }

    fun selectPhrase(phrase: PracticePhrase) {
        _selectedPhrase.value = phrase
        _evaluationState.value = EvaluationUiState.Idle
    }

    fun selectScenario(scenario: RoleplayScenario) {
        initScenario(scenario)
    }

    private fun initScenario(scenario: RoleplayScenario) {
        _selectedScenario.value = scenario
        _roleplayUiState.value = RoleplayUiState.Idle
        val initialMsg = DialogMessage(
            id = "msg_0",
            speaker = scenario.senseiRole,
            text = scenario.initialMessage,
            phonetic = scenario.initialPhonetic,
            translation = scenario.initialTranslation,
            isUser = false,
            speakerRes = scenario.senseiRoleRes,
            translationRes = scenario.initialTranslationRes
        )
        _dialogMessages.value = listOf(initialMsg)
        _suggestedReplies.value = when (scenario.language) {
            "Japanese" -> listOf("Kore o kudasai.", "Matcha latte o onegaishimasu.", "Osusume wa nan desu ka?")
            "French" -> listOf("Un croissant s'il vous plaît.", "Je voudrais un café.", "Qu'est-ce que vous recommandez ?")
            "Spanish" -> listOf("Una cerveza, por favor.", "¿Qué tapas tiene hoy?", "La cuenta, por favor.")
            "German" -> listOf("Ich arbeite an einer KI-App.", "Können wir eine Fritz-Kola trinken?", "Das klingt super!")
            "Italian" -> listOf("Vorrei un cono al pistacchio.", "Qual è il gusto del giorno?", "Grazie mille!")
            "Korean" -> listOf("떡볶이 일인분 주세요.", "얼마예요?", "정말 맛있어요!")
            "Mandarin" -> listOf("请给我这个。", "请问这个多少钱？", "非常感谢！")
            else -> listOf("Hello! I would like to order.", "What do you recommend?", "Thank you so much!")
        }
        _senseiMood.value = "speaking"
    }

    fun evaluateSpeech(
        targetPhrase: String,
        spokenText: String,
        measuredPitchContour: List<Int>? = null
    ) {
        if (spokenText.isBlank()) return
        viewModelScope.launch {
            _evaluationState.value = EvaluationUiState.Evaluating
            _senseiMood.value = "thinking"

            val result = coach.evaluatePronunciation(
                targetLanguage = _targetLanguage.value,
                promptOrWord = targetPhrase,
                spokenTranscript = spokenText,
                measuredPitchContour = measuredPitchContour
            )

            result.onSuccess { eval ->
                _evaluationState.value = EvaluationUiState.Success(eval)
                _senseiMood.value = if (eval.score >= 85) "praising" else "correcting"
                // Save word to vocabulary bank
                val resolvedTranslation = _selectedPhrase.value.let { phrase ->
                    if (phrase.translationRes != 0) {
                        appContext?.getString(phrase.translationRes) ?: phrase.translation
                    } else {
                        phrase.translation
                    }
                }
                repository.addVocabItem(
                    word = targetPhrase,
                    translation = resolvedTranslation,
                    phonetic = eval.phoneticBreakdown,
                    language = _targetLanguage.value,
                    pronunciationScore = eval.score.toLong()
                )
                val streakOutcome = engagementManager?.recordDailyStreakPractice("pronunciation_coach")
                val latestStreakLong = streakOutcome?.first?.currentStreak?.toLong()
                if (latestStreakLong != null) {
                    repository.updateStreak(latestStreakLong)
                }
                profileState.value?.let { prof ->
                    val earnedXp = if (eval.score >= 85) 50L else 30L
                    val syncedProfile = if (latestStreakLong != null) {
                        prof.copy(currentStreak = latestStreakLong)
                    } else {
                        prof
                    }
                    repository.syncLeaderboardEntry(syncedProfile, earnedXp)
                }
                engagementManager?.recordSuccessfulCoreOperation(
                    operationName = "pronunciation_evaluation",
                    score = eval.score
                )
                engagementManager?.trackEvent(
                    eventName = "pronunciation_practiced",
                    category = "coach",
                    metadata = mapOf(
                        "language" to _targetLanguage.value,
                        "score" to eval.score,
                        "phrase" to targetPhrase
                    )
                )
                val currentMastered = profileState.value?.wordsMastered ?: 0L
                if (eval.score >= 85) {
                    engagementManager?.recordPositiveMilestone(
                        milestoneName = "high_score_achieved",
                        metadata = mapOf("score" to eval.score, "language" to _targetLanguage.value),
                        isCelebratory = true,
                        isUserBusy = false,
                        wordsMastered = currentMastered,
                        latestScore = eval.score
                    )
                } else {
                    engagementManager?.evaluatePromptOpportunity(
                        triggerMoment = TriggerMoment.POST_PRACTICE_IDLE,
                        isUserBusy = false,
                        wordsMastered = currentMastered,
                        latestScore = eval.score
                    )
                }
            }.onFailure { error ->
                _evaluationState.value = EvaluationUiState.Error(
                    error.localizedMessage
                        ?: appContext?.getString(R.string.error_evaluation_failed)
                        ?: "Evaluation failed"
                )
                _senseiMood.value = "idle"
                engagementManager?.trackEvent(
                    eventName = "pronunciation_eval_failed",
                    category = "error",
                    metadata = mapOf("message" to error.localizedMessage)
                )
            }
        }
    }

    fun sendRoleplayMessage(userMessage: String) {
        if (userMessage.isBlank()) return
        val currentDialog = _dialogMessages.value
        val activeScenario = _selectedScenario.value
        val userMsg = DialogMessage(
            id = "msg_${System.currentTimeMillis()}",
            speaker = activeScenario.userRole,
            text = userMessage,
            isUser = true,
            speakerRes = activeScenario.userRoleRes
        )
        _dialogMessages.value = currentDialog + userMsg

        viewModelScope.launch {
            _roleplayUiState.value = RoleplayUiState.Thinking
            _senseiMood.value = "thinking"

            val history = currentDialog.map { it.speaker to it.text }
            val result = coach.conductRoleplayTurn(
                language = activeScenario.language,
                scenarioTitle = activeScenario.title,
                conversationHistory = history,
                userSpokenMessage = userMessage
            )

            result.onSuccess { turn ->
                _roleplayUiState.value = RoleplayUiState.Idle
                _senseiMood.value = turn.senseiMood

                val senseiMsg = DialogMessage(
                    id = "msg_${System.currentTimeMillis() + 1}",
                    speaker = activeScenario.senseiRole,
                    text = turn.senseiReply,
                    phonetic = turn.phoneticGuide,
                    translation = turn.englishTranslation,
                    accentNote = turn.accentAndGrammarNote,
                    isUser = false,
                    speakerRes = activeScenario.senseiRoleRes
                )
                _dialogMessages.value = _dialogMessages.value + senseiMsg
                _suggestedReplies.value = turn.suggestedUserReplies

                val streakOutcome = engagementManager?.recordDailyStreakPractice("roleplay_turn")
                val latestStreakLong = streakOutcome?.first?.currentStreak?.toLong()
                if (latestStreakLong != null) {
                    repository.updateStreak(latestStreakLong)
                }

                profileState.value?.let { prof ->
                    val syncedProfile = if (latestStreakLong != null) {
                        prof.copy(currentStreak = maxOf(prof.currentStreak, latestStreakLong))
                    } else {
                        prof
                    }
                    repository.syncLeaderboardEntry(syncedProfile, 25L)
                }

                val turnCount = _dialogMessages.value.size
                engagementManager?.recordSuccessfulCoreOperation("roleplay_turn")
                engagementManager?.trackEvent(
                    eventName = "roleplay_turn_completed",
                    category = "roleplay",
                    metadata = mapOf(
                        "scenario" to _selectedScenario.value.title,
                        "language" to _selectedScenario.value.language,
                        "turns_count" to turnCount
                    )
                )

                // Log session if turn count is significant
                if (turnCount >= 4) {
                    repository.logSession(
                        scenarioTitle = _selectedScenario.value.title,
                        language = _selectedScenario.value.language,
                        turnsCount = turnCount.toLong(),
                        avgScore = 88L,
                        feedbackSummary = turn.accentAndGrammarNote
                    )
                    engagementManager?.recordPositiveMilestone(
                        milestoneName = "roleplay_session_milestone",
                        metadata = mapOf("scenario" to _selectedScenario.value.title, "turns" to turnCount),
                        isCelebratory = true,
                        isUserBusy = false,
                        wordsMastered = profileState.value?.wordsMastered ?: 0L
                    )
                }
            }.onFailure { err ->
                _roleplayUiState.value = RoleplayUiState.Error(
                    err.localizedMessage
                        ?: appContext?.getString(R.string.error_roleplay_failed)
                        ?: "Sensei could not respond."
                )
                _senseiMood.value = "idle"
                engagementManager?.trackEvent(
                    eventName = "roleplay_turn_failed",
                    category = "error",
                    metadata = mapOf("message" to err.localizedMessage)
                )
            }
        }
    }

    fun practiceVocabWord(item: VocabItem) {
        if (item.language.isNotBlank()) {
            _targetLanguage.value = item.language
        }
        val defaultFocus = appContext?.getString(R.string.coach_default_accent_focus)
            ?: "Vowel & Consonant clarity"
        val matching = PracticePhrase(
            id = item.id,
            targetText = item.word,
            phonetic = item.phonetic.ifBlank { item.word },
            translation = item.translation,
            difficulty = if (item.masteryLevel >= 4) "Advanced" else "Core",
            accentFocus = defaultFocus,
            accentFocusRes = R.string.coach_default_accent_focus
        )
        _selectedPhrase.value = matching
        _evaluationState.value = EvaluationUiState.Idle
        _currentTab.value = LearningTab.COACH
    }

    fun practiceLocalWord(word: VocabularyWord) {
        _targetLanguage.value = word.language
        val defaultFocus = appContext?.getString(R.string.coach_default_accent_focus)
            ?: "Vowel & Consonant clarity"
        val matching = PracticePhrase(
            id = word.remoteId.ifBlank { "local_${word.id}" },
            targetText = word.word,
            phonetic = word.phonetics.ifBlank { word.word },
            translation = word.translation,
            difficulty = if (word.masteryLevel >= 4) "Advanced" else "Core",
            accentFocus = defaultFocus,
            accentFocusRes = R.string.coach_default_accent_focus
        )
        _selectedPhrase.value = matching
        _evaluationState.value = EvaluationUiState.Idle
        _currentTab.value = LearningTab.COACH
    }

    fun reviewLocalWordMastery(word: VocabularyWord, remembered: Boolean) {
        viewModelScope.launch {
            repository.recordFlashcardMasteryReview(word, remembered)
            engagementManager?.trackEvent(
                eventName = if (remembered) "vocab_flashcard_mastered_step" else "vocab_flashcard_review_flagged",
                category = "vocab",
                metadata = mapOf("word" to word.word, "language" to word.language, "prev_level" to word.masteryLevel)
            )
        }
    }

    fun updateVocabScore(vocabId: String, newScore: Long) {
        viewModelScope.launch {
            repository.updateVocabScore(vocabId, newScore)
        }
    }

    fun trackEvent(eventName: String, category: String, metadata: Map<String, Any?> = emptyMap()) {
        engagementManager?.trackEvent(eventName, category, metadata)
    }

    fun onEngagementPromptAction(prompt: EngagementPrompt, action: PromptAction) {
        engagementManager?.handlePromptAction(prompt, action)
    }

    fun shareAppManually() {
        engagementManager?.launchShareIntent()
    }

    fun rateAppManually() {
        engagementManager?.openPlayStorePage()
    }

    fun dismissHomeDonationBanner() {
        engagementManager?.dismissHomeDonationBanner()
    }

    fun consumeOpenDonationSheetRequest() {
        engagementManager?.consumeOpenDonationSheetRequest()
    }

    fun canShowPostOperationSupportCard(score: Int): Boolean {
        return engagementManager?.canShowPostOperationSupportCard(score) ?: false
    }

    fun trackDonationSheetOpened(channel: com.example.donation.DonationChannel) {
        engagementManager?.trackEvent(
            eventName = "donation_sheet_opened",
            category = "donation",
            metadata = mapOf("channel" to channel.name)
        )
    }

    fun trackDonationChannelSelected(channel: com.example.donation.DonationChannel) {
        engagementManager?.trackEvent(
            eventName = "donation_channel_selected",
            category = "donation",
            metadata = mapOf("channel" to channel.name)
        )
    }

    fun refreshDailyEngagement() {
        engagementManager?.refreshDailyRetentionState()
    }

    fun trackStreakDialogOpened(currentStreak: Int) {
        engagementManager?.trackEvent(
            eventName = "streak_dialog_opened",
            category = "retention",
            metadata = mapOf("current_streak" to currentStreak)
        )
    }

    fun launchPlayDonation(context: Context, productId: String) {
        donationBillingManager?.launchDonationPurchase(context, productId)
    }

    fun openPayPalDonation(context: Context, amountUsd: String?) {
        donationBillingManager?.openPayPalDonation(context, amountUsd)
    }

    fun copyPayPalLink(context: Context, amountUsd: String?) {
        donationBillingManager?.copyPayPalLink(context, amountUsd)
    }

    fun sharePayPalLink(context: Context, amountUsd: String?) {
        donationBillingManager?.sharePayPalLink(context, amountUsd)
    }

    fun openKofiDonation(context: Context) {
        donationBillingManager?.openKofiDonation(context)
    }

    fun copyKofiLink(context: Context) {
        donationBillingManager?.copyKofiLink(context)
    }

    fun clearDonationStatus() {
        donationBillingManager?.clearStatusMessage()
    }

    fun refreshPurchases() {
        donationBillingManager?.refreshPurchases()
    }

    fun setAiCoachingMode(mode: AiCoachingMode) {
        aiSettingsManager?.setMode(mode)
        engagementManager?.trackEvent(
            eventName = "ai_mode_changed",
            category = "settings",
            metadata = mapOf("mode" to mode.name)
        )
    }

    fun saveByokApiKey(rawKey: String) {
        aiSettingsManager?.saveUserApiKey(rawKey)
        engagementManager?.trackEvent(
            eventName = if (rawKey.isBlank()) "byok_key_cleared" else "byok_key_saved",
            category = "settings"
        )
    }

    fun toggleEcoLiteMode(enabled: Boolean) {
        aiSettingsManager?.toggleEcoLiteMode(enabled)
        engagementManager?.trackEvent(
            eventName = "eco_lite_toggled",
            category = "settings",
            metadata = mapOf("enabled" to enabled.toString())
        )
    }

    fun claimGraceCredits(): Boolean {
        val claimed = aiSettingsManager?.claimGraceCredits() ?: false
        if (claimed) {
            engagementManager?.trackEvent(
                eventName = "grace_credits_claimed",
                category = "quota"
            )
        }
        return claimed
    }

    fun getValueReceipt(): com.example.ai.ValueReceiptSnapshot {
        return aiSettingsManager?.usageQuotaManager?.getValueReceipt()
            ?: com.example.ai.ValueReceiptSnapshot(0, 0, 0, 0, 0L)
    }

    fun completeDojoBlitzSession(pointsEarned: Int, phrasesCleared: Int) {
        viewModelScope.launch {
            val streakOutcome = if (phrasesCleared > 0) {
                engagementManager?.recordDailyStreakPractice("dojo_blitz")?.also { (state, _) ->
                    repository.updateStreak(state.currentStreak.toLong())
                }
            } else {
                null
            }
            if (pointsEarned > 0) {
                profileState.value?.let { prof ->
                    val syncedProfile = streakOutcome?.first?.currentStreak?.let { streakCount ->
                        prof.copy(currentStreak = streakCount.toLong())
                    } ?: prof
                    repository.syncLeaderboardEntry(syncedProfile, pointsEarned.toLong())
                }
            }
            engagementManager?.trackEvent(
                eventName = "dojo_blitz_completed",
                category = "game",
                metadata = mapOf(
                    "language" to _targetLanguage.value,
                    "points" to pointsEarned,
                    "phrases_cleared" to phrasesCleared
                )
            )
            if (phrasesCleared > 0) {
                engagementManager?.recordSuccessfulCoreOperation(
                    operationName = "dojo_blitz_game",
                    score = 90
                )
                engagementManager?.recordPositiveMilestone(
                    milestoneName = "dojo_blitz_victory",
                    metadata = mapOf("phrases_cleared" to phrasesCleared, "points" to pointsEarned),
                    isCelebratory = true,
                    isUserBusy = false,
                    wordsMastered = profileState.value?.wordsMastered ?: 0L,
                    latestScore = 90
                )
            }
        }
    }

    fun addCustomPhrase(
        language: String,
        targetText: String,
        phonetic: String,
        translation: String,
        accentFocus: String
    ) {
        val cleanTarget = targetText.trim()
        if (cleanTarget.isBlank()) return
        viewModelScope.launch {
            val insertedId = customPhraseDao?.insertPhrase(
                CustomPhraseEntity(
                    language = language,
                    targetText = cleanTarget,
                    phonetic = phonetic.trim(),
                    translation = translation.trim(),
                    accentFocus = accentFocus.trim()
                )
            ) ?: 0L
            val defaultFocus = appContext?.getString(R.string.coach_default_accent_focus) ?: "Natural cadence"
            val cleanFocus = accentFocus.trim()
            val created = PracticePhrase(
                id = "custom_phrase_$insertedId",
                targetText = cleanTarget,
                phonetic = phonetic.trim().ifBlank { cleanTarget },
                translation = translation.trim(),
                difficulty = "Custom",
                accentFocus = cleanFocus.ifBlank { defaultFocus },
                accentFocusRes = if (cleanFocus.isBlank()) R.string.coach_default_accent_focus else 0
            )
            _selectedPhrase.value = created
            _evaluationState.value = EvaluationUiState.Idle
            engagementManager?.trackEvent(
                eventName = "custom_phrase_created",
                category = "coach",
                metadata = mapOf("language" to language)
            )
        }
    }

    fun deleteCustomPhrase(phraseId: Int) {
        viewModelScope.launch {
            customPhraseDao?.deletePhraseById(phraseId)
            val fallback = getPhrasesForLanguage(_targetLanguage.value).firstOrNull()
            if (fallback != null && _selectedPhrase.value.id == "custom_phrase_$phraseId") {
                _selectedPhrase.value = fallback
            }
        }
    }

    fun addCustomScenario(
        title: String,
        language: String,
        senseiRole: String,
        userRole: String,
        openingLine: String
    ) {
        val cleanTitle = title.trim()
        val cleanOpening = openingLine.trim()
        if (cleanTitle.isBlank() || cleanOpening.isBlank()) return
        val defaultSensei = appContext?.getString(R.string.topic_pack_sensei_role) ?: "Sensei"
        val defaultUser = appContext?.getString(R.string.topic_pack_user_role) ?: "Traveler"
        val resolvedSenseiRole = senseiRole.trim().ifBlank { defaultSensei }
        val resolvedUserRole = userRole.trim().ifBlank { defaultUser }
        viewModelScope.launch {
            val insertedId = customScenarioDao?.insertScenario(
                CustomScenarioEntity(
                    title = cleanTitle,
                    language = language,
                    senseiRole = resolvedSenseiRole,
                    userRole = resolvedUserRole,
                    openingLine = cleanOpening
                )
            ) ?: 0L
            val scenario = RoleplayScenario(
                id = "custom_sc_$insertedId",
                title = cleanTitle,
                language = language,
                description = cleanTitle,
                senseiRole = resolvedSenseiRole,
                userRole = resolvedUserRole,
                initialMessage = cleanOpening,
                initialPhonetic = "",
                initialTranslation = ""
            )
            initScenario(scenario)
            engagementManager?.trackEvent(
                eventName = "custom_scenario_created",
                category = "roleplay",
                metadata = mapOf("language" to language)
            )
        }
    }

    private val _isGeneratingTopicPack = MutableStateFlow(false)
    val isGeneratingTopicPack: StateFlow<Boolean> = _isGeneratingTopicPack.asStateFlow()

    fun generateTopicLessonPack(
        topic: String,
        onComplete: (Boolean) -> Unit = {}
    ) {
        val cleanTopic = topic.trim()
        if (cleanTopic.isBlank() || _isGeneratingTopicPack.value) return
        val language = _targetLanguage.value
        viewModelScope.launch {
            _isGeneratingTopicPack.value = true
            try {
                val pack = coach.generateByokTopicPack(language, cleanTopic)
                    .getOrElse { coach.buildLocalTopicPackFallback(language, cleanTopic) }
                var firstPhrase: PracticePhrase? = null
                for ((idx, item) in pack.phrases.withIndex()) {
                    val insertedId = customPhraseDao?.insertPhrase(
                        CustomPhraseEntity(
                            language = language,
                            targetText = item.targetText,
                            phonetic = item.phonetic,
                            translation = item.translation,
                            accentFocus = item.accentFocus
                        )
                    ) ?: (idx + 1).toLong()
                    if (firstPhrase == null) {
                        val defaultFocus = appContext?.getString(R.string.coach_default_accent_focus) ?: "Natural cadence"
                        firstPhrase = PracticePhrase(
                            id = "custom_phrase_$insertedId",
                            targetText = item.targetText,
                            phonetic = item.phonetic.ifBlank { item.targetText },
                            translation = item.translation,
                            difficulty = "Custom",
                            accentFocus = item.accentFocus.ifBlank { defaultFocus },
                            accentFocusRes = if (item.accentFocus.isBlank()) R.string.coach_default_accent_focus else 0
                        )
                    }
                }
                firstPhrase?.let { created ->
                    _selectedPhrase.value = created
                    _evaluationState.value = EvaluationUiState.Idle
                }

                val scenarioId = customScenarioDao?.insertScenario(
                    CustomScenarioEntity(
                        title = pack.scenarioTitle,
                        language = language,
                        senseiRole = pack.senseiRole,
                        userRole = pack.userRole,
                        openingLine = pack.openingLine
                    )
                ) ?: 1L
                val createdScenario = RoleplayScenario(
                    id = "custom_sc_$scenarioId",
                    title = pack.scenarioTitle,
                    language = language,
                    description = pack.scenarioTitle,
                    senseiRole = pack.senseiRole,
                    userRole = pack.userRole,
                    initialMessage = pack.openingLine,
                    initialPhonetic = "",
                    initialTranslation = ""
                )
                initScenario(createdScenario)
                engagementManager?.trackEvent(
                    eventName = "topic_pack_generated",
                    category = "ai_pack",
                    metadata = mapOf(
                        "language" to language,
                        "topic" to cleanTopic,
                        "used_byok" to pack.usedByokCloud
                    )
                )
                onComplete(true)
            } catch (_: Exception) {
                onComplete(false)
            } finally {
                _isGeneratingTopicPack.value = false
            }
        }
    }

    fun deleteCustomScenario(scenarioId: Int) {
        viewModelScope.launch {
            customScenarioDao?.deleteScenarioById(scenarioId)
            if (_selectedScenario.value.id == "custom_sc_$scenarioId") {
                val fallback = defaultScenarios.firstOrNull {
                    it.language.equals(_targetLanguage.value, ignoreCase = true)
                } ?: defaultScenarios.first()
                initScenario(fallback)
            }
        }
    }

    fun setStreakReminderEnabled(enabled: Boolean, hourOfDay: Int = _streakReminderHour.value) {
        _streakReminderEnabled.value = enabled
        _streakReminderHour.value = hourOfDay
        appContext?.let { ctx ->
            if (enabled) {
                ReminderScheduler.scheduleEveningReminder(ctx, hourOfDay, 0)
            } else {
                ReminderScheduler.cancelEveningReminder(ctx)
            }
        }
        engagementManager?.trackEvent(
            eventName = "streak_reminder_toggled",
            category = "retention",
            metadata = mapOf("enabled" to enabled, "hour" to hourOfDay)
        )
    }

    fun sharePronunciationMilestone(phraseText: String, phoneticText: String, score: Int) {
        val headline = appContext?.getString(R.string.share_card_headline_score)
            ?: "Pronunciation Mastery"
        engagementManager?.launchMilestoneShareCard(
            headline = headline,
            phraseText = phraseText,
            phoneticText = phoneticText,
            score = score
        )
    }

    fun shareStreakMilestone(streakDays: Int) {
        val headline = appContext?.getString(R.string.share_card_headline_streak)
            ?: "Dojo Streak Milestone"
        val phraseText = appContext?.getString(R.string.share_card_streak_format, streakDays)
            ?: "$streakDays-Day Streak"
        engagementManager?.launchMilestoneShareCard(
            headline = headline,
            phraseText = phraseText,
            phoneticText = localizedLanguageNameSimple(_targetLanguage.value),
            score = null
        )
    }

    private fun localizedLanguageNameSimple(language: String): String {
        val resId = when (language.lowercase()) {
            "japanese", "ja" -> R.string.lang_japanese
            "spanish", "es" -> R.string.lang_spanish
            "french", "fr" -> R.string.lang_french
            "german", "de" -> R.string.lang_german
            "korean", "ko" -> R.string.lang_korean
            "mandarin", "chinese", "zh" -> R.string.lang_mandarin
            "italian", "it" -> R.string.lang_italian
            else -> R.string.lang_english
        }
        return appContext?.getString(resId) ?: language
    }

    override fun onCleared() {
        super.onCleared()
        donationBillingManager?.destroy()
    }

    fun getPhrasesForLanguage(lang: String): List<PracticePhrase> {
        return when (lang.lowercase()) {
            "japanese", "ja" -> defaultPhrasesJapanese
            "french", "fr" -> defaultPhrasesFrench
            "spanish", "es" -> defaultPhrasesSpanish
            "german", "de" -> defaultPhrasesGerman
            "korean", "ko" -> defaultPhrasesKorean
            "mandarin", "chinese", "zh" -> defaultPhrasesMandarin
            "italian", "it" -> defaultPhrasesItalian
            else -> defaultPhrasesEnglish
        }
    }

    companion object {
        fun provideFactory(
            repository: SenseiRepository,
            engagementManager: EngagementManager? = null,
            donationBillingManager: DonationBillingManager? = null,
            aiSettingsManager: AiSettingsManager? = null,
            cacheDao: CachedEvaluationDao? = null,
            appContext: Context? = null,
            customPhraseDao: CustomPhraseDao? = null,
            customScenarioDao: CustomScenarioDao? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(
                    repository = repository,
                    engagementManager = engagementManager,
                    donationBillingManager = donationBillingManager,
                    aiSettingsManager = aiSettingsManager,
                    cacheDao = cacheDao,
                    appContext = appContext,
                    customPhraseDao = customPhraseDao,
                    customScenarioDao = customScenarioDao
                ) as T
            }
        }

        val defaultPhrasesJapanese = listOf(
            PracticePhrase("ja_1", "こんにちは、お元気ですか？", "Konnichiwa, o-genki desu ka?", "Hello, how are you?", "Beginner", "Flat pitch on 'Konnichiwa'", R.string.phrase_ja_1_translation, R.string.phrase_ja_1_focus),
            PracticePhrase("ja_2", "これをください。", "Kore o kudasai.", "I will take this, please.", "Beginner", "Light 'r' tongue flap", R.string.phrase_ja_2_translation, R.string.phrase_ja_2_focus),
            PracticePhrase("ja_3", "ありがとうございます！", "Arigatou gozaimasu!", "Thank you very much!", "Beginner", "Long 'ou' vowel", R.string.phrase_ja_3_translation, R.string.phrase_ja_3_focus),
            PracticePhrase("ja_4", "すみません、駅はどこですか？", "Sumimasen, eki wa doko desu ka?", "Excuse me, where is the station?", "Intermediate", "Rising polite inflection", R.string.phrase_ja_4_translation, R.string.phrase_ja_4_focus),
            PracticePhrase("ja_5", "とても美味しいです！", "Totemo oishii desu!", "It is very delicious!", "Beginner", "Clean 'desu' unvoiced 'u'", R.string.phrase_ja_5_translation, R.string.phrase_ja_5_focus)
        )

        val defaultPhrasesFrench = listOf(
            PracticePhrase("fr_1", "Bonjour, enchanté de vous rencontrer.", "Bohn-zhoor, ahn-shahn-tay", "Hello, pleased to meet you.", "Beginner", "Soft nasal 'en' & 'on'", R.string.phrase_fr_1_translation, R.string.phrase_fr_1_focus),
            PracticePhrase("fr_2", "Un croissant et un café, s'il vous plaît.", "Uhn krwa-sahn ay uhn ka-fay", "A croissant and a coffee, please.", "Beginner", "Throaty French 'r'", R.string.phrase_fr_2_translation, R.string.phrase_fr_2_focus),
            PracticePhrase("fr_3", "Où se trouvent les toilettes ?", "Oo suh troov lay twa-let", "Where is the restroom?", "Beginner", "Smooth liaison", R.string.phrase_fr_3_translation, R.string.phrase_fr_3_focus),
            PracticePhrase("fr_4", "C'est absolument magnifique !", "Say tab-so-loo-mahn ma-nyee-feek", "It is absolutely magnificent!", "Intermediate", "Pure 'u' lips rounded", R.string.phrase_fr_4_translation, R.string.phrase_fr_4_focus)
        )

        val defaultPhrasesSpanish = listOf(
            PracticePhrase("es_1", "¡Hola! ¿Cómo estás hoy?", "OH-lah, KOH-moh es-TAHS oy?", "Hello! How are you today?", "Beginner", "Clear open vowels", R.string.phrase_es_1_translation, R.string.phrase_es_1_focus),
            PracticePhrase("es_2", "Una mesa para dos, por favor.", "OO-nah MEH-sah PAH-rah dohs", "A table for two, please.", "Beginner", "Soft single tap 'r'", R.string.phrase_es_2_translation, R.string.phrase_es_2_focus),
            PracticePhrase("es_3", "El ferrocarril rápido.", "ehl feh-rroh-kah-RREEL RRAH-pee-doh", "The fast railway.", "Advanced", "Double rolled 'rr' trill", R.string.phrase_es_3_translation, R.string.phrase_es_3_focus),
            PracticePhrase("es_4", "La cuenta, por favor.", "lah KWEHN-tah, por fah-VOR", "The bill, please.", "Beginner", "Stress on final syllables", R.string.phrase_es_4_translation, R.string.phrase_es_4_focus)
        )

        val defaultPhrasesGerman = listOf(
            PracticePhrase("de_1", "Guten Morgen, wie geht es Ihnen?", "GOO-ten MOR-gen, vee gayt es EE-nen", "Good morning, how are you?", "Beginner", "Long 'ee' vowel in Ihnen", R.string.phrase_de_1_translation, R.string.phrase_de_1_focus),
            PracticePhrase("de_2", "Ich möchte bitte einen Kaffee.", "Ikh MUKH-teh BIH-teh", "I would like a coffee, please.", "Intermediate", "Soft palate 'ch' fricative", R.string.phrase_de_2_translation, R.string.phrase_de_2_focus),
            PracticePhrase("de_3", "Dankeschön für Ihre Hilfe!", "DAHN-keh-shun foor EE-reh", "Thank you very much for your help!", "Beginner", "Rounded 'ö' and 'ü'", R.string.phrase_de_3_translation, R.string.phrase_de_3_focus)
        )

        val defaultPhrasesKorean = listOf(
            PracticePhrase("ko_1", "안녕하세요, 반갑습니다!", "An-nyeong-ha-se-yo, ban-gap-seum-ni-da!", "Hello, nice to meet you!", "Beginner", "Polite melodic cadence", R.string.phrase_ko_1_translation, R.string.phrase_ko_1_focus),
            PracticePhrase("ko_2", "이것 얼마예요?", "I-geot eol-ma-ye-yo?", "How much is this?", "Beginner", "Soft 'l' sound in eol-ma", R.string.phrase_ko_2_translation, R.string.phrase_ko_2_focus),
            PracticePhrase("ko_3", "맛있게 드세요!", "Ma-sit-ge deu-se-yo!", "Enjoy your meal!", "Intermediate", "Aspirated vs tense consonants", R.string.phrase_ko_3_translation, R.string.phrase_ko_3_focus)
        )

        val defaultPhrasesMandarin = listOf(
            PracticePhrase("zh_1", "你好，很高兴认识你！", "Nǐ hǎo, hěn gāoxìng rènshí nǐ!", "Hello, nice to meet you!", "Beginner", "3rd tone sandhi on Nǐ hǎo", R.string.phrase_zh_1_translation, R.string.phrase_zh_1_focus),
            PracticePhrase("zh_2", "请问这个多少钱？", "Qǐngwèn zhège duōshǎo qián?", "Excuse me, how much is this?", "Intermediate", "4th tone on zhè, 2nd tone on qián", R.string.phrase_zh_2_translation, R.string.phrase_zh_2_focus),
            PracticePhrase("zh_3", "太好吃了，谢谢！", "Tài hǎochī le, xièxie!", "Extremely delicious, thanks!", "Beginner", "Falling 4th tone on Tài and xiè", R.string.phrase_zh_3_translation, R.string.phrase_zh_3_focus)
        )

        val defaultPhrasesItalian = listOf(
            PracticePhrase("it_1", "Buongiorno! Come sta?", "Bwon-JOR-noh! KOH-meh stah?", "Good day! How are you?", "Beginner", "Musical syllable rhythm", R.string.phrase_it_1_translation, R.string.phrase_it_1_focus),
            PracticePhrase("it_2", "Un gelato al pistacchio, per favore.", "Oon jeh-LAH-toh ahl pees-TAHK-kyoh", "A pistachio gelato, please.", "Beginner", "Double consonant 'cch' pause", R.string.phrase_it_2_translation, R.string.phrase_it_2_focus),
            PracticePhrase("it_3", "Grazie mille per tutto!", "GRAHT-syeh MEEL-leh pehr TOOT-toh", "A thousand thanks for everything!", "Beginner", "Lively dental 't' & 'z'", R.string.phrase_it_3_translation, R.string.phrase_it_3_focus)
        )

        val defaultPhrasesEnglish = listOf(
            PracticePhrase("en_1", "Could you please tell me where the nearest metro is?", "kʊd juː pliːz tɛl miː", "Direction request", "Intermediate", "Voiced 'th' in the", R.string.phrase_en_1_translation, R.string.phrase_en_1_focus),
            PracticePhrase("en_2", "I'd love to schedule a follow-up interview next week.", "aɪd lʌv tuː ˈskɛdʒuːl", "Professional career query", "Advanced", "Flapped 'd' in schedule", R.string.phrase_en_2_translation, R.string.phrase_en_2_focus)
        )

        val defaultScenarios = listOf(
            RoleplayScenario(
                id = "sc_tokyo_cafe",
                title = "Tokyo Café Order",
                language = "Japanese",
                description = "Order signature matcha latte and pastries at an artisanal Shibuya café.",
                senseiRole = "Café Barista",
                userRole = "Customer",
                initialMessage = "いらっしゃいませ！ご注文はお決まりですか？",
                initialPhonetic = "Irasshaimase! Go-chuumon wa o-kimari desu ka?",
                initialTranslation = "Welcome! Have you decided on your order?",
                titleRes = R.string.scenario_tokyo_cafe_title,
                senseiRoleRes = R.string.scenario_tokyo_cafe_sensei,
                userRoleRes = R.string.scenario_tokyo_cafe_user,
                initialTranslationRes = R.string.scenario_tokyo_cafe_init_translation
            ),
            RoleplayScenario(
                id = "sc_paris_boulangerie",
                title = "Parisian Boulangerie",
                language = "French",
                description = "Order warm croissants, baguettes, and ask for recommendations from the master baker.",
                senseiRole = "Master Baker",
                userRole = "Customer",
                initialMessage = "Bonjour ! Qu'est-ce qui vous ferait plaisir aujourd'hui ?",
                initialPhonetic = "Bohn-zhoor! Kess-kee voo feh-reh pleh-zeer oh-zhoor-dwee?",
                initialTranslation = "Hello! What would you like today?",
                titleRes = R.string.scenario_paris_boulangerie_title,
                senseiRoleRes = R.string.scenario_paris_boulangerie_sensei,
                userRoleRes = R.string.scenario_paris_boulangerie_user,
                initialTranslationRes = R.string.scenario_paris_boulangerie_init_translation
            ),
            RoleplayScenario(
                id = "sc_madrid_tapas",
                title = "Madrid Tapas Bar",
                language = "Spanish",
                description = "Savor traditional tapas and drinks at a bustling tavern in Madrid.",
                senseiRole = "Tavern Host",
                userRole = "Patron",
                initialMessage = "¡Buenas tardes! ¿Qué os apetece tomar para empezar?",
                initialPhonetic = "BWEH-nahs TAR-dehs! ¿Keh ohs ah-peh-TEH-seh toh-MAR?",
                initialTranslation = "Good afternoon! What can I get started for you to drink?",
                titleRes = R.string.scenario_madrid_tapas_title,
                senseiRoleRes = R.string.scenario_madrid_tapas_sensei,
                userRoleRes = R.string.scenario_madrid_tapas_user,
                initialTranslationRes = R.string.scenario_madrid_tapas_init_translation
            ),
            RoleplayScenario(
                id = "sc_seoul_market",
                title = "Seoul Night Market",
                language = "Korean",
                description = "Explore vibrant street food stalls and order delicious tteokbokki.",
                senseiRole = "Street Food Chef",
                userRole = "Visitor",
                initialMessage = "어서 오세요! 따끈따끈한 떡볶이 맛보고 가세요!",
                initialPhonetic = "Eo-seo o-se-yo! Tta-kkeun-tta-kkeun-han tteok-bok-ki mat-bo-go ga-se-yo!",
                initialTranslation = "Welcome! Come try our steaming hot tteokbokki!",
                titleRes = R.string.scenario_seoul_market_title,
                senseiRoleRes = R.string.scenario_seoul_market_sensei,
                userRoleRes = R.string.scenario_seoul_market_user,
                initialTranslationRes = R.string.scenario_seoul_market_init_translation
            ),
            RoleplayScenario(
                id = "sc_rome_gelato",
                title = "Florence Gelateria",
                language = "Italian",
                description = "Order handcrafted gelato cones with authentic flavors.",
                senseiRole = "Gelatiere",
                userRole = "Customer",
                initialMessage = "Ciao! Cono o coppetta? Quali gusti preferisci?",
                initialPhonetic = "CHAH-oh! KOH-noh oh kohp-PEHT-tah? KWAH-lee GOOS-tee?",
                initialTranslation = "Hi! Cone or cup? Which flavors would you like?",
                titleRes = R.string.scenario_rome_gelato_title,
                senseiRoleRes = R.string.scenario_rome_gelato_sensei,
                userRoleRes = R.string.scenario_rome_gelato_user,
                initialTranslationRes = R.string.scenario_rome_gelato_init_translation
            ),
            RoleplayScenario(
                id = "sc_berlin_tech",
                title = "Berlin Tech Meetup",
                language = "German",
                description = "Chat about technology and software projects over Fritz-Kola.",
                senseiRole = "Tech Lead",
                userRole = "Engineer",
                initialMessage = "Hallo! Schön, dass du da bist. An welchem Projekt arbeitest du?",
                initialPhonetic = "HAH-loh! Shoon dahss doo dah bist. Ahn VEHL-khem Proh-YEHKT?",
                initialTranslation = "Hello! Great to have you here. What project are you working on?",
                titleRes = R.string.scenario_berlin_tech_title,
                senseiRoleRes = R.string.scenario_berlin_tech_sensei,
                userRoleRes = R.string.scenario_berlin_tech_user,
                initialTranslationRes = R.string.scenario_berlin_tech_init_translation
            ),
            RoleplayScenario(
                id = "sc_shanghai_teahouse",
                title = "Shanghai Teahouse",
                language = "Mandarin",
                description = "Taste artisanal oolong and jasmine teas at a traditional Shanghai teahouse.",
                senseiRole = "Tea Master",
                userRole = "Guest",
                initialMessage = "欢迎光临！今天您想品尝哪一款茗茶？",
                initialPhonetic = "Huānyíng guānglín! Jīntiān nín xiǎng pǐncháng nǎ yī kuǎn míngchá?",
                initialTranslation = "Welcome! Which tea would you like to taste today?",
                titleRes = R.string.scenario_shanghai_tea_title,
                senseiRoleRes = R.string.scenario_shanghai_tea_sensei,
                userRoleRes = R.string.scenario_shanghai_tea_user,
                initialTranslationRes = R.string.scenario_shanghai_tea_init_translation
            ),
            RoleplayScenario(
                id = "sc_london_interview",
                title = "London Studio Coffee",
                language = "English",
                description = "Discuss creative projects and career goals over flat whites in London.",
                senseiRole = "Project Host",
                userRole = "Creator",
                initialMessage = "Good morning! Tell me what excites you about your next project.",
                initialPhonetic = "ɡʊd ˈmɔːnɪŋ! tɛl miː wɒt ɪkˈsaɪts juː",
                initialTranslation = "Good morning! Tell me what excites you about your next project.",
                titleRes = R.string.scenario_london_interview_title,
                senseiRoleRes = R.string.scenario_london_interview_sensei,
                userRoleRes = R.string.scenario_london_interview_user,
                initialTranslationRes = R.string.scenario_london_interview_init_translation
            )
        )
    }
}
