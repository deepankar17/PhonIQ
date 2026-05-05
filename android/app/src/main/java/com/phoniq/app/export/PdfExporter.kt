package com.phoniq.app.export

import android.content.ContentValues
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.provider.MediaStore
import com.phoniq.app.data.db.entity.TransactionEntity
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Exports transaction data to a PDF file in the public Downloads directory.
 *
 * Uses only the built-in [PdfDocument] API — no third-party PDF library needed.
 */
object PdfExporter {

    private const val PAGE_WIDTH = 595      // A4 width in points (72 dpi)
    private const val PAGE_HEIGHT = 842     // A4 height in points
    private const val MARGIN = 48f
    private const val LINE_HEIGHT = 22f
    private const val HEADER_HEIGHT = 80f

    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val moneyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Generates a transaction summary PDF and saves it to Downloads.
     * @param transactions List of transactions to include.
     * @param monthLabel   Human-readable label e.g. "April 2026".
     * @param onResult     Called with the file path on success, or an error message.
     */
    fun exportTransactions(
        context: Context,
        transactions: List<TransactionEntity>,
        monthLabel: String,
        onResult: (String) -> Unit,
    ) {
        val doc = PdfDocument()
        try {
            // ------ Summary page ------
            addSummaryPage(doc, transactions, monthLabel)

            // ------ Detail pages ------
            addDetailPages(doc, transactions)

            // ------ Save to Downloads ------
            val fileName = "PhonIQ_Transactions_${monthLabel.replace(" ", "_")}.pdf"
            val saved = saveToDownloads(context, doc, fileName)
            onResult(if (saved) "Exported to Downloads/$fileName" else "Export failed")
        } catch (e: Exception) {
            onResult("Export error: ${e.message}")
        } finally {
            doc.close()
        }
    }

    // -----------------------------------------------------------------------
    // Pages
    // -----------------------------------------------------------------------

    private fun addSummaryPage(
        doc: PdfDocument,
        txns: List<TransactionEntity>,
        monthLabel: String,
    ) {
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = paint(18f, Color.parseColor("#1A1A2E"), bold = true)
        val subPaint = paint(11f, Color.GRAY)
        val headPaint = paint(10f, Color.parseColor("#6C63FF"), bold = true)
        val bodyPaint = paint(10f, Color.DKGRAY)
        val positivePaint = paint(10f, Color.parseColor("#4CAF50"), bold = true)
        val negativePaint = paint(10f, Color.parseColor("#F44336"), bold = true)

        var y = MARGIN + 30f

        // App title + date
        canvas.drawText("PhonIQ · Transaction Report", MARGIN, y, titlePaint)
        y += LINE_HEIGHT * 1.2f
        canvas.drawText(
            "Period: $monthLabel  ·  Generated: ${dateFormat.format(Date())}",
            MARGIN, y, subPaint,
        )
        y += LINE_HEIGHT * 2f

        // Summary box
        val totalDebit = txns.filter { it.txnType == "DEBIT" }.sumOf { it.amount }
        val totalCredit = txns.filter { it.txnType == "CREDIT" }.sumOf { it.amount }
        val net = totalCredit - totalDebit

        canvas.drawText("SUMMARY", MARGIN, y, headPaint)
        y += LINE_HEIGHT

        data class SummaryRow(val label: String, val value: String, val paint: Paint)
        listOf(
            SummaryRow("Total Transactions", "${txns.size}", bodyPaint),
            SummaryRow("Total Debits", moneyFormat.format(totalDebit), negativePaint),
            SummaryRow("Total Credits", moneyFormat.format(totalCredit), positivePaint),
            SummaryRow("Net", moneyFormat.format(net), if (net >= 0) positivePaint else negativePaint),
        ).forEach { row ->
            canvas.drawText(row.label, MARGIN, y, bodyPaint)
            canvas.drawText(row.value, PAGE_WIDTH - MARGIN, y, row.paint.apply { textAlign = Paint.Align.RIGHT })
            y += LINE_HEIGHT
        }

        y += LINE_HEIGHT

        // Category breakdown
        canvas.drawText("BY CATEGORY", MARGIN, y, headPaint)
        y += LINE_HEIGHT
        txns.filter { it.txnType == "DEBIT" }
            .groupBy { it.category }
            .mapValues { (_, v) -> v.sumOf { it.amount } }
            .entries.sortedByDescending { it.value }
            .forEach { (cat, total) ->
                canvas.drawText(cat, MARGIN + 8f, y, bodyPaint)
                canvas.drawText(
                    moneyFormat.format(total),
                    PAGE_WIDTH - MARGIN, y,
                    negativePaint.apply { textAlign = Paint.Align.RIGHT },
                )
                y += LINE_HEIGHT
                if (y > PAGE_HEIGHT - MARGIN) return@forEach
            }

        doc.finishPage(page)
    }

