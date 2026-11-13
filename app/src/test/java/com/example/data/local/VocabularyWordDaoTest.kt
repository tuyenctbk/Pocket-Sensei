package com.example.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class VocabularyWordDaoTest {

    private lateinit var database: SenseiLocalDatabase
    private lateinit var vocabDao: VocabularyWordDao
    private lateinit var eventDao: AppEventDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, SenseiLocalDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        vocabDao = database.vocabularyWordDao()
        eventDao = database.appEventDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndObserveVocabularyWord_tracksProgressCorrectly() = runBlocking {
        val entry = VocabularyWord(
            word = "ありがとうございます",
            translation = "Thank you very much",
            phonetics = "Arigatou gozaimasu",
            masteryLevel = 3,
            language = "Japanese",
            pronunciationScore = 91
        )
        val rowId = vocabDao.insertOrUpdate(entry)
        assertTrue(rowId > 0)

        val words = vocabDao.observeAllWords().first()
        assertEquals(1, words.size)
        val saved = words.first()
        assertEquals("ありがとうございます", saved.word)
        assertEquals("Thank you very much", saved.translation)
        assertEquals("Arigatou gozaimasu", saved.phonetics)
        assertEquals(3, saved.masteryLevel)

        vocabDao.recordPracticeAttempt(
            id = saved.id,
            masteryLevel = 5,
            pronunciationScore = 98
        )
        val updated = vocabDao.findByWordAndLanguage("ありがとうございます", "Japanese")
        assertNotNull(updated)
        assertEquals(5, updated?.masteryLevel)
        assertEquals(98, updated?.pronunciationScore)
        assertEquals(2, updated?.practiceCount)
    }

    @Test
    fun insertAndObserveAppEvents_persistsTelemetry() = runBlocking {
        eventDao.insertEvent(
            AppEventLog(
                eventName = "pronunciation_practiced",
                category = "coach",
                metadataJson = """{"score":94}""",
                sessionId = "sess_test_1"
            )
        )
        val count = eventDao.observeTotalEventCount().first()
        val recent = eventDao.observeRecentEvents(10).first()

        assertEquals(1, count)
        assertEquals("pronunciation_practiced", recent.first().eventName)
        assertEquals("coach", recent.first().category)
    }
}
