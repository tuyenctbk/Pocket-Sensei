package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        VocabularyWord::class,
        AppEventLog::class,
        CachedEvaluationEntity::class,
        CustomPhraseEntity::class,
        CustomScenarioEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class SenseiLocalDatabase : RoomDatabase() {

    abstract fun vocabularyWordDao(): VocabularyWordDao
    abstract fun appEventDao(): AppEventDao
    abstract fun cachedEvaluationDao(): CachedEvaluationDao
    abstract fun customPhraseDao(): CustomPhraseDao
    abstract fun customScenarioDao(): CustomScenarioDao

    companion object {
        @Volatile
        private var INSTANCE: SenseiLocalDatabase? = null

        fun getInstance(context: Context): SenseiLocalDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SenseiLocalDatabase::class.java,
                    "pocket_sensei_local.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
