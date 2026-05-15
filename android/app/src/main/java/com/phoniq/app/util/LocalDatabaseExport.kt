package com.phoniq.app.util

import android.content.ContentResolver
import android.net.Uri
import com.phoniq.app.PhonIQApp
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

object LocalDatabaseExport {
    private const val DB_NAME = "phoniq.db"

    /**
     * Checkpoint WAL into the main DB file, then stream [DB_NAME] to [outputStream].
     */
    fun copyDatabaseToStream(app: PhonIQApp, outputStream: OutputStream): Result<Unit> =
        runCatching {
            app.database.openHelper.writableDatabase.execSQL("PRAGMA wal_checkpoint(FULL)")
            val dbFile = app.getDatabasePath(DB_NAME)
            if (!dbFile.exists()) {
                error("Database file missing")
            }
            FileInputStream(dbFile).use { input ->
                input.copyTo(outputStream)
            }
        }

    /**
     * Replace local `phoniq.db` (and WAL/SHM siblings) with [uri] contents after closing Room.
     * Caller should restart the process before using [PhonIQApp.database] again.
     */
    fun restoreDatabaseFromUri(app: PhonIQApp, resolver: ContentResolver, uri: Uri): Result<Unit> =
        runCatching {
            val stream =
                resolver.openInputStream(uri) ?: error("Could not open backup")
            stream.use { restoreDatabaseFromStream(app, it) }
        }

    fun restoreDatabaseFromStream(app: PhonIQApp, input: InputStream): Result<Unit> =
        runCatching {
            app.closeDatabase()
            val dbFile = app.getDatabasePath(DB_NAME)
            dbFile.parentFile?.mkdirs()
            File(dbFile.path + "-wal").delete()
            File(dbFile.path + "-shm").delete()
            if (dbFile.exists()) {
                check(dbFile.delete()) { "Could not remove existing database" }
            }
            FileOutputStream(dbFile).use { out ->
                input.copyTo(out)
                out.fd.sync()
            }
        }
}
