package com.phoniq.app.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {

    val MIGRATION_4_5 =
        object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE sms_messages ADD COLUMN is_pinned INTEGER NOT NULL DEFAULT 0",
                )
                db.execSQL(
                    "ALTER TABLE sms_messages ADD COLUMN is_archived INTEGER NOT NULL DEFAULT 0",
                )
            }
        }
}
