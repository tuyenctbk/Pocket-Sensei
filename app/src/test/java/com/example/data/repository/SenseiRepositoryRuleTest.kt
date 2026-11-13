package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class SenseiRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun getOrCreateProfile_validUser_createsAndReturnsProfile() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = SenseiRepository(firestore)

        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getOrCreateProfile("Alice Learner", "Japanese")
        }
        assertTrue("getOrCreateProfile should succeed", result.isSuccess)
        val profile = result.getOrThrow()
        assertEquals("Japanese", profile.targetLanguage)
        assertEquals("Alice Learner", profile.displayName)
    }

    @Test
    fun addVocabItem_authenticatedOwner_createsAndReadsVocab() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = SenseiRepository(firestore)

        val addResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addVocabItem(
                word = "ありがとう",
                translation = "Thank you",
                phonetic = "Arigatou",
                language = "Japanese",
                pronunciationScore = 95
            )
        }
        assertTrue("addVocabItem should succeed", addResult.isSuccess)
        val vocabId = addResult.getOrThrow()
        assertNotNull(vocabId)

        val vocabList = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeVocab().first { list -> list.any { it.id == vocabId } }
        }
        assertTrue(vocabList.any { it.word == "ありがとう" })
    }

    @Test
    fun logSession_authenticatedOwner_createsPracticeSession() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = SenseiRepository(firestore)

        val sessionResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.logSession(
                scenarioTitle = "Tokyo Cafe Order",
                language = "Japanese",
                turnsCount = 5,
                avgScore = 90,
                feedbackSummary = "Excellent pitch accent on 'kudasai'."
            )
        }
        assertTrue("logSession should succeed", sessionResult.isSuccess)
        val sessionId = sessionResult.getOrThrow()
        assertNotNull(sessionId)

        val sessions = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeSessions().first { list -> list.any { it.id == sessionId } }
        }
        assertTrue(sessions.any { it.scenarioTitle == "Tokyo Cafe Order" })
    }

    @Test
    fun crossUserAccess_bobCannotReadAlicesVocabItem() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = SenseiRepository(firestore)
        val vocabId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.addVocabItem("水", "Water", "Mizu", "Japanese", 90).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("users").document(aliceUid).collection("vocab").document(vocabId).get().await()
            }
            fail("Expected PERMISSION_DENIED when Bob accesses Alice's vocab item")
        } catch (e: Exception) {
            val firestoreException = checkNotNull(e.unwrapFirestoreException()) {
                "Expected nested FirebaseFirestoreException, got $e"
            }
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
        }
    }

    @Test
    fun observeVocab_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = SenseiRepository(firestore)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeVocab().first()
            }
            fail("Expected exception when unauthenticated")
        } catch (e: Exception) {
            val firestoreException = checkNotNull(e.unwrapFirestoreException()) {
                "Expected nested FirebaseFirestoreException, got $e"
            }
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
        }
    }

    @Test
    fun syncLeaderboardEntry_authenticatedUser_writesAndObservesLeaderboard() = runBlocking {
        signInTestUser(CAROL_EMAIL)
        val repository = SenseiRepository(firestore)
        val profile = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getOrCreateProfile("Carol Sensei", "Japanese").getOrThrow()
        }

        val syncResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.syncLeaderboardEntry(profile, additionalXp = 150L)
        }
        assertTrue("syncLeaderboardEntry should succeed", syncResult.isSuccess)

        val entries = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeLeaderboard().first { list -> list.any { it.userId == profile.userId } }
        }
        val carolEntry = entries.first { it.userId == profile.userId }
        assertTrue("totalXp should include added XP", carolEntry.totalXp >= 150L)
    }

    @Test
    fun crossUserAccess_bobCannotOverwriteAlicesLeaderboardEntry() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = SenseiRepository(firestore)
        val aliceProfile = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.getOrCreateProfile("Alice Learner", "Japanese").getOrThrow()
        }
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.syncLeaderboardEntry(aliceProfile, additionalXp = 100L).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("leaderboard").document(aliceUid)
                    .update("totalXp", 99999L)
                    .await()
            }
            fail("Expected PERMISSION_DENIED when Bob modifies Alice's leaderboard entry")
        } catch (e: Exception) {
            val firestoreException = checkNotNull(e.unwrapFirestoreException()) {
                "Expected nested FirebaseFirestoreException, got $e"
            }
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
        }
    }

    @Test
    fun updateStreak_authenticatedUser_updatesProfileAndLeaderboardStreak() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = SenseiRepository(firestore)
        val profile = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getOrCreateProfile("Alice Learner", "Japanese").getOrThrow()
        }
        assertEquals(0L, profile.currentStreak)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.syncLeaderboardEntry(profile, additionalXp = 50L).getOrThrow()
            repository.updateStreak(7L).getOrThrow()
        }

        val updatedProfile = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeProfile().first { it?.currentStreak == 7L }
        }
        assertEquals(7L, updatedProfile?.currentStreak)

        val leaderboardEntries = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeLeaderboard().first { list -> list.any { it.userId == profile.userId && it.streakDays == 7L } }
        }
        assertEquals(7L, leaderboardEntries.first { it.userId == profile.userId }.streakDays)
    }

    private companion object {
        const val ALICE_EMAIL = "alice@sensei.test"
        const val BOB_EMAIL = "bob@sensei.test"
        const val CAROL_EMAIL = "carol@sensei.test"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
