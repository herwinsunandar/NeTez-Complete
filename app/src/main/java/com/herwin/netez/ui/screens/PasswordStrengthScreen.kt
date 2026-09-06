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
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.herwin.netez.R
import com.herwin.netez.ui.AppIcons
import com.herwin.netez.ui.theme.PremiumNavy
import com.herwin.netez.ui.theme.SoftDarkBg
import com.herwin.netez.ui.theme.TextGray
import com.herwin.netez.ui.viewmodels.PasswordStrengthViewModel
import com.herwin.netez.ui.viewmodels.StrengthResult
import com.herwin.netez.ui.components.AddPasswordBottomSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordStrengthScreen(
    onBack: () -> Unit,
    viewModel: PasswordStrengthViewModel = viewModel()
) {
    val context = LocalContext.current
    val result = viewModel.strengthResult
    var showSaveDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val savedMsg = stringResource(R.string.pass_save_success)

    if (showSaveDialog) {
        AddPasswordBottomSheet(
            prefilledPassword = if (viewModel.isGeneratorExpanded) viewModel.generatedPassword else viewModel.passwordInput,
            prefilledStrength = (if (viewModel.isGeneratorExpanded) viewModel.generatedStrength else viewModel.strengthResult)?.score ?: 0,
            onDismiss = { showSaveDialog = false },
            onSave = { entry ->
                viewModel.saveToManager(entry)
                showSaveDialog = false
                scope.launch {
                    snackbarHostState.showSnackbar(savedMsg)
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PremiumNavy)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.pass_strength_warning),
                    color = Color.Yellow.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SoftDarkBg),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = viewModel.passwordInput,
                            onValueChange = { viewModel.onPasswordChange(it) },
                            label = { Text(stringResource(R.string.pass_input_label), color = TextGray) },
                            placeholder = { Text(stringResource(R.string.pass_input_placeholder), color = TextGray.copy(alpha = 0.5f)) },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = if (viewModel.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF6C8CFF),
                                unfocusedBorderColor = TextGray
                            ),
                            trailingIcon = {
                                Row {
                                    IconButton(onClick = { viewModel.isPasswordVisible = !viewModel.isPasswordVisible }) {
                                        Icon(
                                            painter = if (viewModel.isPasswordVisible) AppIcons.visibilityOff() else AppIcons.visibility(),
                                            contentDescription = null,
                                            tint = TextGray
                                        )
                                    }
                                    if (viewModel.passwordInput.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.onPasswordChange("") }) {
                                            Icon(AppIcons.close(), contentDescription = null, tint = TextGray)
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
                                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF6C8CFF))
                            ) {
                                Text(stringResource(R.string.pass_generate), fontSize = 12.sp)
                            }
                        }

                        Button(
                            onClick = { viewModel.checkPwned() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C8CFF)),
                            shape = RoundedCornerShape(12.dp),
                            enabled = viewModel.passwordInput.isNotEmpty() && !viewModel.isCheckingPwned
                        ) {
                            if (viewModel.isCheckingPwned) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Text(stringResource(R.string.pass_check_leak), fontWeight = FontWeight.Bold)
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
                                score >= 80 -> Color(0xFF10B981)
                                score >= 60 -> Color(0xFFF59E0B)
                                score >= 40 -> Color(0xFFF97316)
                                else -> Color(0xFFEF4444)
                            }

                            Column {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("$label $score/100 • " + stringResource(R.string.pass_crack_time, result?.crackTime ?: "..."), fontSize = 10.sp, color = color, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { score / 100f },
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                                    color = color,
                                    trackColor = Color.White.copy(alpha = 0.05f)
                                )
                            }
                        }
                    }
                }
            }

            if (result != null) {
                item {
                    val statusColor = when {
                        result.score >= 80 -> Color(0xFF10B981)
                        result.score >= 60 -> Color(0xFFF59E0B)
                        result.score >= 40 -> Color(0xFFF97316)
                        else -> Color(0xFFEF4444)
                    }
                    val bandColor = statusColor.copy(alpha = 0.1f)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SoftDarkBg),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Surface(
                                    color = bandColor,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        painter = if (result.score >= 60) AppIcons.shield() else AppIcons.warning(),
                                        contentDescription = null,
                                        tint = statusColor,
                                        modifier = Modifier.padding(12.dp).fillMaxSize()
                                    )
                                }
                                
                                Column {
                                    Text(
                                        text = if (result.score >= 80) "STRONG" else if (result.score >= 40) "FAIR" else "WEAK",
                                        color = statusColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp
                                    )
                                    Text(text = stringResource(R.string.pass_crack_time, result.crackTime), color = Color.White, fontSize = 12.sp)
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(stringResource(R.string.pass_score_label), fontSize = 10.sp, color = TextGray)
                                Text("${result.score}/100", fontSize = 10.sp, color = statusColor, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { result.score / 100f },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                                color = statusColor,
                                trackColor = Color.White.copy(alpha = 0.05f)
                            )
                        }
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        StrengthDetailItem(
                            label = stringResource(R.string.pass_length),
                            value = stringResource(R.string.pass_stat_chars, result.length),
                            isPassed = result.length >= 12,
                            onCopy = { viewModel.copyToClipboard(context, result.length.toString()) }
                        )
                        StrengthDetailItem(
                            label = stringResource(R.string.pass_complexity),
                            value = buildString {
                                if (result.hasUpper) append(stringResource(R.string.pass_upper) + ", ")
                                if (result.hasLower) append(stringResource(R.string.pass_lower) + ", ")
                                if (result.hasDigit) append(stringResource(R.string.pass_number) + ", ")
                                if (result.hasSymbol) append(stringResource(R.string.pass_symbol))
                            }.removeSuffix(", "),
                            isPassed = result.hasUpper && result.hasLower && (result.hasDigit || result.hasSymbol),
                            onCopy = { viewModel.copyToClipboard(context, "Complexity: ${result.hasUpper}, ${result.hasLower}, ${result.hasDigit}, ${result.hasSymbol}") }
                        )
                        
                        if (result.pwnedCount != -2) {
                            StrengthDetailItem(
                                label = stringResource(R.string.pass_breach_check),
                                value = when {
                                    result.pwnedCount == -1 -> stringResource(R.string.pass_hibp_fail)
                                    result.pwnedCount == 0 -> stringResource(R.string.pass_hibp_clean)
                                    else -> stringResource(R.string.pass_hibp_found, result.pwnedCount)
                                },
                                isPassed = result.pwnedCount == 0,
                                onCopy = { viewModel.copyToClipboard(context, "HIBP Check: ${result.pwnedCount}") }
                            )
                        }
                    }
                }

                if (result.suggestions.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = SoftDarkBg),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(stringResource(R.string.pass_suggestions), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(Modifier.height(8.dp))
                                result.suggestions.forEach { suggestion ->
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                                        Icon(AppIcons.info(), contentDescription = null, tint = Color(0xFF6C8CFF), modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(suggestion, color = TextGray, fontSize = 12.sp)
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
fun StrengthDetailItem(label: String, value: String, isPassed: Boolean, onCopy: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SoftDarkBg),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, fontSize = 11.sp, color = TextGray)
                Text(value, fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Medium)
            }
            
            IconButton(onClick = onCopy, modifier = Modifier.size(24.dp)) {
                Icon(AppIcons.contentCopy(), null, tint = TextGray.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
            }
            
            Spacer(Modifier.width(8.dp))
            
            Icon(
                painter = if (isPassed) AppIcons.checkCircle() else AppIcons.cancel(),
                contentDescription = null,
                tint = if (isPassed) Color(0xFF10B981) else Color(0xFFEF4444)
            )
        }
    }
}

