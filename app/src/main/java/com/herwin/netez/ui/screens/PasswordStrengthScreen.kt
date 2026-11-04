package com.herwin.netez.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.herwin.netez.R
import com.herwin.netez.ui.AppIcons
import com.herwin.netez.ui.components.AddPasswordBottomSheet
import com.herwin.netez.ui.theme.TechMonospace
import com.herwin.netez.ui.viewmodels.PasswordStrengthViewModel

import com.herwin.netez.entitlement.FeatureId
import com.herwin.netez.ui.components.ContextualPaywallDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordStrengthScreen(
    onBack: () -> Unit = {},
    viewModel: PasswordStrengthViewModel = viewModel()
) {
    val result = viewModel.strengthResult
    var showSaveDialog by remember { mutableStateOf(false) }

    if (viewModel.showGlobalSupportDialog) {
        ContextualPaywallDialog(
            featureId = FeatureId.GLOBAL_TOOL_USAGE,
            limitText = "You've performed 3 network & security scans! If NeTez is helpful, consider watching a short video to support development or upgrade to PRO (Skippable).",
            onDismiss = { viewModel.showGlobalSupportDialog = false },
            onUpgradeClick = {}
        )
    }

    if (showSaveDialog) {
        AddPasswordBottomSheet(
            prefilledPassword = if (viewModel.isGeneratorExpanded) viewModel.generatedPassword else viewModel.passwordInput,
            prefilledStrength = (if (viewModel.isGeneratorExpanded) viewModel.generatedStrength else viewModel.strengthResult)?.score ?: 0,
            onDismiss = { showSaveDialog = false },
            onSave = { entry ->
                viewModel.saveToManager(entry)
                showSaveDialog = false
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.pass_strength_warning),
                        color = MaterialTheme.colorScheme.tertiary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Input Card
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
                        OutlinedTextField(
                            value = viewModel.passwordInput,
                            onValueChange = { viewModel.onPasswordChange(it) },
                            placeholder = {
                                Text(
                                    text = "Type password to analyze...",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    fontSize = 13.sp
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = if (viewModel.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            ),
                            trailingIcon = {
                                Row {
                                    IconButton(onClick = { viewModel.isPasswordVisible = !viewModel.isPasswordVisible }) {
                                        Icon(
                                            painter = if (viewModel.isPasswordVisible) AppIcons.visibilityOff() else AppIcons.visibility(),
                                            contentDescription = "Toggle visibility",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (viewModel.passwordInput.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.onPasswordChange("") }) {
                                            Icon(
                                                painter = AppIcons.close(),
                                                contentDescription = "Clear",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { viewModel.toggleGenerator() },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    painter = AppIcons.key(),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.pass_generate),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Button(
                            onClick = { viewModel.checkPwned() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp),
                            enabled = viewModel.passwordInput.isNotEmpty() && !viewModel.isCheckingPwned
                        ) {
                            if (viewModel.isCheckingPwned) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    painter = AppIcons.shieldCheck(),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.pass_check_leak),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        if (viewModel.passwordInput.isNotEmpty()) {
                            val score = result?.score ?: 0
                            val label = when {
                                score >= 80 -> "STRONG"
                                score >= 60 -> "GOOD"
                                score >= 40 -> "FAIR"
                                else -> "WEAK"
                            }
                            val color = when {
                                score >= 80 -> MaterialTheme.colorScheme.secondary
                                score >= 60 -> MaterialTheme.colorScheme.tertiary
                                score >= 40 -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.error
                            }

                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "$label $score/100 • " + stringResource(R.string.pass_crack_time, result?.crackTime ?: "..."),
                                        fontSize = 11.sp,
                                        fontFamily = TechMonospace,
                                        color = color,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { score / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(CircleShape),
                                    color = color,
                                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                )
                            }
                        }
                    }
                }
            }

            if (result != null) {
                item {
                    val statusColor = when {
                        result.score >= 80 -> MaterialTheme.colorScheme.secondary
                        result.score >= 60 -> MaterialTheme.colorScheme.tertiary
                        result.score >= 40 -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.error
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                        tonalElevation = 2.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Surface(
                                    color = statusColor.copy(alpha = 0.15f),
                                    shape = CircleShape,
                                    border = BorderStroke(1.dp, statusColor)
                                ) {
                                    Box(
                                        modifier = Modifier.size(44.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = if (result.score >= 60) AppIcons.shield() else AppIcons.warning(),
                                            contentDescription = null,
                                            tint = statusColor,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }

                                Column {
                                    Text(
                                        text = if (result.score >= 80) "STRONG" else if (result.score >= 40) "FAIR" else "WEAK",
                                        color = statusColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                    Text(
                                        text = stringResource(R.string.pass_crack_time, result.crackTime),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontFamily = TechMonospace,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Spacer(Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = stringResource(R.string.pass_score_label),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${result.score}/100",
                                    fontSize = 11.sp,
                                    fontFamily = TechMonospace,
                                    color = statusColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { result.score / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape),
                                color = statusColor,
                                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            )
                        }
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        StrengthDetailItem(
                            label = stringResource(R.string.pass_length),
                            value = stringResource(R.string.pass_stat_chars, result.length),
                            isPassed = result.length >= 12
                        )
                        StrengthDetailItem(
                            label = stringResource(R.string.pass_complexity),
                            value = buildString {
                                if (result.hasUpper) append(stringResource(R.string.pass_upper) + ", ")
                                if (result.hasLower) append(stringResource(R.string.pass_lower) + ", ")
                                if (result.hasDigit) append(stringResource(R.string.pass_number) + ", ")
                                if (result.hasSymbol) append(stringResource(R.string.pass_symbol))
                            }.removeSuffix(", "),
                            isPassed = result.hasUpper && result.hasLower && (result.hasDigit || result.hasSymbol)
                        )

                        if (result.pwnedCount != -2) {
                            StrengthDetailItem(
                                label = stringResource(R.string.pass_breach_check),
                                value = when {
                                    result.pwnedCount == -1 -> stringResource(R.string.pass_hibp_fail)
                                    result.pwnedCount == 0 -> stringResource(R.string.pass_hibp_clean)
                                    else -> stringResource(R.string.pass_hibp_found, result.pwnedCount)
                                },
                                isPassed = result.pwnedCount == 0
                            )
                        }
                    }
                }

                if (result.suggestions.isNotEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                            tonalElevation = 2.dp
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = stringResource(R.string.pass_suggestions),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(Modifier.height(8.dp))
                                result.suggestions.forEach { suggestion ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    ) {
                                        Icon(
                                            painter = AppIcons.info(),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = suggestion,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                AnimatedVisibility(
                    visible = viewModel.isGeneratorExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    PasswordGeneratorPanel(viewModel, onSaveToManager = { showSaveDialog = true })
                }
            }

            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}

@Composable
fun StrengthDetailItem(label: String, value: String, isPassed: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = value,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = TechMonospace,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.width(8.dp))

            Icon(
                painter = if (isPassed) AppIcons.checkCircle() else AppIcons.cancel(),
                contentDescription = null,
                tint = if (isPassed) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun PasswordGeneratorPanel(viewModel: PasswordStrengthViewModel, onSaveToManager: () -> Unit) {
    val context = LocalContext.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = viewModel.generatedPassword.ifEmpty { stringResource(R.string.pass_gen_generating) },
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = TechMonospace,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { viewModel.generateNewPassword() }) {
                    Icon(
                        painter = AppIcons.refresh(),
                        contentDescription = "Regenerate",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            viewModel.generatedStrength?.let { strength ->
                val color = when {
                    strength.score >= 80 -> MaterialTheme.colorScheme.secondary
                    strength.score >= 60 -> MaterialTheme.colorScheme.tertiary
                    strength.score >= 40 -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.error
                }
                Text(
                    text = "${strength.score}/100 ${if (strength.score >= 80) "STRONG" else "FAIR"} • " + stringResource(R.string.pass_crack_time, strength.crackTime),
                    color = color,
                    fontFamily = TechMonospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = stringResource(R.string.pass_length) + ": ${viewModel.genLength.toInt()}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
            Slider(
                value = viewModel.genLength,
                onValueChange = {
                    viewModel.genLength = it
                    viewModel.generateNewPassword()
                },
                valueRange = 8f..64f,
                steps = 56
            )

            Spacer(Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                GeneratorSwitch(stringResource(R.string.pass_gen_upper), viewModel.genUpper) { viewModel.genUpper = it; viewModel.generateNewPassword() }
                GeneratorSwitch(stringResource(R.string.pass_gen_lower), viewModel.genLower) { viewModel.genLower = it; viewModel.generateNewPassword() }
                GeneratorSwitch(stringResource(R.string.pass_gen_numbers), viewModel.genNumbers) { viewModel.genNumbers = it; viewModel.generateNewPassword() }
                GeneratorSwitch(stringResource(R.string.pass_gen_symbols), viewModel.genSymbols) { viewModel.genSymbols = it; viewModel.generateNewPassword() }
                GeneratorSwitch(stringResource(R.string.pass_gen_ambiguous), viewModel.genExcludeAmbiguous) { viewModel.genExcludeAmbiguous = it; viewModel.generateNewPassword() }
            }

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(12, 16, 20, 32).forEach { len ->
                    FilterChip(
                        selected = viewModel.genLength.toInt() == len,
                        onClick = { viewModel.genLength = len.toFloat(); viewModel.generateNewPassword() },
                        label = { Text(len.toString(), fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
                FilterChip(
                    selected = false,
                    onClick = { viewModel.generateMemorable() },
                    label = { Text(stringResource(R.string.pass_gen_memorable), fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { viewModel.copyAndUse(context) },
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = "COPY & USE",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                OutlinedButton(
                    onClick = { onSaveToManager() },
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = "SAVE VAULT",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

@Composable
fun GeneratorSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .toggleable(value = checked, onValueChange = onCheckedChange)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
        Switch(
            checked = checked,
            onCheckedChange = null,
            modifier = Modifier.scale(0.7f)
        )
    }
}
