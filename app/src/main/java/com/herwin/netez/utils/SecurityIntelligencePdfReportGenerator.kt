package com.herwin.netez.utils

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.herwin.netez.engine.intelligence.*
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SecurityIntelligencePdfReportGenerator {

    fun generatePdfReport(
        context: Context,
        result: SecurityIntelligenceResult,
        outputStream: OutputStream
    ) {
        val pdfDoc = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        var pageNum = 1

        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
        var currentPage = pdfDoc.startPage(pageInfo)
        var canvas = currentPage.canvas

        var y = 40f
        val marginX = 36f
        val contentWidth = 523f

        // Paints
        val titlePaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val subTitlePaint = Paint().apply {
            color = Color.parseColor("#2563EB")
            textSize = 11f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }

        val metaPaint = Paint().apply {
            color = Color.parseColor("#64748B")
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val sectionPaint = Paint().apply {
            color = Color.parseColor("#1E293B")
            textSize = 12.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val bodyPaint = Paint().apply {
            color = Color.parseColor("#334155")
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val monoPaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 9f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }

        val footerPaint = Paint().apply {
            color = Color.parseColor("#94A3B8")
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }

        fun drawFooter() {
            canvas.drawText(
                "NeTez Security Intelligence • Confidential Security Report • Page $pageNum",
                pageWidth / 2f,
                820f,
                footerPaint
            )
        }

        fun checkAndCreateNewPage(requiredSpace: Float = 20f) {
            if (y + requiredSpace > 780f) {
                drawFooter()
                pdfDoc.finishPage(currentPage)
                pageNum++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
                currentPage = pdfDoc.startPage(pageInfo)
                canvas = currentPage.canvas

                // Draw Mini Top Banner on new page
                val miniBannerRect = RectF(marginX, 30f, marginX + contentWidth, 60f)
                canvas.drawRoundRect(miniBannerRect, 6f, 6f, Paint().apply { color = Color.parseColor("#F1F5F9") })
                canvas.drawText("NeTez Security Intelligence Report · Target: ${result.target.rawInput}", marginX + 12f, 48f, subTitlePaint)
                y = 80f
            }
        }

        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault())

        // 1. Header Banner Box
        val headerRect = RectF(marginX, y, marginX + contentWidth, y + 80f)
        canvas.drawRoundRect(headerRect, 8f, 8f, Paint().apply { color = Color.parseColor("#F1F5F9") })

        canvas.drawText("NeTez Security Intelligence Report", marginX + 16f, y + 26f, titlePaint)
        canvas.drawText("TARGET: ${result.target.rawInput} (${result.target.type.name})", marginX + 16f, y + 46f, subTitlePaint)
        canvas.drawText("Date: ${sdf.format(Date(result.timestamp))} · Completed Checks: ${result.checksCompleted}", marginX + 16f, y + 64f, metaPaint)

        y += 96f

        // 2. Overall Status Summary Card
        val statusColorStr = when (result.overallStatus) {
            "CRITICAL RISK", "HIGH RISK" -> "#DC2626"
            "MODERATE RISK" -> "#D97706"
            else -> "#16A34A"
        }
        val statusPaint = Paint().apply {
            color = Color.parseColor(statusColorStr)
            textSize = 12f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }

        canvas.drawText("ASSESSMENT STATUS: ${result.overallStatus}", marginX, y, statusPaint)
        y += 16f
        canvas.drawText("Summary: ${result.checksCompleted} checks completed · ${result.warningCount} warning(s) · ${result.criticalCount} critical issue(s)", marginX, y, bodyPaint)

        y += 20f
        canvas.drawLine(marginX, y, marginX + contentWidth, y, Paint().apply { color = Color.parseColor("#E2E8F0"); strokeWidth = 1f })
        y += 18f

        // 3. Category Assessment Statuses
        checkAndCreateNewPage(30f)
        canvas.drawText("1. Assessment Categories & Sources", marginX, y, sectionPaint)
        y += 16f

        result.categoryStatuses.forEach { cat ->
            checkAndCreateNewPage(40f)

            val (statusText, catColorStr) = when (cat.status) {
                CheckExecutionStatus.SUCCESS -> Pair("[✓ Done]", "#16A34A")
                CheckExecutionStatus.WARNING -> Pair("[! Warning]", "#D97706")
                CheckExecutionStatus.CRITICAL -> Pair("[🔴 Critical]", "#DC2626")
                else -> Pair("[Unavailable]", "#64748B")
            }

            val catStatusPaint = Paint().apply {
                color = Color.parseColor(catColorStr)
                textSize = 9.5f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            }

            canvas.drawText("• ${cat.title}", marginX + 10f, y, monoPaint)
            canvas.drawText(statusText, marginX + 180f, y, catStatusPaint)

            y += 12f
            canvas.drawText("  Details: ${cat.summary}", marginX + 10f, y, bodyPaint)
            if (cat.sourceProvider.isNotBlank()) {
                y += 12f
                canvas.drawText("  Source Feed: ${cat.sourceProvider}", marginX + 10f, y, metaPaint)
            }
            y += 16f
        }

        y += 8f
        canvas.drawLine(marginX, y, marginX + contentWidth, y, Paint().apply { color = Color.parseColor("#E2E8F0"); strokeWidth = 1f })
        y += 18f

        // 4. Detailed Security Findings
        if (result.findings.isNotEmpty()) {
            checkAndCreateNewPage(30f)
            canvas.drawText("2. Security Findings & Recommendations (${result.findings.size})", marginX, y, sectionPaint)
            y += 18f

            result.findings.forEachIndexed { idx, finding ->
                checkAndCreateNewPage(45f)

                val sevColorStr = when (finding.severity) {
                    FindingSeverity.CRITICAL, FindingSeverity.HIGH -> "#DC2626"
                    FindingSeverity.MEDIUM, FindingSeverity.LOW -> "#D97706"
                    FindingSeverity.INFO -> "#2563EB"
                }
                val sevPaint = Paint().apply {
                    color = Color.parseColor(sevColorStr)
                    textSize = 9f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }

                canvas.drawText("[${finding.severity.name}] ${finding.title}", marginX + 10f, y, sevPaint)
                y += 13f

                if (finding.currentState.isNotBlank()) {
                    canvas.drawText("  Evidence: ${finding.currentState}", marginX + 10f, y, monoPaint)
                    y += 12f
                }

                canvas.drawText("  Impact: ${finding.riskImpact}", marginX + 10f, y, bodyPaint)
                y += 12f
                canvas.drawText("  Recommendation: ${finding.recommendation}", marginX + 10f, y, bodyPaint)

                if (finding.sourceProvider.isNotBlank()) {
                    y += 12f
                    canvas.drawText("  Source: ${finding.sourceProvider}", marginX + 10f, y, metaPaint)
                }

                y += 16f
            }

            canvas.drawLine(marginX, y, marginX + contentWidth, y, Paint().apply { color = Color.parseColor("#E2E8F0"); strokeWidth = 1f })
            y += 18f
        }

        // 5. Passed Checks
        if (result.passedChecks.isNotEmpty()) {
            checkAndCreateNewPage(30f)
            canvas.drawText("3. Passed Security Checks (${result.passedChecks.size})", marginX, y, sectionPaint)
            y += 16f

            val passedPaint = Paint().apply {
                color = Color.parseColor("#16A34A")
                textSize = 9f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            result.passedChecks.forEach { passed ->
                checkAndCreateNewPage(24f)
                val sourceInfo = if (passed.sourceProvider.isNotBlank()) " (Source: ${passed.sourceProvider})" else ""
                canvas.drawText("✓ ${passed.title}$sourceInfo", marginX + 10f, y, passedPaint)
                if (passed.detail.isNotBlank()) {
                    y += 11f
                    canvas.drawText("  Detail: ${passed.detail}", marginX + 22f, y, metaPaint)
                }
                y += 14f
            }

            y += 8f
            canvas.drawLine(marginX, y, marginX + contentWidth, y, Paint().apply { color = Color.parseColor("#E2E8F0"); strokeWidth = 1f })
            y += 18f
        }

        // 6. Technical Details Summary
        if (result.technicalDetails.isNotEmpty()) {
            checkAndCreateNewPage(30f)
            canvas.drawText("4. Technical Details Summary", marginX, y, sectionPaint)
            y += 16f

            result.technicalDetails.forEach { (key, value) ->
                checkAndCreateNewPage(16f)
                canvas.drawText("• $key: $value", marginX + 10f, y, monoPaint)
                y += 13f
            }
        }

        // Draw final page footer
        drawFooter()
        pdfDoc.finishPage(currentPage)

        try {
            pdfDoc.writeTo(outputStream)
        } finally {
            pdfDoc.close()
        }
    }
}