@Composable
fun PasswordGeneratorPanel(viewModel: PasswordStrengthViewModel, onSaveToManager: () -> Unit) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2235)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF6C8CFF).copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = viewModel.generatedPassword.ifEmpty { stringResource(R.string.pass_gen_generating) },
                    modifier = Modifier.weight(1f),
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { viewModel.generateNewPassword() }) {
                    Icon(AppIcons.refresh(), null, tint = Color(0xFF6C8CFF))
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            viewModel.generatedStrength?.let { strength ->
                val color = when {
                    strength.score >= 80 -> Color(0xFF10B981)
                    strength.score >= 60 -> Color(0xFFF59E0B)
                    strength.score >= 40 -> Color(0xFFF97316)
                    else -> Color(0xFFEF4444)
                }
                Text(
                    text = "${strength.score}/100 ${if (strength.score >= 80) "STRONG" else "FAIR"} • " + stringResource(R.string.pass_crack_time, strength.crackTime),
                    color = color,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(Modifier.height(16.dp))
            
            Text(stringResource(R.string.pass_length) + ": ${viewModel.genLength.toInt()}", color = TextGray, fontSize = 12.sp)
            Slider(
                value = viewModel.genLength,
                onValueChange = { 
                    viewModel.genLength = it
                    viewModel.generateNewPassword()
                },
                valueRange = 8f..64f,
                steps = 56,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF6C8CFF),
                    activeTrackColor = Color(0xFF6C8CFF)
                )
            )
            
            Spacer(Modifier.height(8.dp))
            
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                GeneratorSwitch(stringResource(R.string.pass_gen_upper), viewModel.genUpper) { viewModel.genUpper = it; viewModel.generateNewPassword() }
                GeneratorSwitch(stringResource(R.string.pass_gen_lower), viewModel.genLower) { viewModel.genLower = it; viewModel.generateNewPassword() }
                GeneratorSwitch(stringResource(R.string.pass_gen_numbers), viewModel.genNumbers) { viewModel.genNumbers = it; viewModel.generateNewPassword() }
                GeneratorSwitch(stringResource(R.string.pass_gen_symbols), viewModel.genSymbols) { viewModel.genSymbols = it; viewModel.generateNewPassword() }
                GeneratorSwitch(stringResource(R.string.pass_gen_ambiguous), viewModel.genExcludeAmbiguous) { viewModel.genExcludeAmbiguous = it; viewModel.generateNewPassword() }
            }
            
            Spacer(Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(12, 16, 20, 32).forEach { len ->
                    FilterChip(
                        selected = viewModel.genLength.toInt() == len,
                        onClick = { viewModel.genLength = len.toFloat(); viewModel.generateNewPassword() },
                        label = { Text(len.toString(), fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF6C8CFF).copy(alpha = 0.2f),
                            selectedLabelColor = Color(0xFF6C8CFF)
                        )
                    )
                }
                FilterChip(
                    selected = false,
                    onClick = { viewModel.generateMemorable() },
                    label = { Text(stringResource(R.string.pass_gen_memorable), fontSize = 10.sp) }
                )
            }
            
            Spacer(Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { viewModel.copyAndUse(context) },
                    modifier = Modifier.weight(1.2f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C8CFF)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(stringResource(R.string.pass_gen_copy_use), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                }
                
                Button(
                    onClick = { onSaveToManager() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C8CFF)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("💾 " + stringResource(R.string.pass_gen_save), fontWeight = FontWeight.Bold, fontSize = 10.sp)
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
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF6C8CFF),
                uncheckedThumbColor = TextGray,
                uncheckedTrackColor = Color.Transparent
            ),
            modifier = Modifier.scale(0.7f)
        )
    }
}
