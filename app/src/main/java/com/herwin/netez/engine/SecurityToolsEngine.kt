package com.herwin.netez.engine

import android.content.Context
import android.net.Uri
import android.util.Base64
import org.json.JSONObject
import java.net.URLDecoder
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.Adler32
import java.util.zip.CRC32
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.log2

object SecurityToolsEngine {

    // 1. Hash Calculator
    fun hash(text: String, algo: String): String {
        val digest = MessageDigest.getInstance(algo)
        return digest.digest(text.toByteArray(Charsets.UTF_8)).toHex()
    }

    fun hashFile(uri: Uri, context: Context, algo: String): String {
        val digest = MessageDigest.getInstance(algo)
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        } ?: throw Exception("Cannot open file")
        return digest.digest().toHex()
    }

    // 2. Base64 Encoder/Decoder
    fun base64Encode(text: String, urlSafe: Boolean = false): String {
        val flags = if (urlSafe) Base64.URL_SAFE or Base64.NO_WRAP else Base64.NO_WRAP
        return Base64.encodeToString(text.toByteArray(Charsets.UTF_8), flags)
    }

    fun base64Decode(base64: String): String {
        val bytes = Base64.decode(base64.trim(), Base64.DEFAULT)
        return String(bytes, Charsets.UTF_8)
    }

    fun base64EncodeFile(uri: Uri, context: Context): String {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw Exception("Cannot read file")
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    // 3. URL Encoder/Decoder
    fun urlEncode(text: String, spaceAsPlus: Boolean = false): String {
        val encoded = URLEncoder.encode(text, "UTF-8")
        return if (!spaceAsPlus) encoded.replace("+", "%20") else encoded
    }

    fun urlDecode(text: String): String {
        return URLDecoder.decode(text, "UTF-8")
    }

    // 4. JWT Decoder
    data class JwtResult(
        val headerJson: String,
        val payloadJson: String,
        val signature: String,
        val isExpired: Boolean,
        val expiryDate: Date?,
        val issuedAt: Date?
    )

    fun decodeJwt(jwt: String): JwtResult {
        val parts = jwt.trim().split(".")
        if (parts.size != 3) {
            throw Exception("Invalid JWT format (expected 3 parts separated by dots)")
        }

        val headerRaw = decodeBase64Part(parts[0])
        val payloadRaw = decodeBase64Part(parts[1])

        val headerPretty = try { JSONObject(headerRaw).toString(2) } catch (e: Exception) { headerRaw }
        val payloadPretty = try { JSONObject(payloadRaw).toString(2) } catch (e: Exception) { payloadRaw }

        val payloadObj = try { JSONObject(payloadRaw) } catch (e: Exception) { null }
        val exp = payloadObj?.optLong("exp", -1L)?.takeIf { it > 0 }?.let { Date(it * 1000) }
        val iat = payloadObj?.optLong("iat", -1L)?.takeIf { it > 0 }?.let { Date(it * 1000) }

        val isExpired = exp?.before(Date()) ?: false

        return JwtResult(
            headerJson = headerPretty,
            payloadJson = payloadPretty,
            signature = parts[2],
            isExpired = isExpired,
            expiryDate = exp,
            issuedAt = iat
        )
    }

    private fun decodeBase64Part(part: String): String {
        var s = part.replace("-", "+").replace("_", "/")
        while (s.length % 4 != 0) s += "="
        return String(Base64.decode(s, Base64.DEFAULT), Charsets.UTF_8)
    }

    // 5. UUID Generator
    fun generateUUID(version: Int = 4, count: Int = 1): List<String> {
        val result = mutableListOf<String>()
        val c = count.coerceIn(1, 20)
        for (i in 0 until c) {
            when (version) {
                1 -> {
                    val time = System.currentTimeMillis()
                    val rand = SecureRandom().nextInt(0xFFFF)
                    result.add("%08x-%04x-11e1-a200-%012x".format(time and 0xFFFFFFFFL, (time shr 32) and 0xFFFFL, rand.toLong() and 0xFFFFFFFFFFFFL))
                }
                else -> result.add(UUID.randomUUID().toString())
            }
        }
        return result
    }

    // 6. Random Token Generator
    data class TokenResult(
        val tokens: List<String>,
        val entropyBits: Double
    )

    fun generateToken(
        length: Int = 32,
        charset: String = "ALPHANUMERIC",
        includeSymbols: Boolean = false,
        count: Int = 1
    ): TokenResult {
        val len = length.coerceIn(8, 128)
        val cnt = count.coerceIn(1, 10)
        var chars = when (charset) {
            "HEX" -> "0123456789abcdef"
            "NUMERIC" -> "0123456789"
            "ALPHA" -> "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
            else -> "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        }
        if (includeSymbols) {
            chars += "!@#$%^&*()-_=+[]{}|;:,.<>?/~"
        }

        val random = SecureRandom()
        val tokens = (0 until cnt).map {
            (0 until len).map { chars[random.nextInt(chars.length)] }.joinToString("")
        }

        val entropy = len * (log2(chars.length.toDouble()) / log2(2.0))
        return TokenResult(tokens, entropy)
    }

    // 7. HMAC Generator
    fun hmac(text: String, key: String, algo: String = "HmacSHA256"): String {
        val mac = Mac.getInstance(algo)
        val secretKey = SecretKeySpec(key.toByteArray(Charsets.UTF_8), algo)
        mac.init(secretKey)
        return mac.doFinal(text.toByteArray(Charsets.UTF_8)).toHex()
    }

    // 8. Checksum Calculator
    fun crc32(text: String): String {
        val crc = CRC32()
        crc.update(text.toByteArray(Charsets.UTF_8))
        return crc.value.toString(16).padStart(8, '0')
    }

    fun adler32(text: String): String {
        val adler = Adler32()
        adler.update(text.toByteArray(Charsets.UTF_8))
        return adler.value.toString(16)
    }

    fun crc32File(uri: Uri, context: Context): String {
        val crc = CRC32()
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                crc.update(buffer, 0, bytesRead)
            }
        } ?: throw Exception("Cannot open file")
        return crc.value.toString(16).padStart(8, '0')
    }

    // 9. Timestamp Converter
    data class TimestampResult(
        val epochSeconds: Long,
        val epochMillis: Long,
        val iso8601: String,
        val utc: String,
        val local: String,
        val relative: String
    )

    fun convertTimestamp(input: String): TimestampResult {
        val raw = input.trim()
        val millis: Long = when {
            raw.toLongOrNull() != null -> {
                val l = raw.toLong()
                if (l < 10000000000L) l * 1000L else l
            }
            else -> {
                val formats = listOf(
                    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                    "yyyy-MM-dd'T'HH:mm:ss'Z'",
                    "yyyy-MM-dd HH:mm:ss",
                    "yyyy-MM-dd"
                )
                var parsed: Long? = null
                for (fmt in formats) {
                    try {
                        val sdf = SimpleDateFormat(fmt, Locale.US)
                        sdf.timeZone = TimeZone.getTimeZone("UTC")
                        parsed = sdf.parse(raw)?.time
                        if (parsed != null) break
                    } catch (e: Exception) {}
                }
                parsed ?: throw Exception("Cannot parse date/timestamp: $raw")
            }
        }

        val date = Date(millis)
        val isoSdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        val utcSdf = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'UTC'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        val localSdf = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.getDefault())

        val diff = System.currentTimeMillis() - millis
        val relative = when {
            diff in 0..59000 -> "Just now"
            diff in 60000..3599000 -> "${diff / 60000} mins ago"
            diff in 3600000..86399000 -> "${diff / 3600000} hours ago"
            diff >= 86400000 -> "${diff / 86400000} days ago"
            else -> "${-diff / 1000} seconds in the future"
        }

        return TimestampResult(
            epochSeconds = millis / 1000L,
            epochMillis = millis,
            iso8601 = isoSdf.format(date),
            utc = utcSdf.format(date),
            local = localSdf.format(date),
            relative = relative
        )
    }

    fun nowTimestamps(): TimestampResult {
        return convertTimestamp(System.currentTimeMillis().toString())
    }

    // 10. Regex Tester
    data class RegexResult(
        val matches: List<MatchItem>,
        val isMatch: Boolean,
        val matchCount: Int
    )

    data class MatchItem(
        val value: String,
        val start: Int,
        val end: Int,
        val groups: List<String>
    )

    fun testRegex(
        pattern: String,
        text: String,
        caseInsensitive: Boolean = false,
        multiline: Boolean = false,
        dotAll: Boolean = false
    ): RegexResult {
        if (pattern.isEmpty()) throw Exception("Pattern cannot be empty")

        val options = mutableSetOf<RegexOption>()
        if (caseInsensitive) options.add(RegexOption.IGNORE_CASE)
        if (multiline) options.add(RegexOption.MULTILINE)
        if (dotAll) options.add(RegexOption.DOT_MATCHES_ALL)

        val regex = Regex(pattern, options)
        val matchResults = regex.findAll(text).toList()

        val items = matchResults.map { mr ->
            MatchItem(
                value = mr.value,
                start = mr.range.first,
                end = mr.range.last + 1,
                groups = mr.groupValues
            )
        }

        return RegexResult(
            matches = items,
            isMatch = items.isNotEmpty(),
            matchCount = items.size
        )
    }

    private fun ByteArray.toHex(): String {
        return joinToString("") { "%02x".format(it) }
    }
}
