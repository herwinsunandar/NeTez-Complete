package com.herwin.netez.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.herwin.netez.data.AppDatabase
import com.herwin.netez.data.TestResult
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).testResultDao()

    val allResults: StateFlow<List<TestResult>> = dao.getAllResults()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addResult(type: String, target: String, result: String) {
        viewModelScope.launch {
            dao.insertResult(TestResult(type = type, target = target, result = result))
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            dao.deleteAll()
        }
    }
}
