package com.herwin.netez.ui.viewmodels

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.herwin.netez.data.AppDatabase
import com.herwin.netez.data.QuickNote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class QuickNotesViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).quickNoteDao()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterType = MutableStateFlow("All")
    val filterType: StateFlow<String> = _filterType.asStateFlow()

    val notes: StateFlow<List<QuickNote>> = combine(
        _searchQuery,
        _filterType,
        dao.getAllNotes()
    ) { query, filter, allNotes ->
        val filtered = if (query.isEmpty()) {
            allNotes
        } else {
            allNotes.filter { 
                it.title.contains(query, ignoreCase = true) || 
                it.note.contains(query, ignoreCase = true) || 
                it.tag.contains(query, ignoreCase = true) 
            }
        }

        when (filter) {
            "Pinned" -> filtered.filter { it.isPinned }
            "IP" -> filtered.filter { it.type == "IP" }
            "Domain" -> filtered.filter { it.type == "DOMAIN" }
            "Note" -> filtered.filter { it.type == "NOTE" }
            else -> filtered
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilterType(type: String) {
        _filterType.value = type
    }

    fun addNote(title: String, noteContent: String, tag: String, isPinned: Boolean) {
        val type = detectType(title)
        val newNote = QuickNote(
            title = title,
            note = noteContent,
            tag = tag,
            type = type,
            isPinned = isPinned
        )
        viewModelScope.launch(Dispatchers.IO) {
            dao.insert(newNote)
        }
    }

    fun updateNote(note: QuickNote) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.update(note.copy(type = detectType(note.title)))
        }
    }

    fun deleteNote(note: QuickNote) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.delete(note)
        }
    }

    fun togglePin(note: QuickNote) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.update(note.copy(isPinned = !note.isPinned))
        }
    }

    private fun detectType(title: String): String {
        val ipRegex = Regex("""^(\d{1,3}\.){3}\d{1,3}$""")
        val domainRegex = Regex("""^([a-zA-Z0-9-]+\.)+[a-zA-Z]{2,}$""")
        return when {
            ipRegex.matches(title) -> "IP"
            domainRegex.matches(title) || title.startsWith("http") -> "DOMAIN"
            else -> "NOTE"
        }
    }

    fun copyToClipboard(text: String) {
        val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("QuickNote", text)
        clipboard.setPrimaryClip(clip)
    }

    fun exportToJson(): String {
        val array = JSONArray()
        notes.value.forEach { note ->
            val obj = JSONObject().apply {
                put("title", note.title)
                put("note", note.note)
                put("tag", note.tag)
                put("type", note.type)
                put("isPinned", note.isPinned)
                put("timestamp", note.timestamp)
            }
            array.put(obj)
        }
        return array.toString(4)
    }

    fun importFromJson(json: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val array = JSONArray(json)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val note = QuickNote(
                        title = obj.getString("title"),
                        note = obj.getString("note"),
                        tag = obj.getString("tag"),
                        type = obj.getString("type"),
                        isPinned = obj.getBoolean("isPinned"),
                        timestamp = obj.getLong("timestamp")
                    )
                    dao.insert(note)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
