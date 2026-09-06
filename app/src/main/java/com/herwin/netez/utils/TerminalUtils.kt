package com.herwin.netez.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration

object TerminalUtils {

    fun parseAnsi(input: String): AnnotatedString {
        return buildAnnotatedString {
            var i = 0
            while (i < input.length) {
                val nextEsc = input.indexOf("\u001B[", i)
                if (nextEsc == -1) {
                    append(input.substring(i))
                    break
                }
                append(input.substring(i, nextEsc))
                
                val mIndex = input.indexOf("m", nextEsc)
                if (mIndex == -1) {
                    append("\u001B[")
                    i = nextEsc + 2
                    continue
                }
                
                val codes = input.substring(nextEsc + 2, mIndex).split(";")
                applyAnsiCodes(codes)
                i = mIndex + 1
            }
        }
    }

    private fun AnnotatedString.Builder.applyAnsiCodes(codes: List<String>) {
        codes.forEach { code ->
            when (code) {
                "0" -> { /* Reset - ideally we'd track and pop, but for now we just push a reset style or do nothing */ }
                "1" -> pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                "4" -> pushStyle(SpanStyle(textDecoration = TextDecoration.Underline))
                "30" -> pushStyle(SpanStyle(color = Color.Black))
                "31" -> pushStyle(SpanStyle(color = Color.Red))
                "32" -> pushStyle(SpanStyle(color = Color.Green))
                "33" -> pushStyle(SpanStyle(color = Color.Yellow))
                "34" -> pushStyle(SpanStyle(color = Color.Blue))
                "35" -> pushStyle(SpanStyle(color = Color.Magenta))
                "36" -> pushStyle(SpanStyle(color = Color.Cyan))
                "37" -> pushStyle(SpanStyle(color = Color.White))
                // Bright colors
                "90" -> pushStyle(SpanStyle(color = Color.DarkGray))
                "91" -> pushStyle(SpanStyle(color = Color(0xFFFF5555)))
                "92" -> pushStyle(SpanStyle(color = Color(0xFF55FF55)))
                "93" -> pushStyle(SpanStyle(color = Color(0xFFFFFF55)))
                "94" -> pushStyle(SpanStyle(color = Color(0xFF5555FF)))
                "95" -> pushStyle(SpanStyle(color = Color(0xFFFF55FF)))
                "96" -> pushStyle(SpanStyle(color = Color(0xFF55FFFF)))
                "97" -> pushStyle(SpanStyle(color = Color.White))
            }
        }
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
