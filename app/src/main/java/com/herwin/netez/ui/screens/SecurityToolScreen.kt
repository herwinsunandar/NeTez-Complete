package com.herwin.netez.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.herwin.netez.R
import com.herwin.netez.data.entities.SecurityToolHistory
import com.herwin.netez.engine.SecurityToolsEngine
import com.herwin.netez.ui.AppIcons
import com.herwin.netez.ui.theme.TechMonospace
import com.herwin.netez.ui.viewmodels.SecurityToolViewModel
import java.text.SimpleDateFormat
import java.util.*

import com.herwin.netez.entitlement.FeatureId
import com.herwin.netez.ui.components.ContextualPaywallDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityToolScreen(
    toolType: String,
    onBack: () -> Unit = {},
    viewModel: SecurityToolViewModel = viewModel()
) {
    val context = LocalContext.current
    var showHistorySheet by remember { mutableStateOf(false) }

    if (viewModel.showGlobalSupportDialog) {
        ContextualPaywallDialog(
            featureId = FeatureId.GLOBAL_TOOL_USAGE,
            limitText = "You've performed 3 network & security scans! If NeTez is helpful, consider watching a short video to support development or upgrade to PRO (Skippable).",
            onDismiss = { viewModel.showGlobalSupportDialog = false },
            onUpgradeClick = {}
        )
    }

    LaunchedEffect(toolType) {
        viewModel.initTool(toolType)
    }

    LaunchedEffect(viewModel.actionMessage) {
        viewModel.actionMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.actionMessage = null
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.onFileSelected(context, it) }
    }

    val toolTitle = when (toolType) {
        "HASH_CALCULATOR" -> stringResource(R.string.sectool_hash_title)
        "BASE64" -> stringResource(R.string.sectool_base64_title)
        "URL_ENCODER" -> stringResource(R.string.sectool_url_title)
        "JWT_DECODER" -> stringResource(R.string.sectool_jwt_title)
        "UUID_GENERATOR" -> stringResource(R.string.sectool_uuid_title)
        "RANDOM_TOKEN" -> stringResource(R.string.sectool_token_title)
        "HMAC_GENERATOR" -> stringResource(R.string.sectool_hmac_title)
        "CHECKSUM" -> stringResource(R.string.sectool_checksum_title)
        "TIMESTAMP" -> stringResource(R.string.sectool_timestamp_title)
        "REGEX" -> stringResource(R.string.sectool_regex_title)
        else -> "Security Tool"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row (Tool Title & History Button)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = toolTitle,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )

                    IconButton(
                        onClick = { showHistorySheet = true },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            painter = AppIcons.history(),
                            contentDescription = "History",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // 1. Input Section
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    tonalElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Input",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )

                        if (toolType == "REGEX") {
                            OutlinedTextField(
                                value = viewModel.regexPattern,
                                onValueChange = { viewModel.regexPattern = it },
                                placeholder = {
                                    Text(
                                        text = "Regex Pattern (e.g. \\d+)",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        fontSize = 13.sp
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }

                        if (toolType != "UUID_GENERATOR" && toolType != "RANDOM_TOKEN") {
                            OutlinedTextField(
                                value = viewModel.inputText,
                                onValueChange = { viewModel.inputText = it },
                                placeholder = {
                                    val hint = when (toolType) {
                                        "JWT_DECODER" -> "Paste JWT Token (eyJhbGci...)"
                                        "TIMESTAMP" -> "Enter Epoch timestamp or date"
                                        "HMAC_GENERATOR" -> "Text to authenticate..."
                                        "REGEX" -> "Test text..."
                                        else -> "Enter text to process..."
                                    }
                                    Text(
                                        text = hint,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        fontSize = 13.sp
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 90.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        // File picker for Hash, Base64, Checksum
                        if (toolType == "HASH_CALCULATOR" || toolType == "BASE64" || toolType == "CHECKSUM") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (viewModel.fileName != null) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                painter = AppIcons.folder(),
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                text = viewModel.fileName!!,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontFamily = TechMonospace,
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            IconButton(
                                                onClick = { viewModel.clearFile() },
                                                modifier = Modifier.size(18.dp)
                                            ) {
                                                Icon(
                                                    painter = AppIcons.close(),
                                                    contentDescription = "Clear",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = { filePickerLauncher.launch("*/*") },
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.primary
                                        ),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(painter = AppIcons.folder(), contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Pick File", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        if (toolType == "TIMESTAMP") {
                            Row(
                                horizontalArrangement = Arrangement.End,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                TextButton(
                                    onClick = {
                                        val now = SecurityToolsEngine.nowTimestamps()
                                        viewModel.inputText = now.epochSeconds.toString()
                                        viewModel.calculate(context)
                                    },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(painter = AppIcons.schedule(), contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Insert Current Time", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // 2. Options Section
            item {
                OptionsCard(toolType = toolType, viewModel = viewModel)
            }

            // 3. Action Button
            item {
                Button(
                    onClick = { viewModel.calculate(context) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !viewModel.isCalculating
                ) {
                    if (viewModel.isCalculating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        val btnLabel = when (toolType) {
                            "HASH_CALCULATOR", "CHECKSUM" -> "GENERATE HASH"
                            "BASE64", "URL_ENCODER" -> if (viewModel.isDecodeMode) "DECODE" else "ENCODE"
                            "JWT_DECODER" -> "DECODE JWT"
                            "UUID_GENERATOR" -> "GENERATE UUID"
                            "RANDOM_TOKEN" -> "GENERATE TOKEN"
                            "HMAC_GENERATOR" -> "GENERATE HMAC"
                            "TIMESTAMP" -> "CONVERT TIMESTAMP"
                            "REGEX" -> "TEST REGEX"
                            else -> "CALCULATE"
                        }
                        Text(btnLabel, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            // 4. Output Section
            item {
                OutputSection(viewModel = viewModel, context = context)
            }
        }
    }

    if (showHistorySheet) {
        HistoryBottomSheet(
            viewModel = viewModel,
            onDismiss = { showHistorySheet = false },
            onSelectHistory = { item ->
                viewModel.inputText = item.input
                viewModel.outputText = item.output
                showHistorySheet = false
            }
        )
    }
}

@Composable
private fun OptionsCard(toolType: String, viewModel: SecurityToolViewModel) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Options",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            when (toolType) {
                "HASH_CALCULATOR", "CHECKSUM" -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val algos = if (toolType == "CHECKSUM") listOf("SHA-256", "MD5", "CRC32", "ADLER32") else listOf("SHA-256", "MD5", "SHA-1", "SHA-512")
                        algos.forEach { algo ->
                            FilterChip(
                                selected = viewModel.selectedAlgo == algo,
                                onClick = { viewModel.selectedAlgo = algo },
                                label = { Text(algo, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
                "BASE64", "URL_ENCODER" -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !viewModel.isDecodeMode,
                            onClick = { viewModel.isDecodeMode = false },
                            label = { Text("Encode", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        FilterChip(
                            selected = viewModel.isDecodeMode,
                            onClick = { viewModel.isDecodeMode = true },
                            label = { Text("Decode", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    if (toolType == "BASE64" && !viewModel.isDecodeMode) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "URL Safe Base64",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                            Switch(
                                checked = viewModel.isUrlSafe,
                                onCheckedChange = { viewModel.isUrlSafe = it }
                            )
                        }
                    }
                }
                "HMAC_GENERATOR" -> {
                    OutlinedTextField(
                        value = viewModel.hmacKey,
                        onValueChange = { viewModel.hmacKey = it },
                        placeholder = {
                            Text(
                                text = "Secret Key...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                fontSize = 13.sp
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("HmacSHA256", "HmacSHA512", "HmacSHA1", "HmacMD5").forEach { algo ->
                            FilterChip(
                                selected = viewModel.selectedAlgo == algo,
                                onClick = { viewModel.selectedAlgo = algo },
                                label = { Text(algo, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
                "UUID_GENERATOR" -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = viewModel.uuidVersion == 4,
                            onClick = { viewModel.uuidVersion = 4 },
                            label = { Text("v4 (Random)", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        FilterChip(
                            selected = viewModel.uuidVersion == 1,
                            onClick = { viewModel.uuidVersion = 1 },
                            label = { Text("v1 (Time-based)", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
                "RANDOM_TOKEN" -> {
                    Text(
                        text = "Length: ${viewModel.tokenLength}",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = TechMonospace,
                        fontSize = 12.sp
                    )
                    Slider(
                        value = viewModel.tokenLength.toFloat(),
                        onValueChange = { viewModel.tokenLength = it.toInt() },
                        valueRange = 8f..128f
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("ALPHANUMERIC", "HEX", "NUMERIC", "ALPHA").forEach { cs ->
                            FilterChip(
                                selected = viewModel.tokenCharset == cs,
                                onClick = { viewModel.tokenCharset = cs },
                                label = { Text(cs, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Include Special Symbols (!@#$)",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                        Switch(
                            checked = viewModel.tokenSymbols,
                            onCheckedChange = { viewModel.tokenSymbols = it }
                        )
                    }
                }
                "REGEX" -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = viewModel.regexCaseInsensitive,
                            onClick = { viewModel.regexCaseInsensitive = !viewModel.regexCaseInsensitive },
                            label = { Text("Case Insensitive (i)", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        FilterChip(
                            selected = viewModel.regexMultiline,
                            onClick = { viewModel.regexMultiline = !viewModel.regexMultiline },
                            label = { Text("Multiline (m)", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        FilterChip(
                            selected = viewModel.regexDotAll,
                            onClick = { viewModel.regexDotAll = !viewModel.regexDotAll },
                            label = { Text("DotAll (s)", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
                else -> {
                    Text(
                        text = "Default Options",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun OutputSection(viewModel: SecurityToolViewModel, context: Context) {
    if (viewModel.outputError != null) {
        Surface(
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
        ) {
            Text(
                text = viewModel.outputError!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(14.dp),
                fontSize = 13.sp
            )
        }
    }

    if (viewModel.outputText.isNotEmpty()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Output Result",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )

                    viewModel.tokenResult?.let { tr ->
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "%.0f bits entropy".format(tr.entropyBits),
                                color = MaterialTheme.colorScheme.secondary,
                                fontFamily = TechMonospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    viewModel.jwtResult?.let { jwt ->
                        val statusColor = if (jwt.isExpired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                        Surface(
                            color = statusColor.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, statusColor.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (jwt.isExpired) "EXPIRED 🔴" else "VALID 🟢",
                                color = statusColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = viewModel.outputText,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = TechMonospace,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                // Perfectly balanced 3 action buttons row with equal size & padding
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.copyToClipboard(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            painter = AppIcons.contentCopy(),
                            contentDescription = "Copy",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "COPY",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    OutlinedButton(
                        onClick = { viewModel.shareOutput(context) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            painter = AppIcons.share(),
                            contentDescription = "Share",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "SHARE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    OutlinedButton(
                        onClick = { viewModel.saveToQuickNotes() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            painter = AppIcons.stickyNote(),
                            contentDescription = "Save Note",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "SAVE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryBottomSheet(
    viewModel: SecurityToolViewModel,
    onDismiss: () -> Unit,
    onSelectHistory: (SecurityToolHistory) -> Unit
) {
    val historyList by viewModel.historyList.collectAsState()
    val sdf = SimpleDateFormat("dd MMM HH:mm", Locale.getDefault())

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Calculation History",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                TextButton(
                    onClick = { viewModel.clearToolHistory() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear History", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (historyList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No history entries recorded yet",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 350.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(historyList) { item ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectHistory(item) },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = item.toolType,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontFamily = TechMonospace,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = sdf.format(Date(item.timestamp)),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 10.sp
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = item.output,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 12.sp,
                                    fontFamily = TechMonospace,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
