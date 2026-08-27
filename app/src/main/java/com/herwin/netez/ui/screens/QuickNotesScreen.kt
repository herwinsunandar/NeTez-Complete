package com.herwin.netez.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.herwin.netez.R
import com.herwin.netez.data.QuickNote
import com.herwin.netez.ui.theme.PremiumNavy
import com.herwin.netez.ui.theme.SoftDarkBg
import com.herwin.netez.ui.theme.TextGray
import com.herwin.netez.ui.viewmodels.QuickNotesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickNotesScreen(
    viewModel: QuickNotesViewModel = viewModel(),
    onPing: (String) -> Unit,
    onTraceroute: (String) -> Unit,
    onWhois: (String) -> Unit,
    onSslCheck: (String) -> Unit,
    onTestApi: (String) -> Unit
) {
    val notes by viewModel.notes.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filterType by viewModel.filterType.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var noteToEdit by remember { mutableStateOf<QuickNote?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = PremiumNavy,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Note")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Search Bar
            TextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text(stringResource(R.string.notes_search_hint), color = TextGray) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextGray) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = SoftDarkBg,
                    unfocusedContainerColor = SoftDarkBg,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(12.dp)
            )

            // Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filters = listOf("All", "Pinned", "IP", "Domain", "Note")
                items(filters) { filter ->
                    FilterChip(
                        selected = filterType == filter,
                        onClick = { viewModel.setFilterType(filter) },
                        label = { Text(filter) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White,
                            containerColor = SoftDarkBg,
                            labelColor = TextGray
                        ),
                        border = null
                    )
                }
            }

            // Notes List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(notes, key = { it.id }) { note ->
                    QuickNoteCard(
                        note = note,
                        onCopy = { viewModel.copyToClipboard(it) },
                        onPing = { onPing(note.title) },
                        onTraceroute = { onTraceroute(note.title) },
                        onWhois = { onWhois(note.title) },
                        onSslCheck = { onSslCheck(note.title) },
                        onTestApi = { onTestApi(it) },
                        onDelete = { viewModel.deleteNote(note) },
                        onTogglePin = { viewModel.togglePin(note) },
                        onEdit = { noteToEdit = note }
                    )
                }
                
                if (notes.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.notes_empty), color = TextGray)
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        QuickNoteDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, content, tag, pinned ->
                viewModel.addNote(title, content, tag, pinned)
                showAddDialog = false
            }
        )
    }

    noteToEdit?.let { note ->
        QuickNoteDialog(
            note = note,
            onDismiss = { noteToEdit = null },
            onConfirm = { title, content, tag, pinned ->
                viewModel.updateNote(note.copy(title = title, note = content, tag = tag, isPinned = pinned))
                noteToEdit = null
            }
        )
    }
}

@Composable
fun QuickNoteCard(
    note: QuickNote,
    onCopy: (String) -> Unit,
    onPing: () -> Unit,
    onTraceroute: () -> Unit,
    onWhois: () -> Unit,
    onSslCheck: () -> Unit,
    onTestApi: (String) -> Unit,
    onDelete: () -> Unit,
    onTogglePin: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        colors = CardDefaults.cardColors(containerColor = SoftDarkBg),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onCopy(note.title) }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Title", tint = Color.White)
                }
                
                SelectionContainer(modifier = Modifier.weight(1f)) {
                    Column {
                        Text(
                            text = note.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        if (note.note.isNotEmpty()) {
                            Text(
                                text = note.note,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextGray
                            )
                        }
                    }
                }

                if (note.isPinned) {
                    Icon(
                        Icons.Default.PushPin, 
                        contentDescription = "Pinned", 
                        tint = MaterialTheme.colorScheme.primary, 
                        modifier = Modifier.size(20.dp).clickable { onTogglePin() }
                    )
                } else {
                    IconButton(onClick = onTogglePin) {
                        Icon(Icons.Default.PushPin, contentDescription = "Pin", tint = TextGray.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                    }
                }
            }

            if (note.tag.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = note.tag,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { onCopy("${note.title}\n${note.note}") }) {
                    Text(stringResource(R.string.notes_action_copy), fontSize = 11.sp)
                }
                
                if (note.type == "IP" || note.type == "DOMAIN") {
                    TextButton(onClick = onPing) {
                        Text(stringResource(R.string.notes_action_ping), fontSize = 11.sp)
                    }
                    TextButton(onClick = onTraceroute) {
                        Text("TRACE", fontSize = 11.sp)
                    }
                    TextButton(onClick = onWhois) {
                        Text("WHOIS", fontSize = 11.sp)
                    }
                }
                
                if (note.type == "DOMAIN") {
                    TextButton(onClick = onSslCheck) {
                        Text("SSL", fontSize = 11.sp)
                    }
                    TextButton(onClick = { 
                        val url = if (note.title.startsWith("http")) note.title else "http://${note.title}"
                        onTestApi(url) 
                    }) {
                        Text(stringResource(R.string.notes_action_test_api), fontSize = 11.sp)
                    }
                }

                Spacer(Modifier.weight(1f))

                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun QuickNoteDialog(
    note: QuickNote? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, Boolean) -> Unit
) {
    var title by remember { mutableStateOf(note?.title ?: "") }
    var content by remember { mutableStateOf(note?.note ?: "") }
    var tag by remember { mutableStateOf(note?.tag ?: "") }
    var isPinned by remember { mutableStateOf(note?.isPinned ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (note == null) stringResource(R.string.notes_add_title) else stringResource(R.string.notes_edit_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.notes_field_title)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text(stringResource(R.string.notes_field_note)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = tag,
                    onValueChange = { tag = it },
                    label = { Text(stringResource(R.string.notes_field_tag)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isPinned, onCheckedChange = { isPinned = it })
                    Text(stringResource(R.string.notes_pin))
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(title, content, tag, isPinned) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
