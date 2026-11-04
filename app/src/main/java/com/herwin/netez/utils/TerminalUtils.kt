package com.herwin.netez.utils

import android.content.Context
import android.graphics.Typeface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration

object TerminalUtils {

    // Default Dark Theme ANSI 16 Color Palette (High Contrast)
    val ANSI_COLOR_PALETTE = intArrayOf(
        0xFF0A0C14.toInt(), // 0: Black (Background matching)
        0xFFEF4444.toInt(), // 1: Red
        0xFF10B981.toInt(), // 2: Green
        0xFFF59E0B.toInt(), // 3: Yellow
        0xFF3B82F6.toInt(), // 4: Blue
        0xFFEC4899.toInt(), // 5: Magenta
        0xFF06B6D4.toInt(), // 6: Cyan
        0xFFE5E7EB.toInt(), // 7: Light Gray (White)
        0xFF6B7280.toInt(), // 8: Bright Black (Dark Gray)
        0xFFF87171.toInt(), // 9: Bright Red
        0xFF34D399.toInt(), // 10: Bright Green
        0xFFFBBF24.toInt(), // 11: Bright Yellow
        0xFF60A5FA.toInt(), // 12: Bright Blue
        0xFFF472B6.toInt(), // 13: Bright Magenta
        0xFF38BDF8.toInt(), // 14: Bright Cyan
        0xFFFFFFFF.toInt()  // 15: Bright White
    )

    fun getMonospaceTypeface(context: Context): Typeface {
        return try {
            Typeface.createFromAsset(context.assets, "fonts/jetbrains_mono.ttf")
        } catch (e: Exception) {
            Typeface.MONOSPACE
        }
    }

    fun sanitizeAnsi(raw: String): String {
        var clean = raw
        if (clean.contains("\u001B[2J")) {
            clean = clean.substringAfterLast("\u001B[2J")
        }
        if (clean.contains("\u001B[H\u001B[J")) {
            clean = clean.substringAfterLast("\u001B[H\u001B[J")
        }
        clean = clean.replace(Regex("\u001B[()][0-9A-Za-z]"), "")
        clean = clean.replace(Regex("\u001B[=>]"), "")
        clean = clean.replace(Regex("\u001B\\][^\u0007\u001B]*(\u0007|\u001B\\\\)?"), "")
        clean = clean.replace(Regex("\u001B\\[\\?[0-9]+[a-zA-Z]"), "")
        clean = clean.replace(Regex("\u001B\\[[0-9;]*[a-lA-L]"), "")
        clean = clean.replace("\r\n", "\n").replace("\r", "")
        return clean
    }

    fun parseAnsi(input: String): AnnotatedString {
        val sanitized = sanitizeAnsi(input)
        return buildAnnotatedString {
            var i = 0
            while (i < sanitized.length) {
                val nextEsc = sanitized.indexOf("\u001B[", i)
                if (nextEsc == -1) {
                    append(sanitized.substring(i))
                    break
                }
                append(sanitized.substring(i, nextEsc))

                val mIndex = sanitized.indexOf("m", nextEsc)
                if (mIndex == -1) {
                    append("\u001B[")
                    i = nextEsc + 2
                    continue
                }

                val codes = sanitized.substring(nextEsc + 2, mIndex).split(";")
                applyAnsiCodes(codes)
                i = mIndex + 1
            }
        }
    }

    private fun AnnotatedString.Builder.applyAnsiCodes(codes: List<String>) {
        var currentFg: Color? = null
        var currentBg: Color? = null
        var isBold = false
        var isUnderline = false
        var isInverted = false

        codes.forEach { code ->
            when (code) {
                "0" -> {
                    currentFg = null
                    currentBg = null
                    isBold = false
                    isUnderline = false
                    isInverted = false
                }
                "1" -> isBold = true
                "4" -> isUnderline = true
                "7" -> isInverted = true
                "30" -> currentFg = Color.Black
                "31" -> currentFg = Color(0xFFEF4444)
                "32" -> currentFg = Color(0xFF10B981)
                "33" -> currentFg = Color(0xFFF59E0B)
                "34" -> currentFg = Color(0xFF3B82F6)
                "35" -> currentFg = Color(0xFFEC4899)
                "36" -> currentFg = Color(0xFF06B6D4)
                "37" -> currentFg = Color.White
                "40" -> currentBg = Color.Black
                "41" -> currentBg = Color(0xFFEF4444)
                "42" -> currentBg = Color(0xFF10B981)
                "43" -> currentBg = Color(0xFFF59E0B)
                "44" -> currentBg = Color(0xFF3B82F6)
                "45" -> currentBg = Color(0xFFEC4899)
                "46" -> currentBg = Color(0xFF06B6D4)
                "47" -> currentBg = Color.White
                "90" -> currentFg = Color.Gray
                "91" -> currentFg = Color(0xFFFF6B6B)
                "92" -> currentFg = Color(0xFF51CF66)
                "93" -> currentFg = Color(0xFFFCC419)
                "94" -> currentFg = Color(0xFF339AF0)
                "95" -> currentFg = Color(0xFFCC5DE8)
                "96" -> currentFg = Color(0xFF22B8CF)
                "97" -> currentFg = Color.White
                "100" -> currentBg = Color.Gray
                "101" -> currentBg = Color(0xFFFF6B6B)
                "102" -> currentBg = Color(0xFF51CF66)
                "103" -> currentBg = Color(0xFFFCC419)
                "104" -> currentBg = Color(0xFF339AF0)
                "105" -> currentBg = Color(0xFFCC5DE8)
                "106" -> currentBg = Color(0xFF22B8CF)
                "107" -> currentBg = Color.White
            }
        }

        val finalFg = if (isInverted) (currentBg ?: Color.Black) else currentFg
        val finalBg = if (isInverted) (currentFg ?: Color.White) else currentBg

        val style = SpanStyle(
            color = finalFg ?: Color.Unspecified,
            background = finalBg ?: Color.Unspecified,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            textDecoration = if (isUnderline) TextDecoration.Underline else TextDecoration.None
        )

        pushStyle(style)
    }

    val commonSnippets = listOf(
        Snippet("MikroTik", "/interface print"),
        Snippet("MikroTik", "/ip address print"),
        Snippet("MikroTik", "/system resource print"),
        Snippet("Linux", "htop"),
        Snippet("Linux", "df -h"),
        Snippet("Linux", "sudo apt update && sudo apt upgrade -y"),
        Snippet("Linux", "tail -f /var/log/syslog"),
        Snippet("Network", "ping -c 4 8.8.8.8"),
        Snippet("Network", "ip a"),
        Snippet("Network", "netstat -tulpn")
    )
}

data class Snippet(val category: String, val command: String)
