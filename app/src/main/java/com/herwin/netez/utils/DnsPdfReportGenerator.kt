package com.herwin.netez.utils

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.herwin.netez.network.DnsRecordType
import com.herwin.netez.network.DnsResponse
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DnsPdfReportGenerator {

    fun generatePdfReport(
        context: Context,
        domain: String,
        allResponses: Map<DnsRecordType, DnsResponse>,
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

        val typeTagPaint = Paint().apply {
            color = Color.parseColor("#2563EB")
            textSize = 10f
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
                "NeTez Complete DNS Audit • Confidential Security Report • Page $pageNum",
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
                canvas.drawText("NeTez Complete DNS Audit Report · Domain: $domain", marginX + 12f, 48f, subTitlePaint)
                y = 80f
            }
        }

        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault())
        val firstResponse = allResponses.values.firstOrNull()
        val resolverName = firstResponse?.resolver ?: "DoH Resolver"
        val resolverIp = firstResponse?.resolverIp ?: "Cloudflare"
        val totalFoundCount = allResponses.values.sumOf { it.records.size }

        // 1. Header Banner Box
        val headerRect = RectF(marginX, y, marginX + contentWidth, y + 80f)
        canvas.drawRoundRect(headerRect, 8f, 8f, Paint().apply { color = Color.parseColor("#F1F5F9") })

        canvas.drawText("NeTez Complete 7-Record DNS Audit Report", marginX + 16f, y + 26f, titlePaint)
        canvas.drawText("Domain: $domain (All 7 Record Types)", marginX + 16f, y + 46f, subTitlePaint)
        canvas.drawText("Date: ${sdf.format(Date())} · Resolver: $resolverName ($resolverIp) · Total Records: $totalFoundCount", marginX + 16f, y + 64f, metaPaint)

        y += 96f

        // 2. Summary Breakdown per Record Type
        canvas.drawText("1. DNS Record Breakdown Summary", marginX, y, sectionPaint)
        y += 16f

        val summaryText = DnsRecordType.entries.joinToString("  •  ") { type ->
            val count = allResponses[type]?.records?.size ?: 0
            "${type.name}: $count"
        }
        canvas.drawText("• $summaryText", marginX + 10f, y, monoPaint)

        y += 20f
        canvas.drawLine(marginX, y, marginX + contentWidth, y, Paint().apply { color = Color.parseColor("#E2E8F0"); strokeWidth = 1f })
        y += 18f

        // 3. Detailed Records per Type (A, AAAA, MX, TXT, CNAME, NS, PTR)
        canvas.drawText("2. Resolved Records Details (A, AAAA, MX, TXT, CNAME, NS, PTR)", marginX, y, sectionPaint)
        y += 18f

        DnsRecordType.entries.forEach { recordType ->
            val response = allResponses[recordType]
            val records = response?.records ?: emptyList()

            checkAndCreateNewPage(30f)

            canvas.drawText("Type ${recordType.name} Records (${records.size})", marginX + 10f, y, typeTagPaint)
            y += 14f

            if (records.isEmpty()) {
                canvas.drawText("  No ${recordType.name} records found for $domain.", marginX + 10f, y, metaPaint)
                y += 16f
            } else {
                records.forEachIndexed { idx, record ->
                    checkAndCreateNewPage(32f)

                    canvas.drawText("  [${idx + 1}] Data: ${record.data}", marginX + 10f, y, monoPaint)
                    y += 12f

                    val geoInfo = if (record.countryName.isNotBlank() || record.provider.isNotBlank()) {
                        "      ISP/Country: ${record.provider} (${record.countryName}) · TTL: ${record.ttl}s"
                    } else {
                        "      TTL: ${record.ttl}s"
                    }
                    canvas.drawText(geoInfo, marginX + 10f, y, bodyPaint)
                    y += 16f
                }
            }
            y += 6f
        }

        // 4. Resolver Specification Card
        checkAndCreateNewPage(60f)
        y += 8f
        canvas.drawLine(marginX, y, marginX + contentWidth, y, Paint().apply { color = Color.parseColor("#E2E8F0"); strokeWidth = 1f })
        y += 18f

        canvas.drawText("3. Resolver Specifications", marginX, y, sectionPaint)
        y += 16f

        canvas.drawText("• DoH Provider: $resolverName", marginX + 10f, y, monoPaint)
        y += 13f
        canvas.drawText("• Resolver IP: $resolverIp", marginX + 10f, y, monoPaint)
        y += 13f
        canvas.drawText("• Query Latency: ${firstResponse?.latency ?: 0} ms", marginX + 10f, y, monoPaint)
        y += 13f

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
