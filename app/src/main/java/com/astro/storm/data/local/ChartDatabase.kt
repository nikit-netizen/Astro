package com.astro.storm.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room database for chart persistence
 */
@Database(
    entities = [ChartEntity::class],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class ChartDatabase : RoomDatabase() {
    abstract fun chartDao(): ChartDao

    companion object {
        @Volatile
        private var INSTANCE: ChartDatabase? = null

        /**
         * Migration from version 1 to 2: Add gender column to charts table
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE charts ADD COLUMN gender TEXT NOT NULL DEFAULT 'PREFER_NOT_TO_SAY'")
            }
        }

        fun getInstance(context: Context): ChartDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ChartDatabase::class.java,
                    "astrostorm_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