    private fun addDetailPages(doc: PdfDocument, txns: List<TransactionEntity>) {
        val sorted = txns.sortedByDescending { it.date }
        val bodyPaint = paint(9f, Color.DKGRAY)
        val headPaint = paint(9f, Color.parseColor("#6C63FF"), bold = true)
        val positivePaint = paint(9f, Color.parseColor("#4CAF50"))
        val negativePaint = paint(9f, Color.parseColor("#F44336"))
        val dividerPaint = Paint().apply { color = Color.LTGRAY; strokeWidth = 0.5f }

        var pageNum = 2
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create()
        var page = doc.startPage(pageInfo)
        var canvas = page.canvas
        var y = MARGIN + 20f

        fun pageHeader(c: Canvas) {
            c.drawText("DATE", MARGIN, MARGIN, headPaint)
            c.drawText("MERCHANT", MARGIN + 90f, MARGIN, headPaint)
            c.drawText("CATEGORY", MARGIN + 270f, MARGIN, headPaint)
            c.drawText("AMOUNT", PAGE_WIDTH - MARGIN, MARGIN, headPaint.apply { textAlign = Paint.Align.RIGHT })
            c.drawLine(MARGIN, MARGIN + 6f, PAGE_WIDTH - MARGIN, MARGIN + 6f, dividerPaint)
        }

        pageHeader(canvas)

        sorted.forEach { txn ->
            if (y > PAGE_HEIGHT - MARGIN * 2) {
                doc.finishPage(page)
                pageNum++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create()
                page = doc.startPage(pageInfo)
                canvas = page.canvas
                y = MARGIN + 20f
                pageHeader(canvas)
            }

            val dateStr = dateFormat.format(Date(txn.date))
            val merchant = txn.merchant?.take(28) ?: "—"
            val amtPaint = if (txn.txnType == "CREDIT") positivePaint else negativePaint
            val prefix = if (txn.txnType == "CREDIT") "+" else "-"

            canvas.drawText(dateStr, MARGIN, y, bodyPaint)
            canvas.drawText(merchant, MARGIN + 90f, y, bodyPaint)
            canvas.drawText(txn.category.take(14), MARGIN + 270f, y, bodyPaint)
            canvas.drawText(
                "$prefix${moneyFormat.format(txn.amount)}",
                PAGE_WIDTH - MARGIN, y,
                amtPaint.apply { textAlign = Paint.Align.RIGHT },
            )
            y += LINE_HEIGHT
        }

        doc.finishPage(page)
    }

    // -----------------------------------------------------------------------
    // Save to Downloads via MediaStore
    // -----------------------------------------------------------------------

    private fun saveToDownloads(context: Context, doc: PdfDocument, fileName: String): Boolean {
        return try {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: return false
            resolver.openOutputStream(uri)?.use { out ->
                doc.writeTo(out)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    // -----------------------------------------------------------------------
    // Paint helpers
    // -----------------------------------------------------------------------

    private fun paint(size: Float, color: Int, bold: Boolean = false) = Paint().apply {
        textSize = size
        this.color = color
        typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        isAntiAlias = true
    }
}
