package com.herwin.netez.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.herwin.netez.ui.viewmodels.HttpHeaderViewModel

@Composable
fun HttpHeaderScreen(viewModel: HttpHeaderViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = uiState.url,
                    onValueChange = { viewModel.updateUrl(it) },
                    label = { Text("URL (e.g. google.com)") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                )
                Button(
                    onClick = { viewModel.fetchHeaders() },
                    enabled = !uiState.isLoading,
                    modifier = Modifier.height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("CHECK", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(uiState.headers.entries.toList()) { entry ->
                com.herwin.netez.ui.components.ResultRow(entry.key ?: "Status", entry.value.joinToString(", "))
            }
            
            if (uiState.headers.isNotEmpty() && !uiState.isLoading) {
                item {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    com.herwin.netez.ui.components.ResultFooter(
                        fullTextToCopy = uiState.headers.entries.joinToString("\n") { "${it.key}: ${it.value.joinToString(", ")}" },
                        onCopyAll = {
                            val text = uiState.headers.entries.joinToString("\n") { "${it.key}: ${it.value.joinToString(", ")}" }
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("HTTP Headers", text))
                            android.widget.Toast.makeText(context, "Headers copied", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}

