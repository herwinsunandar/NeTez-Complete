package com.herwin.netez.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.herwin.netez.R
import com.herwin.netez.ui.AppIcons
import com.herwin.netez.ui.components.ResultRow
import com.herwin.netez.ui.theme.TechMonospace
import com.herwin.netez.ui.viewmodels.HttpLabViewModel
import com.herwin.netez.ui.viewmodels.HttpResponseData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HttpLabScreen(
    onBack: () -> Unit = {},
    viewModel: HttpLabViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Params", "Headers", "Body", "Auth")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Request Configuration Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "API Request Builder",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Method + URL + Send Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Method Selector Dropdown
                    var methodExpanded by remember { mutableStateOf(false) }
                    Box {
                        val methodColor = getMethodColor(uiState.method)
                        Surface(
                            onClick = { methodExpanded = true },
                            shape = RoundedCornerShape(12.dp),
                            color = methodColor.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, methodColor.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = uiState.method,
                                    fontFamily = TechMonospace,
                                    fontWeight = FontWeight.Bold,
                                    color = methodColor,
                                    fontSize = 13.sp
                                )
                                Icon(
                                    painter = AppIcons.arrowDropDown(),
                                    contentDescription = "Select Method",
                                    tint = methodColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = methodExpanded,
                            onDismissRequest = { methodExpanded = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            listOf("GET", "POST", "PUT", "DELETE", "PATCH").forEach { m ->
                                val color = getMethodColor(m)
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = m,
                                            fontFamily = TechMonospace,
                                            fontWeight = FontWeight.Bold,
                                            color = color
                                        )
                                    },
                                    onClick = {
                                        viewModel.updateMethod(m)
                                        methodExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // URL Input Field
                    OutlinedTextField(
                        value = uiState.url,
                        onValueChange = { viewModel.updateUrl(it) },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text(
                                text = "https://api.example.com/v1",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                fontSize = 13.sp
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Go
                        ),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    )

                    // Send Button
                    Button(
                        onClick = { viewModel.sendRequest() },
                        enabled = !uiState.isLoading && uiState.url.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = "SEND",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                if (uiState.isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                }
            }
        }

        // Sub-Tabs Navigation (Params, Headers, Body, Auth)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                SecondaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clip(RoundedCornerShape(10.dp))
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            },
                            selectedContentColor = MaterialTheme.colorScheme.primary,
                            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp, max = 220.dp)
                ) {
                    when (selectedTab) {
                        0 -> ParamsTab(uiState.params, viewModel)
                        1 -> HeadersTab(uiState.headers, viewModel)
                        2 -> BodyTab(uiState.body, uiState.contentType, viewModel)
                        3 -> AuthTab(uiState.authType, uiState.authValue, viewModel)
                    }
                }
            }
        }

        // Response or Error Section
        uiState.error?.let { err ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = AppIcons.error(),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        uiState.response?.let { resp ->
            ResponseSection(resp)
        }
    }
}

