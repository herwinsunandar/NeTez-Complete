package com.herwin.netez.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.herwin.netez.R
import com.herwin.netez.data.PasswordEntry
import com.herwin.netez.ui.AppIcons
import com.herwin.netez.ui.components.AddPasswordBottomSheet
import com.herwin.netez.ui.theme.PremiumNavy
import com.herwin.netez.ui.theme.SoftDarkBg
import com.herwin.netez.ui.theme.TextGray
import com.herwin.netez.ui.viewmodels.PasswordFilter
import com.herwin.netez.ui.viewmodels.PasswordManagerViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordManagerScreen(
    onBack: () -> Unit,
    viewModel: PasswordManagerViewModel = viewModel()
) {
    val passwords by viewModel.passwords.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFilter by viewModel.filter.collectAsState()
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }

    if (showAddDialog) {
        AddPasswordBottomSheet(
            onDismiss = { showAddDialog = false },
            onSave = { entry ->
                viewModel.savePassword(entry)
                showAddDialog = false
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFF6C8CFF),
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(AppIcons.add(), null)
            }
        },
        containerColor = PremiumNavy
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatChip(stringResource(R.string.pass_stat_saved, stats.total), Color(0xFF6C8CFF))
                StatChip(stringResource(R.string.pass_stat_weak, stats.weak), Color(0xFFEF4444))
                StatChip(stringResource(R.string.pass_stat_breached, stats.breached), Color(0xFFF59E0B))
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                placeholder = { Text(stringResource(R.string.pass_search_hint), color = TextGray.copy(alpha = 0.5f)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                leadingIcon = { Icon(AppIcons.search(), null, tint = TextGray) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF6C8CFF),
                    unfocusedBorderColor = TextGray
                )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PasswordFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { viewModel.onFilterChange(filter) },
                        label = { Text(filter.name, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF6C8CFF).copy(alpha = 0.2f),
                            selectedLabelColor = Color(0xFF6C8CFF)
                        )
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (passwords.isEmpty()) {
                    item {
                        Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(AppIcons.lock(), null, tint = TextGray.copy(alpha = 0.2f), modifier = Modifier.size(64.dp))
                                Text(stringResource(R.string.pass_empty_list), color = TextGray)
                            }
                        }
                    }
                } else {
                    items(passwords, key = { it.id }) { entry ->
                        PasswordCard(entry, viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun PasswordCard(entry: PasswordEntry, viewModel: PasswordManagerViewModel) {
    var isPasswordVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SoftDarkBg),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF97316).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(entry.appName.take(1).uppercase(), color = Color(0xFFF97316), fontWeight = FontWeight.Bold)
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(entry.appName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    if (entry.isFavorite) {
                        Spacer(Modifier.width(4.dp))
                        Icon(AppIcons.star(), null, tint = Color(0xFFF59E0B), modifier = Modifier.size(14.dp))
                    }
                }
                Text(entry.username, color = TextGray, fontSize = 12.sp)
                
                Spacer(Modifier.height(4.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (isPasswordVisible) viewModel.decryptPassword(entry.passwordEncrypted) else "••••••••",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${entry.strengthScore}/100",
                        color = if (entry.strengthScore >= 80) Color(0xFF10B981) else Color(0xFFEF4444),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Text(
                    text = stringResource(R.string.pass_updated_date, formatDate(entry.updatedAt)),
                    color = TextGray.copy(alpha = 0.5f),
                    fontSize = 10.sp
                )
            }
            
            Row {
                IconButton(onClick = { 
                    isPasswordVisible = !isPasswordVisible 
                }) {
                    Icon(if (isPasswordVisible) AppIcons.visibilityOff() else AppIcons.visibility(), null, tint = TextGray, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = { viewModel.copyToClipboard(context, viewModel.decryptPassword(entry.passwordEncrypted)) }) {
                    Icon(AppIcons.contentCopy(), null, tint = TextGray, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = {  }) {
                    Icon(AppIcons.moreVert(), null, tint = TextGray, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun StatChip(label: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Text(
            label,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
