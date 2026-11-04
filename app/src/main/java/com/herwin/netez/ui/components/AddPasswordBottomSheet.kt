package com.herwin.netez.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.herwin.netez.R
import com.herwin.netez.data.PasswordEntry
import com.herwin.netez.ui.AppIcons
import com.herwin.netez.ui.theme.SoftDarkBg
import com.herwin.netez.ui.theme.TextGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPasswordBottomSheet(
    initialEntry: PasswordEntry? = null,
    prefilledPassword: String = "",
    prefilledStrength: Int = 0,
    onDismiss: () -> Unit,
    onSave: (PasswordEntry) -> Unit
) {
    var appName by remember { mutableStateOf(initialEntry?.appName ?: "") }
    var username by remember { mutableStateOf(initialEntry?.username ?: "") }
    var password by remember { mutableStateOf(initialEntry?.passwordEncrypted ?: prefilledPassword) }
    var notes by remember { mutableStateOf(initialEntry?.notes ?: "") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SoftDarkBg,
        dragHandle = { BottomSheetDefaults.DragHandle(color = TextGray) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (initialEntry != null) "Update Password" else stringResource(R.string.pass_save_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            OutlinedTextField(
                value = appName,
                onValueChange = { appName = it },
                label = { Text(stringResource(R.string.pass_field_app_name), color = TextGray) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF6C8CFF),
                    unfocusedBorderColor = TextGray
                )
            )

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text(stringResource(R.string.pass_field_username), color = TextGray) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF6C8CFF),
                    unfocusedBorderColor = TextGray
                )
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.pass_input_label), color = TextGray) },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                shape = RoundedCornerShape(12.dp),
                trailingIcon = {
                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Icon(if (isPasswordVisible) AppIcons.visibilityOff() else AppIcons.visibility(), null, tint = TextGray)
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF6C8CFF),
                    unfocusedBorderColor = TextGray
                )
            )

            if (prefilledStrength > 0 || (initialEntry?.strengthScore ?: 0) > 0) {
                val score = if (prefilledStrength > 0) prefilledStrength else initialEntry?.strengthScore ?: 0
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("$score/100", color = if (score >= 80) Color(0xFF10B981) else Color(0xFFEF4444), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(if (score >= 80) "STRONG" else "WEAK", color = TextGray, fontSize = 12.sp)
                }
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(stringResource(R.string.pass_field_notes), color = TextGray) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                minLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF6C8CFF),
                    unfocusedBorderColor = TextGray
                )
            )

            Button(
                onClick = {
                    if (appName.isNotEmpty() && username.isNotEmpty() && password.isNotEmpty()) {
                        onSave(
                            (initialEntry ?: PasswordEntry(
                                appName = "",
                                appPackage = null,
                                username = "",
                                passwordEncrypted = "",
                                notes = "",
                                iconUrl = null,
                                strengthScore = prefilledStrength
                            )).copy(
                                appName = appName,
                                username = username,
                                passwordEncrypted = password,
                                notes = notes,
                                strengthScore = if (prefilledStrength > 0) prefilledStrength else (initialEntry?.strengthScore ?: 0),
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C8CFF)),
                shape = RoundedCornerShape(12.dp),
                enabled = appName.isNotEmpty() && username.isNotEmpty() && password.isNotEmpty()
            ) {
                Text(if (initialEntry != null) "UPDATE" else stringResource(R.string.pass_btn_save), fontWeight = FontWeight.Bold)
            }
            
            Spacer(Modifier.height(20.dp))
        }
    }
}
