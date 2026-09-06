package com.herwin.netez.ui.viewmodels

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.herwin.netez.data.AppDatabase
import com.herwin.netez.data.PasswordEntry
import com.herwin.netez.data.SecurityPreferences
import com.herwin.netez.utils.SecurityUtils
import com.herwin.netez.worker.ClipboardClearWorker
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class PasswordManagerViewModel(application: Application) : AndroidViewModel(application) {
    private val passwordDao = AppDatabase.getDatabase(application).passwordDao()
    private val securityPrefs = SecurityPreferences(application)
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()
    
    private val _filter = MutableStateFlow(PasswordFilter.ALL)
    val filter = _filter.asStateFlow()

    val passwords = combine(
        passwordDao.getAllPasswords(),
        _searchQuery,
        _filter
    ) { list, query, filter ->
        list.filter {
            (it.appName.contains(query, ignoreCase = true) || it.username.contains(query, ignoreCase = true)) &&
            when (filter) {
                PasswordFilter.ALL -> true
                PasswordFilter.FAVORITE -> it.isFavorite
                PasswordFilter.WEAK -> it.strengthScore < 50
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stats = combine(
        passwordDao.getCount(),
        passwordDao.getWeakCount()
    ) { total, weak ->
        PasswordStats(total, weak, 0)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PasswordStats(0, 0, 0))

    fun isLocked(): Boolean {
        return securityPrefs.isAutoLockNeeded()
    }

    fun markAuthenticated() {
        securityPrefs.lastAuthTime = System.currentTimeMillis()
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onFilterChange(filter: PasswordFilter) {
        _filter.value = filter
    }

    fun savePassword(entry: PasswordEntry) {
        viewModelScope.launch {
            val encryptedEntry = entry.copy(
                passwordEncrypted = SecurityUtils.encrypt(entry.passwordEncrypted)
            )
            passwordDao.insertPassword(encryptedEntry)
        }
    }

    fun deletePassword(entry: PasswordEntry) {
        viewModelScope.launch {
            passwordDao.deletePassword(entry)
        }
    }

    fun toggleFavorite(entry: PasswordEntry) {
        viewModelScope.launch {
            passwordDao.updatePassword(entry.copy(isFavorite = !entry.isFavorite))
        }
    }

    fun decryptPassword(encrypted: String): String {
        return try {
            SecurityUtils.decrypt(encrypted)
        } catch (e: Exception) {
            "Error Decrypting"
        }
    }

    fun copyToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("password", text)
        clipboard.setPrimaryClip(clip)
        
        val delay = securityPrefs.clipboardClearDelay
        val request = OneTimeWorkRequestBuilder<ClipboardClearWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueue(request)
    }
}

enum class PasswordFilter { ALL, FAVORITE, WEAK }
data class PasswordStats(val total: Int, val weak: Int, val breached: Int)
