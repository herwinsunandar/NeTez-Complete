package com.herwin.netez.ui.viewmodels

import com.herwin.netez.entitlement.FeatureAccessManager

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.herwin.netez.R
import com.herwin.netez.data.AppDatabase
import com.herwin.netez.data.PasswordEntry
import com.herwin.netez.data.SecurityPreferences
import com.herwin.netez.data.entities.PasswordStrengthHistory
import com.herwin.netez.network.HibpManager
import com.herwin.netez.utils.SecurityUtils
import com.herwin.netez.worker.ClipboardClearWorker
import com.nulabinc.zxcvbn.Zxcvbn
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.security.SecureRandom
import java.util.Random
import java.util.concurrent.TimeUnit
import kotlin.math.log2

data class StrengthResult(
    val score: Int,
    val crackTime: String,
    val entropy: Double,
    val length: Int,
    val hasUpper: Boolean,
    val hasLower: Boolean,
    val hasDigit: Boolean,
    val hasSymbol: Boolean,
    val suggestions: List<String>,
    val pwnedCount: Int = -2
)

class PasswordStrengthViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).passwordStrengthDao()
    private val securityPrefs = SecurityPreferences(application)
    private val zxcvbn = Zxcvbn()
    private val secureRandom = SecureRandom()

    var passwordInput by mutableStateOf("")
    var isPasswordVisible by mutableStateOf(false)
    var strengthResult by mutableStateOf<StrengthResult?>(null)
    var isCheckingPwned by mutableStateOf(false)
    var showGlobalSupportDialog by mutableStateOf(false)

    var isGeneratorExpanded by mutableStateOf(false)
    var genLength by mutableFloatStateOf(16f)
    var genUpper by mutableStateOf(true)
    var genLower by mutableStateOf(true)
    var genNumbers by mutableStateOf(true)
    var genSymbols by mutableStateOf(true)
    var genExcludeAmbiguous by mutableStateOf(false)
    var generatedPassword by mutableStateOf("")
    var generatedStrength by mutableStateOf<StrengthResult?>(null)

    val history: StateFlow<List<PasswordStrengthHistory>> = dao.getAllHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var debounceJob: Job? = null

    fun toggleGenerator() {
        isGeneratorExpanded = !isGeneratorExpanded
        if (isGeneratorExpanded && generatedPassword.isEmpty()) {
            generateNewPassword()
        }
    }

    fun onPasswordChange(newPassword: String) {
        passwordInput = newPassword
        debounceJob?.cancel()
        
        if (newPassword.isEmpty()) {
            strengthResult = null
            return
        }

        debounceJob = viewModelScope.launch {
            delay(300)
            analyzePassword(newPassword)
            
            strengthResult?.let {
                if (it.score < 50 && !isGeneratorExpanded) {
                    isGeneratorExpanded = true
                    if (generatedPassword.isEmpty()) generateNewPassword()
                }
            }

            val accessManager = FeatureAccessManager.getInstance(getApplication())
            if (accessManager.trackAndCheckGlobalToolUsage()) {
                showGlobalSupportDialog = true
            }
        }
    }

    private fun analyzePassword(password: String, isForGenerator: Boolean = false) {
        val result = zxcvbn.measure(password)
        
        val hasUpper = password.any { it.isUpperCase() }
        val hasLower = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSymbol = password.any { !it.isLetterOrDigit() }
        
        var charset = 0
        if (hasLower) charset += 26
        if (hasUpper) charset += 26
        if (hasDigit) charset += 10
        if (hasSymbol) charset += 33
        
        val entropy = if (charset > 0) password.length * log2(charset.toDouble()) else 0.0
        val baseScore = (result.score * 25)
        
        val strength = StrengthResult(
            score = baseScore,
            crackTime = result.crackTimesDisplay?.offlineSlowHashing1e4perSecond ?: "Unknown",
            entropy = entropy,
            length = password.length,
            hasUpper = hasUpper,
            hasLower = hasLower,
            hasDigit = hasDigit,
            hasSymbol = hasSymbol,
            suggestions = result.feedback.suggestions
        )

        if (isForGenerator) {
            generatedStrength = strength
        } else {
            strengthResult = strength
        }
    }

    fun generateNewPassword() {
        generatedPassword = generatePassword(
            length = genLength.toInt(),
            upper = genUpper,
            lower = genLower,
            numbers = genNumbers,
            symbols = genSymbols,
            excludeAmbiguous = genExcludeAmbiguous
        )
        analyzePassword(generatedPassword, isForGenerator = true)
    }

    fun generatePassword(
        length: Int,
        upper: Boolean,
        lower: Boolean,
        numbers: Boolean,
        symbols: Boolean,
        excludeAmbiguous: Boolean
    ): String {
        var charset = ""
        val mandatory = mutableListOf<String>()

        if (lower) {
            val s = "abcdefghijklmnopqrstuvwxyz"
            charset += s
            mandatory.add(s)
        }
        if (upper) {
            val s = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
            charset += s
            mandatory.add(s)
        }
        if (numbers) {
            val s = "0123456789"
            charset += s
            mandatory.add(s)
        }
        if (symbols) {
            val s = "!@#$%^&*()_+-=[]{}|;:,.<>?"
            charset += s
            mandatory.add(s)
        }

        if (excludeAmbiguous) {
            val ambiguous = "0O1lI"
            charset = charset.filterNot { it in ambiguous }
            mandatory.indices.forEach { i ->
                mandatory[i] = mandatory[i].filterNot { it in ambiguous }
            }
        }

        if (charset.isEmpty()) return ""

        val result = StringBuilder()
        mandatory.forEach { set ->
            if (set.isNotEmpty()) {
                result.append(set[secureRandom.nextInt(set.length)])
            }
        }

        while (result.length < length) {
            result.append(charset[secureRandom.nextInt(charset.length)])
        }

        return result.toString().toList().shuffled(Random(secureRandom.nextLong())).joinToString("")
    }

    fun generateMemorable() {
        val words = listOf(
            "ayam", "nasi", "makan", "tidur", "lari", "buku", "rumah", "mobil", "pohon", "bunga",
            "mountain", "river", "ocean", "forest", "desert", "cloud", "sun", "moon", "star", "earth",
            "kopi", "teh", "susu", "roti", "keju", "apel", "jeruk", "pisang", "mangga", "anggur",
            "happy", "bright", "strong", "brave", "smart", "quick", "calm", "cool", "warm", "fresh"
        )
        val symbols = "!@#$%^&*"
        val w1 = words[secureRandom.nextInt(words.size)]
        val w2 = words[secureRandom.nextInt(words.size)]
        val w3 = words[secureRandom.nextInt(words.size)]
        val num = secureRandom.nextInt(100)
        val sym = symbols[secureRandom.nextInt(symbols.length)]
        
        generatedPassword = "$w1-$w2-$w3-$num$sym"
        analyzePassword(generatedPassword, isForGenerator = true)
    }

    fun copyToClipboard(context: Context, text: String, isSensitive: Boolean = true) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("password", text)
        if (isSensitive) {
            clip.description.extras = PersistableBundle().apply {
                putBoolean("android.content.extra.IS_SENSITIVE", true)
            }
        }
        clipboard.setPrimaryClip(clip)

        val delay = securityPrefs.clipboardClearDelay
        if (delay > 0) {
            val request = OneTimeWorkRequestBuilder<ClipboardClearWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context).enqueue(request)
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(context, context.getString(R.string.notes_action_copy) + "!", Toast.LENGTH_SHORT).show()
        }
    }

    fun copyAndUse(context: Context) {
        copyToClipboard(context, generatedPassword)
        passwordInput = generatedPassword
        analyzePassword(passwordInput)
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(50)
        }
    }

    fun checkPwned() {
        val password = passwordInput
        if (password.isEmpty()) return

        isCheckingPwned = true
        viewModelScope.launch {
            val count = HibpManager.checkPwnedCount(password)
            strengthResult = strengthResult?.copy(pwnedCount = count)
            isCheckingPwned = false
            
            strengthResult?.let { res ->
                dao.insert(PasswordStrengthHistory(
                    score = res.score,
                    length = res.length,
                    crackTime = res.crackTime
                ))
            }
        }
    }

    fun saveToManager(entry: PasswordEntry) {
        viewModelScope.launch {
            val db = AppDatabase.getDatabase(getApplication())
            val encryptedEntry = entry.copy(
                passwordEncrypted = SecurityUtils.encrypt(entry.passwordEncrypted)
            )
            db.passwordDao().insertPassword(encryptedEntry)
        }
    }
}
