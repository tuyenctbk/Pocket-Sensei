package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.local.SenseiLocalDatabase
import com.example.data.local.SpacedRepetitionScheduler
import com.example.data.local.VocabularyWord
import com.example.data.local.VocabularyWordDao
import com.example.data.model.LeaderboardEntry
import com.example.data.model.PracticeSession
import com.example.data.model.UserProfile
import com.example.data.model.VocabItem
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class SenseiRepository(
    private val db: FirebaseFirestore?,
    private val localDao: VocabularyWordDao? = null,
    private val defaultStudentName: String = "Sensei Student"
) {

    constructor(context: Context) : this(
        db = if (FirebaseApp.getApps(context.applicationContext).isNotEmpty()) {
            FirebaseFirestore.getInstance(
                context.applicationContext.getString(R.string.firestore_database_id)
            )
        } else {
            null
        },
        localDao = SenseiLocalDatabase.getInstance(context.applicationContext).vocabularyWordDao(),
        defaultStudentName = context.applicationContext.getString(R.string.profile_default_name)
    )

    private val auth: FirebaseAuth? = runCatching {
        if (db != null) Firebase.auth else null
    }.getOrNull()

    private val localProfileFlow = MutableStateFlow(
        UserProfile(
            userId = "guest_local",
            displayName = defaultStudentName,
            targetLanguage = "Japanese",
            currentStreak = 1L,
            wordsMastered = 0L,
            totalMinutesPracticed = 5L,
            avgPronunciationScore = 85L
        )
    )

    private val localSessionsFlow = MutableStateFlow<List<PracticeSession>>(emptyList())

    private val localLeaderboardSeed = listOf(
        LeaderboardEntry(id = "sensei_kenji", userId = "sensei_kenji", displayName = "Kenji Sato", targetLanguage = "Japanese", leagueTier = "Black Belt Sensei", totalXp = 2840L, streakDays = 21L, avgAccuracy = 94L),
        LeaderboardEntry(id = "sensei_marie", userId = "sensei_marie", displayName = "Marie Laurent", targetLanguage = "French", leagueTier = "Gold Tier", totalXp = 1920L, streakDays = 14L, avgAccuracy = 91L),
        LeaderboardEntry(id = "sensei_mateo", userId = "sensei_mateo", displayName = "Mateo Silva", targetLanguage = "Spanish", leagueTier = "Gold Tier", totalXp = 1640L, streakDays = 11L, avgAccuracy = 89L),
        LeaderboardEntry(id = "sensei_minji", userId = "sensei_minji", displayName = "Min-ji Park", targetLanguage = "Korean", leagueTier = "Silver Tier", totalXp = 1120L, streakDays = 8L, avgAccuracy = 88L),
        LeaderboardEntry(id = "sensei_lukas", userId = "sensei_lukas", displayName = "Lukas Weber", targetLanguage = "German", leagueTier = "Bronze Tier", totalXp = 640L, streakDays = 5L, avgAccuracy = 84L)
    )

    private val isCloudSignedIn: Boolean
        get() = db != null && auth?.currentUser != null

    private fun requireUserId(): String {
        return auth?.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    suspend fun getOrCreateProfile(displayName: String, targetLanguage: String = "Japanese"): Result<UserProfile> {
        if (!isCloudSignedIn) {
            val cleanName = displayName.ifBlank { defaultStudentName }
            val updated = localProfileFlow.value.copy(
                displayName = cleanName,
                targetLanguage = targetLanguage
            )
            localProfileFlow.value = updated
            return Result.success(updated)
        }
        val uid = requireUserId()
        val firestore = db!!
        val docRef = firestore.collection("users").document(uid)
        return try {
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                val profile = snapshot.toObject(UserProfile::class.java)
                Result.success(profile ?: UserProfile(userId = uid, displayName = displayName, targetLanguage = targetLanguage))
            } else {
                val cleanName = displayName.ifBlank { defaultStudentName }
                val initialProfile = mapOf(
                    "userId" to uid,
                    "displayName" to cleanName,
                    "targetLanguage" to targetLanguage,
                    "currentStreak" to 0L,
                    "wordsMastered" to 0L,
                    "totalMinutesPracticed" to 5L,
                    "avgPronunciationScore" to 85L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(initialProfile).await()
                val created = docRef.get().await().toObject(UserProfile::class.java)
                    ?: UserProfile(userId = uid, displayName = cleanName, targetLanguage = targetLanguage)
                Result.success(created)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    fun observeProfile(): Flow<UserProfile?> = flow {
        if (!isCloudSignedIn) {
            emitAll(localProfileFlow)
            return@flow
        }
        val uid = auth?.currentUser?.uid ?: "unauthenticated"
        val firestore = db!!
        val path = "users/$uid"
        emitAll(
            firestore.collection("users").document(uid).snapshots()
                .map { it.toObject(UserProfile::class.java) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth?.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun updateLearningLanguage(language: String): Result<Unit> {
        if (!isCloudSignedIn) {
            localProfileFlow.value = localProfileFlow.value.copy(targetLanguage = language)
            return Result.success(Unit)
        }
        val uid = requireUserId()
        val docRef = db!!.collection("users").document(uid)
        return try {
            docRef.update(
                mapOf(
                    "targetLanguage" to language,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateStreak(streakDays: Long): Result<Unit> {
        val safeStreak = streakDays.coerceAtLeast(0L)
        if (!isCloudSignedIn) {
            localProfileFlow.value = localProfileFlow.value.copy(currentStreak = safeStreak)
            return Result.success(Unit)
        }
        val uid = auth?.currentUser?.uid ?: return Result.success(Unit)
        val firestore = db!!
        val docRef = firestore.collection("users").document(uid)
        return try {
            if (docRef.get().await().exists()) {
                docRef.update(
                    mapOf(
                        "currentStreak" to safeStreak,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            val leaderboardRef = firestore.collection("leaderboard").document(uid)
            if (leaderboardRef.get().await().exists()) {
                leaderboardRef.update(
                    mapOf(
                        "streakDays" to safeStreak,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun incrementPracticeStats(minutes: Long, sessionAvgScore: Long): Result<Unit> {
        if (!isCloudSignedIn) {
            val current = localProfileFlow.value
            val currentAvg = current.avgPronunciationScore
            val calculatedAvg = if (currentAvg > 0) ((currentAvg * 3 + sessionAvgScore) / 4) else sessionAvgScore
            localProfileFlow.value = current.copy(
                totalMinutesPracticed = current.totalMinutesPracticed + minutes,
                avgPronunciationScore = calculatedAvg
            )
            return Result.success(Unit)
        }
        val uid = requireUserId()
        val docRef = db!!.collection("users").document(uid)
        return try {
            val snapshot = docRef.get().await()
            val currentAvg = snapshot.getLong("avgPronunciationScore") ?: 80L
            val calculatedAvg = if (currentAvg > 0) ((currentAvg * 3 + sessionAvgScore) / 4) else sessionAvgScore
            docRef.update(
                mapOf(
                    "totalMinutesPracticed" to FieldValue.increment(minutes),
                    "avgPronunciationScore" to calculatedAvg,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeLocalVocabulary(): Flow<List<VocabularyWord>> {
        return localDao?.observeAllWords() ?: emptyFlow()
    }

    fun observeVocab(): Flow<List<VocabItem>> = flow {
        if (!isCloudSignedIn) {
            emitAll(
                (localDao?.observeAllWords() ?: emptyFlow()).map { words ->
                    words.map { w ->
                        VocabItem(
                            id = w.remoteId.ifBlank { "local_${w.id}" },
                            userId = "guest_local",
                            word = w.word,
                            translation = w.translation,
                            phonetic = w.phonetics,
                            language = w.language,
                            masteryLevel = w.masteryLevel.toLong(),
                            pronunciationScore = w.pronunciationScore.toLong()
                        )
                    }
                }
            )
            return@flow
        }
        val uid = auth?.currentUser?.uid ?: "unauthenticated"
        val path = "users/$uid/vocab"
        emitAll(
            db!!.collection("users").document(uid).collection("vocab")
                .snapshots()
                .map { snapshot ->
                    val items = snapshot.toObjects(VocabItem::class.java)
                    localDao?.let { dao ->
                        val localWords = items.map { item ->
                            val existing = dao.findByWordAndLanguage(item.word, item.language)
                            VocabularyWord(
                                id = existing?.id ?: 0,
                                word = item.word,
                                translation = item.translation,
                                phonetics = item.phonetic,
                                masteryLevel = item.masteryLevel.toInt().coerceIn(1, 5),
                                language = item.language,
                                pronunciationScore = item.pronunciationScore.toInt().coerceIn(0, 100),
                                practiceCount = existing?.practiceCount ?: 1,
                                remoteId = item.id,
                                lastPracticedAt = item.updatedAt?.toDate()?.time
                                    ?: existing?.lastPracticedAt
                                    ?: System.currentTimeMillis(),
                                nextReviewAt = existing?.nextReviewAt ?: System.currentTimeMillis(),
                                intervalDays = existing?.intervalDays ?: 1
                            )
                        }
                        if (localWords.isNotEmpty()) {
                            dao.insertOrUpdateAll(localWords)
                        }
                    }
                    items
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth?.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun addVocabItem(
        word: String,
        translation: String,
        phonetic: String,
        language: String,
        pronunciationScore: Long
    ): Result<String> {
        val masteryInt = masteryLevelForScore(pronunciationScore.toInt())
        val existingLocal = localDao?.findByWordAndLanguage(word, language)
        val effectiveMastery = maxOf(existingLocal?.masteryLevel ?: 1, masteryInt)
        val effectiveScore = maxOf(existingLocal?.pronunciationScore ?: 0, pronunciationScore.toInt().coerceIn(0, 100))

        val nowMs = System.currentTimeMillis()
        val schedule = SpacedRepetitionScheduler.computeScheduleForScore(
            currentMastery = effectiveMastery,
            currentIntervalDays = existingLocal?.intervalDays ?: 1,
            pronunciationScore = effectiveScore,
            nowMs = nowMs
        )

        // Persist immediately to Room so offline practice updates VocabularyWord without waiting for network.
        localDao?.let { dao ->
            dao.insertOrUpdate(
                VocabularyWord(
                    id = existingLocal?.id ?: 0,
                    word = word,
                    translation = translation.ifBlank { existingLocal?.translation.orEmpty() },
                    phonetics = phonetic.ifBlank { existingLocal?.phonetics.orEmpty() },
                    masteryLevel = effectiveMastery,
                    language = language,
                    pronunciationScore = effectiveScore,
                    practiceCount = (existingLocal?.practiceCount ?: 0) + 1,
                    remoteId = existingLocal?.remoteId.orEmpty(),
                    lastPracticedAt = nowMs,
                    nextReviewAt = schedule.nextReviewAtMs,
                    intervalDays = schedule.nextIntervalDays
                )
            )
        }

        if (!isCloudSignedIn) {
            if (pronunciationScore >= 85) {
                localProfileFlow.value = localProfileFlow.value.copy(
                    wordsMastered = localProfileFlow.value.wordsMastered + 1
                )
            }
            return Result.success(existingLocal?.remoteId.orEmpty())
        }
        val uid = auth?.currentUser?.uid
            ?: return Result.success(existingLocal?.remoteId.orEmpty())
        val firestore = db!!
        val docId = existingLocal?.remoteId?.takeIf { it.isNotBlank() }
            ?: "vocab_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
        val docRef = firestore.collection("users").document(uid).collection("vocab").document(docId)
        val payload = mutableMapOf<String, Any>(
            "userId" to uid,
            "word" to word,
            "translation" to translation,
            "language" to language,
            "masteryLevel" to effectiveMastery.toLong(),
            "pronunciationScore" to effectiveScore.toLong(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (existingLocal?.remoteId.isNullOrBlank()) {
            payload["createdAt"] = FieldValue.serverTimestamp()
        }
        if (phonetic.isNotBlank()) {
            payload["phonetic"] = phonetic
        }
        return try {
            docRef.set(payload, com.google.firebase.firestore.SetOptions.merge()).await()
            localDao?.let { dao ->
                val latestLocal = dao.findByWordAndLanguage(word, language)
                if (latestLocal != null && latestLocal.remoteId.isBlank()) {
                    dao.updateWord(latestLocal.copy(remoteId = docId))
                }
            }
            // Increment mastered words if high score and profile exists
            if (pronunciationScore >= 85) {
                val userDoc = db.collection("users").document(uid)
                if (userDoc.get().await().exists()) {
                    userDoc.update(
                        mapOf(
                            "wordsMastered" to FieldValue.increment(1),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun recordFlashcardMasteryReview(localWord: VocabularyWord, remembered: Boolean): Result<Unit> {
        val nowMs = System.currentTimeMillis()
        val schedule = SpacedRepetitionScheduler.computeNextSchedule(
            currentMastery = localWord.masteryLevel,
            currentIntervalDays = localWord.intervalDays,
            remembered = remembered,
            nowMs = nowMs
        )
        val nextLevel = schedule.nextMasteryLevel
        val adjustedScore = if (remembered) {
            maxOf(localWord.pronunciationScore, nextLevel * 19).coerceIn(0, 100)
        } else {
            minOf(localWord.pronunciationScore, 65).coerceIn(0, 100)
        }

        localDao?.recordPracticeAttempt(
            id = localWord.id,
            masteryLevel = nextLevel,
            pronunciationScore = adjustedScore,
            practicedAt = nowMs,
            nextReviewAt = schedule.nextReviewAtMs,
            intervalDays = schedule.nextIntervalDays
        )

        val uid = if (isCloudSignedIn) auth?.currentUser?.uid else null
        if (uid == null || localWord.remoteId.isBlank()) return Result.success(Unit)
        val docRef = db!!.collection("users").document(uid).collection("vocab").document(localWord.remoteId)
        return try {
            docRef.update(
                mapOf(
                    "masteryLevel" to nextLevel.toLong(),
                    "pronunciationScore" to adjustedScore.toLong(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateVocabScore(vocabId: String, newScore: Long): Result<Unit> {
        val newLevel = masteryLevelForScore(newScore.toInt()).toLong()
        if (!isCloudSignedIn) {
            localDao?.findByRemoteId(vocabId)?.let { localWord ->
                val nowMs = System.currentTimeMillis()
                val schedule = SpacedRepetitionScheduler.computeScheduleForScore(
                    currentMastery = newLevel.toInt(),
                    currentIntervalDays = localWord.intervalDays,
                    pronunciationScore = newScore.toInt(),
                    nowMs = nowMs
                )
                localDao.recordPracticeAttempt(
                    id = localWord.id,
                    masteryLevel = newLevel.toInt(),
                    pronunciationScore = newScore.toInt().coerceIn(0, 100),
                    practicedAt = nowMs,
                    nextReviewAt = schedule.nextReviewAtMs,
                    intervalDays = schedule.nextIntervalDays
                )
            }
            return Result.success(Unit)
        }
        val uid = requireUserId()
        val docRef = db!!.collection("users").document(uid).collection("vocab").document(vocabId)
        return try {
            docRef.update(
                mapOf(
                    "pronunciationScore" to newScore,
                    "masteryLevel" to newLevel,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            localDao?.findByRemoteId(vocabId)?.let { localWord ->
                val nowMs = System.currentTimeMillis()
                val schedule = SpacedRepetitionScheduler.computeScheduleForScore(
                    currentMastery = newLevel.toInt(),
                    currentIntervalDays = localWord.intervalDays,
                    pronunciationScore = newScore.toInt(),
                    nowMs = nowMs
                )
                localDao.recordPracticeAttempt(
                    id = localWord.id,
                    masteryLevel = newLevel.toInt(),
                    pronunciationScore = newScore.toInt().coerceIn(0, 100),
                    practicedAt = nowMs,
                    nextReviewAt = schedule.nextReviewAtMs,
                    intervalDays = schedule.nextIntervalDays
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    companion object {
        fun masteryLevelForScore(score: Int): Int = when {
            score >= 95 -> 5
            score >= 85 -> 4
            score >= 75 -> 3
            score >= 60 -> 2
            else -> 1
        }
    }

    fun observeSessions(): Flow<List<PracticeSession>> = flow {
        if (!isCloudSignedIn) {
            emitAll(localSessionsFlow)
            return@flow
        }
        val uid = auth?.currentUser?.uid ?: "unauthenticated"
        val path = "users/$uid/sessions"
        emitAll(
            db!!.collection("users").document(uid).collection("sessions")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(PracticeSession::class.java) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth?.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun logSession(
        scenarioTitle: String,
        language: String,
        turnsCount: Long,
        avgScore: Long,
        feedbackSummary: String
    ): Result<String> {
        val sessionId = "sess_${System.currentTimeMillis()}"
        if (!isCloudSignedIn) {
            val newSession = PracticeSession(
                id = sessionId,
                userId = "guest_local",
                scenarioTitle = scenarioTitle,
                language = language,
                turnsCount = turnsCount,
                avgScore = avgScore,
                feedbackSummary = feedbackSummary.take(1500)
            )
            localSessionsFlow.value = listOf(newSession) + localSessionsFlow.value
            val current = localProfileFlow.value
            localProfileFlow.value = current.copy(
                totalMinutesPracticed = current.totalMinutesPracticed + 2L.coerceAtLeast(turnsCount),
                avgPronunciationScore = avgScore
            )
            return Result.success(sessionId)
        }
        val uid = requireUserId()
        val firestore = db!!
        val docRef = firestore.collection("users").document(uid).collection("sessions").document(sessionId)
        val payload = mapOf(
            "userId" to uid,
            "scenarioTitle" to scenarioTitle,
            "language" to language,
            "turnsCount" to turnsCount,
            "avgScore" to avgScore,
            "feedbackSummary" to feedbackSummary.take(1500),
            "createdAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            // Update user practice minutes and streak if profile exists
            val userDoc = db.collection("users").document(uid)
            if (userDoc.get().await().exists()) {
                userDoc.update(
                    mapOf(
                        "totalMinutesPracticed" to FieldValue.increment(2L.coerceAtLeast(turnsCount)),
                        "avgPronunciationScore" to avgScore,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(sessionId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeLeaderboard(language: String = "All"): Flow<List<LeaderboardEntry>> = flow {
        if (!isCloudSignedIn) {
            emitAll(
                localProfileFlow.map { profile ->
                    val myXp = (profile.wordsMastered * 50L) + (profile.totalMinutesPracticed * 20L)
                    val myTier = when {
                        myXp >= 2500 -> "Black Belt Sensei"
                        myXp >= 1500 -> "Gold Tier"
                        myXp >= 800 -> "Silver Tier"
                        myXp >= 300 -> "Bronze Tier"
                        else -> "White Belt"
                    }
                    val me = LeaderboardEntry(
                        userId = profile.userId,
                        displayName = profile.displayName.ifBlank { defaultStudentName },
                        targetLanguage = profile.targetLanguage,
                        leagueTier = myTier,
                        totalXp = myXp,
                        streakDays = profile.currentStreak,
                        avgAccuracy = profile.avgPronunciationScore
                    )
                    val combined = localLeaderboardSeed + me
                    val filtered = if (language == "All") combined else combined.filter {
                        it.targetLanguage.equals(language, ignoreCase = true)
                    }
                    filtered.sortedByDescending { it.totalXp }
                }
            )
            return@flow
        }
        val path = "leaderboard"
        emitAll(
            db!!.collection("leaderboard")
                .snapshots()
                .map { snapshot ->
                    val list = snapshot.toObjects(LeaderboardEntry::class.java)
                    val filtered = if (language == "All") list else list.filter { it.targetLanguage.equals(language, ignoreCase = true) }
                    filtered.sortedByDescending { it.totalXp }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun syncLeaderboardEntry(
        profile: UserProfile,
        additionalXp: Long = 0L
    ): Result<Unit> {
        if (!isCloudSignedIn) return Result.success(Unit)
        val uid = requireUserId()
        val docRef = db!!.collection("leaderboard").document(uid)
        return try {
            val existingSnap = docRef.get().await()
            val currentXp = if (existingSnap.exists()) {
                existingSnap.getLong("totalXp") ?: 0L
            } else {
                (profile.wordsMastered * 50L) + (profile.totalMinutesPracticed * 20L)
            }
            val newXp = currentXp + additionalXp
            val tier = when {
                newXp >= 2500 -> "Black Belt Sensei"
                newXp >= 1500 -> "Gold Tier"
                newXp >= 800 -> "Silver Tier"
                newXp >= 300 -> "Bronze Tier"
                else -> "White Belt"
            }
            val payload = mapOf(
                "userId" to uid,
                "displayName" to (profile.displayName.ifBlank { defaultStudentName }),
                "targetLanguage" to profile.targetLanguage,
                "leagueTier" to tier,
                "totalXp" to newXp,
                "streakDays" to profile.currentStreak,
                "avgAccuracy" to profile.avgPronunciationScore,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }
}

