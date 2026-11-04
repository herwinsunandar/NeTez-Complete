package com.herwin.netez.utils

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.herwin.netez.device.DeviceCheckStatus
import com.herwin.netez.device.DeviceSecurityAuditResult
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DeviceSecurityPdfReportGenerator {

    fun generatePdfReport(
        context: Context,
        result: DeviceSecurityAuditResult,
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
                "NeTez Device Security Audit • Confidential Security Report • Page $pageNum",
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
                canvas.drawText("NeTez Device Security Audit Report · ${result.manufacturer} ${result.model}", marginX + 12f, 48f, subTitlePaint)
                y = 80f
            }
        }

        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault())

        // 1. Header Banner Box
        val headerRect = RectF(marginX, y, marginX + contentWidth, y + 80f)
        canvas.drawRoundRect(headerRect, 8f, 8f, Paint().apply { color = Color.parseColor("#F1F5F9") })

        canvas.drawText("NeTez Device Security Audit Report", marginX + 16f, y + 26f, titlePaint)
        canvas.drawText("Device: ${result.manufacturer} ${result.model} (Android ${result.androidVersion})", marginX + 16f, y + 46f, subTitlePaint)
        canvas.drawText("Date: ${sdf.format(Date(result.scannedAt))} · SDK API: ${result.sdkVersion} · Security Patch: ${result.securityPatch ?: "N/A"}", marginX + 16f, y + 64f, metaPaint)

        y += 96f

        // 2. Score & Grade Card
        val (scoreColorStr, gradeText) = when {
            result.securityScore >= 80 -> Pair("#16A34A", "EXCELLENT")
            result.securityScore >= 60 -> Pair("#D97706", "GOOD / FAIR")
            else -> Pair("#DC2626", "WEAK / CRITICAL")
        }

        val scorePaint = Paint().apply {
            color = Color.parseColor(scoreColorStr)
            textSize = 13f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }

        canvas.drawText("DEVICE SECURITY SCORE: ${result.securityScore}/100 (${result.grade})", marginX, y, scorePaint)
        y += 16f
        canvas.drawText("Summary: ${result.safeCount} Safe · ${result.warningCount} Warning(s) · ${result.criticalCount} Critical Issue(s) · ${result.infoCount} Info", marginX, y, bodyPaint)

        y += 20f
        canvas.drawLine(marginX, y, marginX + contentWidth, y, Paint().apply { color = Color.parseColor("#E2E8F0"); strokeWidth = 1f })
        y += 18f

        // 3. Detailed Device Security Checks
        checkAndCreateNewPage(30f)
        canvas.drawText("1. Detailed Device Security Findings & Recommendations (${result.checks.size})", marginX, y, sectionPaint)
        y += 18f

        result.checks.forEach { check ->
            checkAndCreateNewPage(45f)

            val (statusText, statusColorStr) = when (check.status) {
                DeviceCheckStatus.CRITICAL -> Pair("[🔴 CRITICAL]", "#DC2626")
                DeviceCheckStatus.WARNING -> Pair("[⚠️ WARNING]", "#D97706")
                DeviceCheckStatus.SAFE -> Pair("[✓ SAFE]", "#16A34A")
                DeviceCheckStatus.INFO -> Pair("[ℹ️ INFO]", "#2563EB")
                DeviceCheckStatus.UNKNOWN -> Pair("[UNKNOWN]", "#64748B")
            }

            val statusPaint = Paint().apply {
                color = Color.parseColor(statusColorStr)
                textSize = 9.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            canvas.drawText("$statusText ${check.name}", marginX + 10f, y, statusPaint)
            y += 13f

            if (check.detail.isNotBlank()) {
                canvas.drawText("  Detail: ${check.detail}", marginX + 10f, y, bodyPaint)
                y += 12f
            }

            if (check.recommendation.isNotBlank()) {
                canvas.drawText("  Recommendation: ${check.recommendation}", marginX + 10f, y, bodyPaint)
                y += 12f
            }

            if (!check.evidence.isNullOrBlank()) {
                canvas.drawText("  Evidence: ${check.evidence}", marginX + 10f, y, metaPaint)
                y += 12f
            }

            y += 12f
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
