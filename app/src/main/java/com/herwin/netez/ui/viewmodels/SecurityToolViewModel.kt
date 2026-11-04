package com.herwin.netez.ui.viewmodels

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.herwin.netez.data.AppDatabase
import com.herwin.netez.data.QuickNote
import com.herwin.netez.data.entities.SecurityToolHistory
import com.herwin.netez.engine.SecurityToolsEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import com.herwin.netez.entitlement.FeatureAccessManager

class SecurityToolViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val historyDao = db.securityToolHistoryDao()
    private val quickNoteDao = db.quickNoteDao()

    var toolType by mutableStateOf("HASH_CALCULATOR")
    var showGlobalSupportDialog by mutableStateOf(false)

    // Input States
    var inputText by mutableStateOf("")
    var fileUri by mutableStateOf<Uri?>(null)
    var fileName by mutableStateOf<String?>(null)

    // Option States
    var selectedAlgo by mutableStateOf("SHA-256") // MD5, SHA-1, SHA-256, SHA-512, CRC32
    var isUrlSafe by mutableStateOf(false)
    var isDecodeMode by mutableStateOf(false) // Base64 / URL mode
    var hmacKey by mutableStateOf("")
    var uuidVersion by mutableStateOf(4)
    var tokenLength by mutableStateOf(32)
    var tokenCharset by mutableStateOf("ALPHANUMERIC")
    var tokenSymbols by mutableStateOf(false)
    var count by mutableStateOf(1)
    var regexPattern by mutableStateOf("")
    var regexCaseInsensitive by mutableStateOf(false)
    var regexMultiline by mutableStateOf(false)
    var regexDotAll by mutableStateOf(false)

    // Output States
    var isCalculating by mutableStateOf(false)
    var outputText by mutableStateOf("")
    var outputError by mutableStateOf<String?>(null)
    var jwtResult by mutableStateOf<SecurityToolsEngine.JwtResult?>(null)
    var timestampResult by mutableStateOf<SecurityToolsEngine.TimestampResult?>(null)
    var regexResult by mutableStateOf<SecurityToolsEngine.RegexResult?>(null)
    var tokenResult by mutableStateOf<SecurityToolsEngine.TokenResult?>(null)
    var actionMessage by mutableStateOf<String?>(null)

    val historyList = historyDao.getAllHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun initTool(type: String) {
        toolType = type
        outputText = ""
        outputError = null
        jwtResult = null
        timestampResult = null
        regexResult = null
        tokenResult = null
        fileUri = null
        fileName = null

        // Default settings per tool
        when (type) {
            "HASH_CALCULATOR" -> selectedAlgo = "SHA-256"
            "HMAC_GENERATOR" -> selectedAlgo = "HmacSHA256"
            "CHECKSUM" -> selectedAlgo = "SHA-256"
            "TIMESTAMP" -> {
                val now = SecurityToolsEngine.nowTimestamps()
                timestampResult = now
                inputText = now.epochSeconds.toString()
                outputText = now.local
            }
            "UUID_GENERATOR" -> {
                val uuids = SecurityToolsEngine.generateUUID(4, 1)
                outputText = uuids.joinToString("\n")
            }
            "RANDOM_TOKEN" -> {
                val res = SecurityToolsEngine.generateToken(32, "ALPHANUMERIC", false, 1)
                tokenResult = res
                outputText = res.tokens.joinToString("\n")
            }
        }
    }

    fun onFileSelected(context: Context, uri: Uri) {
        fileUri = uri
        fileName = getFileName(context, uri)
        calculate(context)
    }

    fun clearFile() {
        fileUri = null
        fileName = null
    }

    fun calculate(context: Context? = null) {
        viewModelScope.launch {
            isCalculating = true
            outputError = null
            try {
                withContext(Dispatchers.IO) {
                    when (toolType) {
                        "HASH_CALCULATOR" -> {
                            val result = if (fileUri != null && context != null) {
                                SecurityToolsEngine.hashFile(fileUri!!, context, selectedAlgo)
                            } else {
                                if (inputText.isBlank()) throw Exception("Please enter text or select a file")
                                SecurityToolsEngine.hash(inputText, selectedAlgo)
                            }
                            outputText = result
                        }
                        "BASE64" -> {
                            val result = if (fileUri != null && context != null) {
                                SecurityToolsEngine.base64EncodeFile(fileUri!!, context)
                            } else {
                                if (inputText.isBlank()) throw Exception("Please enter text to encode/decode")
                                if (isDecodeMode) {
                                    SecurityToolsEngine.base64Decode(inputText)
                                } else {
                                    SecurityToolsEngine.base64Encode(inputText, isUrlSafe)
                                }
                            }
                            outputText = result
                        }
                        "URL_ENCODER" -> {
                            if (inputText.isBlank()) throw Exception("Please enter text")
                            outputText = if (isDecodeMode) {
                                SecurityToolsEngine.urlDecode(inputText)
                            } else {
                                SecurityToolsEngine.urlEncode(inputText)
                            }
                        }
                        "JWT_DECODER" -> {
                            if (inputText.isBlank()) throw Exception("Please enter a valid JWT token")
                            val res = SecurityToolsEngine.decodeJwt(inputText)
                            jwtResult = res
                            outputText = "Header:\n${res.headerJson}\n\nPayload:\n${res.payloadJson}"
                        }
                        "UUID_GENERATOR" -> {
                            val list = SecurityToolsEngine.generateUUID(uuidVersion, count)
                            outputText = list.joinToString("\n")
                        }
                        "RANDOM_TOKEN" -> {
                            val res = SecurityToolsEngine.generateToken(tokenLength, tokenCharset, tokenSymbols, count)
                            tokenResult = res
                            outputText = res.tokens.joinToString("\n")
                        }
                        "HMAC_GENERATOR" -> {
                            if (inputText.isBlank()) throw Exception("Please enter text")
                            if (hmacKey.isBlank()) throw Exception("Please enter secret key")
                            outputText = SecurityToolsEngine.hmac(inputText, hmacKey, selectedAlgo)
                        }
                        "CHECKSUM" -> {
                            val result = if (selectedAlgo == "CRC32") {
                                if (fileUri != null && context != null) {
                                    SecurityToolsEngine.crc32File(fileUri!!, context)
                                } else {
                                    SecurityToolsEngine.crc32(inputText)
                                }
                            } else if (selectedAlgo == "ADLER32") {
                                SecurityToolsEngine.adler32(inputText)
                            } else {
                                if (fileUri != null && context != null) {
                                    SecurityToolsEngine.hashFile(fileUri!!, context, selectedAlgo)
                                } else {
                                    SecurityToolsEngine.hash(inputText, selectedAlgo)
                                }
                            }
                            outputText = result
                        }
                        "TIMESTAMP" -> {
                            if (inputText.isBlank()) throw Exception("Please enter timestamp or ISO date")
                            val res = SecurityToolsEngine.convertTimestamp(inputText)
                            timestampResult = res
                            outputText = "Epoch Seconds: ${res.epochSeconds}\nEpoch Millis: ${res.epochMillis}\nISO 8601: ${res.iso8601}\nUTC: ${res.utc}\nLocal: ${res.local}\nRelative: ${res.relative}"
                        }
                        "REGEX" -> {
                            if (regexPattern.isBlank()) throw Exception("Please enter a regular expression pattern")
                            val res = SecurityToolsEngine.testRegex(regexPattern, inputText, regexCaseInsensitive, regexMultiline, regexDotAll)
                            regexResult = res
                            outputText = "Matches: ${res.matchCount}\n" + res.matches.joinToString("\n") { m ->
                                "Match: '${m.value}' at [${m.start}..${m.end}] Groups: ${m.groups}"
                            }
                        }
                    }

                    if (outputText.isNotEmpty()) {
                        saveHistory(
                            toolType = toolType,
                            input = fileName ?: inputText.take(100),
                            output = outputText.take(200)
                        )
                        val accessManager = FeatureAccessManager.getInstance(getApplication())
                        if (accessManager.trackAndCheckGlobalToolUsage()) {
                            showGlobalSupportDialog = true
                        }
                    }
                }
            } catch (e: Exception) {
                outputError = e.localizedMessage ?: "Calculation error"
            } finally {
                isCalculating = false
            }
        }
    }

    private suspend fun saveHistory(toolType: String, input: String, output: String) {
        historyDao.insert(
            SecurityToolHistory(
                toolType = toolType,
                input = input,
                output = output,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    fun clearToolHistory() {
        viewModelScope.launch {
            historyDao.clearHistoryForTool(toolType)
            actionMessage = "History cleared"
        }
    }

    fun saveToQuickNotes() {
        if (outputText.isBlank()) return
        viewModelScope.launch {
            val note = QuickNote(
                title = "$toolType Result",
                note = outputText,
                tag = "Security Tools",
                type = "NOTE",
                timestamp = System.currentTimeMillis()
            )
            quickNoteDao.insert(note)
            actionMessage = "Saved to Quick Notes"
        }
    }

    fun copyToClipboard(context: Context, text: String? = null) {
        val str = text ?: outputText
        if (str.isBlank()) return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("$toolType Output", str)
        clipboard.setPrimaryClip(clip)
        actionMessage = "Copied to clipboard"
    }

    fun shareOutput(context: Context) {
        if (outputText.isBlank()) return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "$toolType Result")
            putExtra(Intent.EXTRA_TEXT, "$toolType Result:\n\n$outputText\n\nSent from NeTez Security Tools")
        }
        context.startActivity(Intent.createChooser(intent, "Share Output"))
    }

    private fun getFileName(context: Context, uri: Uri): String {
        var name = "File"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) name = cursor.getString(index)
            }
        }
        return name
    }
}
