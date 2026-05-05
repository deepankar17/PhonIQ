package com.phoniq.app.export

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import com.phoniq.app.data.db.entity.CallLogEntity
import com.phoniq.app.data.db.entity.TransactionEntity
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Exports Room entities to CSV files in the public Downloads folder via MediaStore.
 * No INTERNET permission required — files stay local.
 */
object CsvExporter {

    private val dateFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    // ── Transactions ──────────────────────────────────────────────────────

    fun exportTransactions(context: Context, transactions: List<TransactionEntity>): Result<String> =
        runCatching {
            val fileName = "phoniq_transactions_${timestamp()}.csv"
            val uri = createDownloadFile(context, fileName, "text/csv")
            context.contentResolver.openOutputStream(uri)!!.use { out ->
                writeCsv(out, listOf("Date", "Type", "Amount", "Merchant", "Category", "Account ID")) {
                    transactions.forEach { t ->
                        writeLine(
                            out,
                            dateFmt.format(Date(t.date)),
                            t.txnType,
                            "%.2f".format(t.amount),
                            t.merchant ?: "",
                            t.category,
                            t.accountId?.toString() ?: "",
                        )
                    }
                }
            }
            "Saved: Downloads/$fileName"
        }

    // ── Call log ──────────────────────────────────────────────────────────

    fun exportCallLog(context: Context, calls: List<CallLogEntity>): Result<String> =
        runCatching {
            val fileName = "phoniq_calls_${timestamp()}.csv"
            val uri = createDownloadFile(context, fileName, "text/csv")
            context.contentResolver.openOutputStream(uri)!!.use { out ->
                writeCsv(out, listOf("Timestamp", "Number", "Type", "Duration (s)")) {
                    calls.forEach { c ->
                        writeLine(
                            out,
                            dateFmt.format(Date(c.timestamp)),
                            c.number,
                            c.type,
                            c.durationSec.toString(),
                        )
                    }
                }
            }
            "Saved: Downloads/$fileName"
        }

    // ── Helpers ───────────────────────────────────────────────────────────

    private fun timestamp() = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())

    private fun createDownloadFile(context: Context, name: String, mimeType: String) =
        context.contentResolver.insert(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, name)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            },
        ) ?: error("Could not create file $name in Downloads")

    private inline fun writeCsv(
        out: OutputStream,
        header: List<String>,
        block: () -> Unit,
    ) {
        out.write((header.joinToString(",") + "\n").toByteArray())
        block()
    }

    private fun writeLine(out: OutputStream, vararg fields: String) {
        val line = fields.joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" } + "\n"
        out.write(line.toByteArray())
    }
}
