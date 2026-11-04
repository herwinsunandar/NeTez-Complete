package com.herwin.netez.ui.viewmodels

import com.herwin.netez.entitlement.FeatureAccessManager
import com.herwin.netez.entitlement.FeatureAccessState
import com.herwin.netez.entitlement.FeatureId

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.PersistableBundle
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.herwin.netez.data.AppDatabase
import com.herwin.netez.data.SecurityPreferences
import com.herwin.netez.data.VaultEntry
import com.herwin.netez.utils.SecurityUtils
import com.herwin.netez.worker.ClipboardClearWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

enum class VaultTypeFilter {
    ALL, WEBSITE, SERVER, SSH, SFTP, API, DATABASE, NETWORK, NOTES
}

data class VaultStats(
    val total: Int = 0,
    val weak: Int = 0,
    val servers: Int = 0
)

class PasswordManagerViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val vaultDao = db.passwordDao()
    private val securityPrefs = SecurityPreferences(application)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedType = MutableStateFlow(VaultTypeFilter.ALL)
    val selectedType = _selectedType.asStateFlow()

    var actionMessage by mutableStateOf<String?>(null)
    var showPaywallDialog by mutableStateOf(false)
    var paywallLimitText by mutableStateOf("")

    val vaultEntries = combine(
        vaultDao.getAllEntries(),
        _searchQuery,
        _selectedType
    ) { list, query, typeFilter ->
        list.filter { entry ->
            val matchQuery = query.isBlank() ||
                    entry.title.contains(query, ignoreCase = true) ||
                    entry.appName.contains(query, ignoreCase = true) ||
                    entry.username.contains(query, ignoreCase = true) ||
                    (entry.hostnameOrIp?.contains(query, ignoreCase = true) == true)

            val matchType = when (typeFilter) {
                VaultTypeFilter.ALL -> true
                VaultTypeFilter.WEBSITE -> entry.credentialType == "WEBSITE"
                VaultTypeFilter.SERVER -> entry.credentialType == "SERVER"
                VaultTypeFilter.SSH -> entry.credentialType == "SSH"
                VaultTypeFilter.SFTP -> entry.credentialType == "SFTP"
                VaultTypeFilter.API -> entry.credentialType == "API"
                VaultTypeFilter.DATABASE -> entry.credentialType == "DATABASE"
                VaultTypeFilter.NETWORK -> entry.credentialType == "NETWORK"
                VaultTypeFilter.NOTES -> entry.credentialType == "NOTES"
            }

            matchQuery && matchType
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stats = combine(
        vaultDao.getAllEntries(),
        vaultDao.getWeakCount()
    ) { list, weak ->
        val serverCount = list.count { it.credentialType == "SERVER" || it.credentialType == "SSH" || it.credentialType == "SFTP" }
        VaultStats(
            total = list.size,
            weak = weak,
            servers = serverCount
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), VaultStats())

    fun isLocked(): Boolean {
        return securityPrefs.isAutoLockNeeded()
    }

    fun markAuthenticated() {
        securityPrefs.lastAuthTime = System.currentTimeMillis()
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onTypeFilterChange(filter: VaultTypeFilter) {
        _selectedType.value = filter
    }

    fun saveEntry(entry: VaultEntry) {
        val accessManager = FeatureAccessManager.getInstance(getApplication())
        viewModelScope.launch(Dispatchers.IO) {
            val isNew = db.passwordDao().getEntryById(entry.id) == null
            val currentCount = vaultEntries.value.size

            if (isNew) {
                val capacityState = accessManager.checkAccess(FeatureId.CREDENTIAL_VAULT_MAX, currentCount)
                if (capacityState == FeatureAccessState.LIMIT_REACHED) {
                    withContext(Dispatchers.Main) {
                        paywallLimitText = "Free plan supports up to 10 credentials in vault. Upgrade to NeTez PRO for unlimited vault storage."
                        showPaywallDialog = true
                    }
                    return@launch
                }
            }

            val isAdvancedType = entry.credentialType == "API" || entry.credentialType == "DATABASE" || entry.credentialType == "NETWORK" || entry.credentialType == "CUSTOM"
            if (isAdvancedType && !accessManager.isPro()) {
                withContext(Dispatchers.Main) {
                    paywallLimitText = "API, Database, Network Device, and Custom secrets are available in NeTez PRO."
                    showPaywallDialog = true
                }
                return@launch
            }

            val titleToSave = entry.title.ifBlank { entry.appName.ifBlank { entry.hostnameOrIp ?: "Untitled" } }
            val encryptedEntry = entry.copy(
                title = titleToSave,
                appName = entry.appName.ifBlank { titleToSave },
                passwordEncrypted = if (entry.passwordEncrypted.isNotBlank()) SecurityUtils.encrypt(entry.passwordEncrypted) else "",
                privateKeyEncrypted = if (!entry.privateKeyEncrypted.isNullOrBlank()) SecurityUtils.encrypt(entry.privateKeyEncrypted) else null,
                keyPassphraseEncrypted = if (!entry.keyPassphraseEncrypted.isNullOrBlank()) SecurityUtils.encrypt(entry.keyPassphraseEncrypted) else null,
                apiTokenEncrypted = if (!entry.apiTokenEncrypted.isNullOrBlank()) SecurityUtils.encrypt(entry.apiTokenEncrypted) else null,
                notesEncrypted = if (!entry.notesEncrypted.isNullOrBlank()) SecurityUtils.encrypt(entry.notesEncrypted) else null,
                customFieldsJsonEncrypted = if (!entry.customFieldsJsonEncrypted.isNullOrBlank()) SecurityUtils.encrypt(entry.customFieldsJsonEncrypted) else null,
                updatedAt = System.currentTimeMillis()
            )
            vaultDao.insertEntry(encryptedEntry)
            withContext(Dispatchers.Main) {
                actionMessage = "Credential saved successfully"
            }
        }
    }

    fun deleteEntry(entry: VaultEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            vaultDao.deleteEntry(entry)
            withContext(Dispatchers.Main) {
                actionMessage = "Credential deleted"
            }
        }
    }

    fun toggleFavorite(entry: VaultEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            vaultDao.updateEntry(entry.copy(isFavorite = !entry.isFavorite, updatedAt = System.currentTimeMillis()))
        }
    }

    fun decryptSecret(encrypted: String?): String {
        if (encrypted.isNullOrBlank()) return ""
        return try {
            SecurityUtils.decrypt(encrypted)
        } catch (e: Exception) {
            "Error Decrypting"
        }
    }

    suspend fun readTextFromUri(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader().use { it.readText() }
            } ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    fun copyToClipboard(context: Context, text: String, label: String = "password") {
        if (text.isBlank()) return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            clip.description.extras = PersistableBundle().apply {
                putBoolean("android.content.extra.IS_SENSITIVE", true)
            }
        }
        clipboard.setPrimaryClip(clip)
        actionMessage = "Copied to clipboard"

        val delay = securityPrefs.clipboardClearDelay
        if (delay > 0) {
            val request = OneTimeWorkRequestBuilder<ClipboardClearWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