@Composable
fun ParamsTab(params: List<Pair<String, String>>, viewModel: HttpLabViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        params.forEachIndexed { index, pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = pair.first,
                    onValueChange = { viewModel.updateParam(index, it, pair.second) },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Parameter Key", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                OutlinedTextField(
                    value = pair.second,
                    onValueChange = { viewModel.updateParam(index, pair.first, it) },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Value", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                IconButton(
                    onClick = { viewModel.removeParam(index) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        painter = AppIcons.close(),
                        contentDescription = "Remove Param",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        TextButton(
            onClick = { viewModel.addParam() },
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(
                painter = AppIcons.add(),
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text("Add URL Parameter", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun HeadersTab(headers: List<Pair<String, String>>, viewModel: HttpLabViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        headers.forEachIndexed { index, pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = pair.first,
                    onValueChange = { viewModel.updateHeader(index, it, pair.second) },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Header Name", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                OutlinedTextField(
                    value = pair.second,
                    onValueChange = { viewModel.updateHeader(index, pair.first, it) },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Header Value", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                IconButton(
                    onClick = { viewModel.removeHeader(index) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        painter = AppIcons.close(),
                        contentDescription = "Remove Header",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        TextButton(
            onClick = { viewModel.addHeader() },
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(
                painter = AppIcons.add(),
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text("Add Custom Header", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun BodyTab(body: String, contentType: String, viewModel: HttpLabViewModel) {
    val contentTypes = listOf("application/json", "application/x-www-form-urlencoded", "text/plain", "application/xml")

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Content-Type Selector Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            contentTypes.forEach { type ->
                FilterChip(
                    selected = contentType == type,
                    onClick = { viewModel.updateContentType(type) },
                    label = { Text(text = type, fontSize = 11.sp, fontFamily = TechMonospace) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }

        OutlinedTextField(
            value = body,
            onValueChange = { viewModel.updateBody(it) },
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp),
            placeholder = {
                Text(
                    text = "{\n  \"key\": \"value\"\n}",
                    fontFamily = TechMonospace,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            },
            textStyle = TextStyle(
                fontFamily = TechMonospace,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp
            ),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
fun AuthTab(
    type: String,
    value: String,
    viewModel: HttpLabViewModel
) {
    val authTypes = listOf("None", "Bearer", "Basic", "API Key")
    var basicUser by remember { mutableStateOf("") }
    var basicPass by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Auth Type Selector Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            authTypes.forEach { auth ->
                FilterChip(
                    selected = type == auth,
                    onClick = { viewModel.updateAuthType(auth) },
                    label = { Text(text = auth, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }

        when (type) {
            "None" -> {
                Text(
                    text = "No authentication credentials will be attached to this request.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            "Bearer" -> {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Bearer Token",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = value,
                        onValueChange = { viewModel.updateAuthValue(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. eyJhbGciOi...", fontSize = 12.sp, fontFamily = TechMonospace) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = "Generates: Authorization: Bearer <token>",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = TechMonospace,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 10.sp
                    )
                }
            }

            "Basic" -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Basic Authentication Credentials",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = basicUser,
                            onValueChange = {
                                basicUser = it
                                viewModel.updateAuthValue("$basicUser:$basicPass")
                            },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Username", fontSize = 12.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = basicPass,
                            onValueChange = {
                                basicPass = it
                                viewModel.updateAuthValue("$basicUser:$basicPass")
                            },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Password", fontSize = 12.sp) },
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        painter = if (passwordVisible) AppIcons.visibilityOff() else AppIcons.visibility(),
                                        contentDescription = "Toggle Password Visibility",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                    Text(
                        text = "Generates: Authorization: Basic Base64(username:password)",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = TechMonospace,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 10.sp
                    )
                }
            }

            "API Key" -> {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "API Key Header (Key:Value or Key Name)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = value,
                        onValueChange = { viewModel.updateAuthValue(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. X-API-Key:my_secret_token or my_secret_key", fontSize = 12.sp, fontFamily = TechMonospace) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = "Generates custom HTTP header specified by Key Name: Value",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = TechMonospace,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ResponseSection(response: HttpResponseData) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Response Code Badge + Timing Row
            Row(verticalAlignment = Alignment.CenterVertically) {
                val badgeColor = when (response.code) {
                    in 200..299 -> MaterialTheme.colorScheme.secondary
                    in 300..399 -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.error
                }
                Surface(
                    color = badgeColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "${response.code} ${response.message}",
                        color = badgeColor,
                        fontFamily = TechMonospace,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = "${response.time} ms",
                    fontFamily = TechMonospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "${response.size} B",
                    fontFamily = TechMonospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }

            Spacer(Modifier.height(12.dp))

            var respTab by remember { mutableIntStateOf(0) }
            SecondaryTabRow(
                selectedTabIndex = respTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.clip(RoundedCornerShape(8.dp))
            ) {
                Tab(
                    selected = respTab == 0,
                    onClick = { respTab = 0 },
                    text = { Text("Response Body", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
                Tab(
                    selected = respTab == 1,
                    onClick = { respTab = 1 },
                    text = { Text("Response Headers", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
            }

            Spacer(Modifier.height(10.dp))

            if (respTab == 0) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        val context = LocalContext.current
                        TextButton(onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("API Response", response.body))
                            Toast.makeText(context, "Response copied to clipboard", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(
                                painter = AppIcons.contentCopy(),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("COPY RESPONSE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    SelectionContainer {
                        Text(
                            text = response.body.ifBlank { "(Empty Response Body)" },
                            fontFamily = TechMonospace,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp)
                                .verticalScroll(rememberScrollState())
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    response.headers.forEach { (k, v) ->
                        ResultRow(label = k, value = v, isTechnicalData = true)
                    }
                }
            }
        }
    }
}

private fun getMethodColor(method: String): Color {
    return when (method.uppercase()) {
        "GET" -> Color(0xFF10B981)
        "POST" -> Color(0xFFF59E0B)
        "PUT" -> Color(0xFF3B82F6)
        "DELETE" -> Color(0xFFEF4444)
        "PATCH" -> Color(0xFF8B5CF6)
        else -> Color(0xFF6B7280)
    }
}
