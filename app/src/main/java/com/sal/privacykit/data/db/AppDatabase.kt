package com.sal.privacykit.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ProfileEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE profiles_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        deviceTemplateId TEXT NOT NULL,
                        profileMode TEXT NOT NULL,
                        identifierRules TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        customDeviceName TEXT,
                        customDeviceManufacturer TEXT,
                        skipLaunchConfirmation INTEGER NOT NULL,
                        identifierValues TEXT NOT NULL,
                        archived INTEGER NOT NULL,
                        tagColor TEXT,
                        notes TEXT
                    )""",
                )
                db.execSQL(
                    """INSERT INTO profiles_new (
                        id, name, deviceTemplateId, profileMode, identifierRules, createdAt,
                        customDeviceName, customDeviceManufacturer, skipLaunchConfirmation,
                        identifierValues, archived, tagColor, notes
                    ) SELECT
                        id, name, deviceTemplateId, profileMode, identifierRules, createdAt,
                        customDeviceName, customDeviceManufacturer, skipLaunchConfirmation,
                        identifierValues, archived, tagColor, notes
                    FROM profiles""",
                )
                db.execSQL("DROP TABLE profiles")
                db.execSQL("ALTER TABLE profiles_new RENAME TO profiles")
            }
        }

        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "privacy_kit_lite.db",
                ).addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instance = it }
            }
    }
}
