package com.herwin.netez.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.herwin.netez.ui.components.ResultFooter
import com.herwin.netez.ui.components.ResultRow
import com.herwin.netez.ui.theme.PremiumNavy
import com.herwin.netez.ui.theme.SoftDarkBg
import com.herwin.netez.ui.viewmodels.HttpLabViewModel
import com.herwin.netez.ui.viewmodels.HttpResponseData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HttpLabScreen(
    onBack: () -> Unit,
    viewModel: HttpLabViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Params", "Headers", "Body", "Auth")

    Scaffold(
        containerColor = PremiumNavy
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Request Section
            Card(
                colors = CardDefaults.cardColors(containerColor = SoftDarkBg),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        var methodExpanded by remember { mutableStateOf(false) }
                        Box {
                            FilterChip(
                                selected = true,
                                onClick = { methodExpanded = true },
                                label = { Text(uiState.method) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF4A90E2),
                                    selectedLabelColor = Color.White
                                )
                            )
                            DropdownMenu(expanded = methodExpanded, onDismissRequest = { methodExpanded = false }) {
                                listOf("GET", "POST", "PUT", "DELETE", "PATCH").forEach { m ->
                                    DropdownMenuItem(text = { Text(m) }, onClick = {
                                        viewModel.updateMethod(m)
                                        methodExpanded = false
                                    })
                                }
                            }
                        }

                        OutlinedTextField(
                            value = uiState.url,
                            onValueChange = { viewModel.updateUrl(it) },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("https://api.example.com", color = Color.Gray) },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF4A90E2),
                                unfocusedBorderColor = Color.DarkGray
                            )
                        )

                        Button(
                            onClick = { viewModel.sendRequest() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("SEND")
                        }
                    }
                }
            }

            // Tabs Section
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = Color(0xFF4A90E2),
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, color = if (selectedTab == index) Color(0xFF4A90E2) else Color.Gray) }
                    )
                }
            }

            Box(modifier = Modifier.height(200.dp)) {
                when (selectedTab) {
                    0 -> ParamsTab(uiState.params, viewModel)
                    1 -> HeadersTab(uiState.headers, viewModel)
                    2 -> BodyTab(uiState.body, uiState.contentType, viewModel)
                    3 -> AuthTab(uiState.authType, uiState.authValue, viewModel)
                }
            }

            // Response Section
            if (uiState.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Color(0xFF4A90E2))
            }

            uiState.response?.let { resp ->
                ResponseSection(resp)
            }

            uiState.error?.let { err ->
                Text(err, color = Color.Red, modifier = Modifier.padding(8.dp))
            }
        }
    }
}

@Composable
fun ParamsTab(params: List<Pair<String, String>>, viewModel: HttpLabViewModel) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        params.forEachIndexed { index, pair ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(value = pair.first, onValueChange = { viewModel.updateParam(index, it, pair.second) }, modifier = Modifier.weight(1f), placeholder = { Text("Key") })
                TextField(value = pair.second, onValueChange = { viewModel.updateParam(index, pair.first, it) }, modifier = Modifier.weight(1f), placeholder = { Text("Value") })
                IconButton(onClick = { viewModel.removeParam(index) }) { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Gray) }
            }
        }
        TextButton(onClick = { viewModel.addParam() }) {
            Icon(Icons.Default.Add, contentDescription = null)
            Text("Add Parameter")
        }
    }
}

@Composable
fun HeadersTab(headers: List<Pair<String, String>>, viewModel: HttpLabViewModel) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        headers.forEachIndexed { index, pair ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(value = pair.first, onValueChange = { viewModel.updateHeader(index, it, pair.second) }, modifier = Modifier.weight(1f), placeholder = { Text("Header") })
                TextField(value = pair.second, onValueChange = { viewModel.updateHeader(index, pair.first, it) }, modifier = Modifier.weight(1f), placeholder = { Text("Value") })
                IconButton(onClick = { viewModel.removeHeader(index) }) { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Gray) }
            }
        }
        TextButton(onClick = { viewModel.addHeader() }) {
            Icon(Icons.Default.Add, contentDescription = null)
            Text("Add Header")
        }
    }
}

@Composable
fun BodyTab(body: String, contentType: String, viewModel: HttpLabViewModel) {
    Column {
        OutlinedTextField(
            value = body,
            onValueChange = { viewModel.updateBody(it) },
            modifier = Modifier.fillMaxWidth().height(150.dp),
            placeholder = { Text("Request Body (JSON/Raw)", color = Color.Gray) },
            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, color = Color.White)
        )
    }
}

@Composable
fun AuthTab(type: String, value: String, viewModel: HttpLabViewModel) {
    // Simplified Auth Tab
    Text("Auth Type: $type", color = Color.White)
    // Implement full auth UI as needed
}

@Composable
fun ResponseSection(response: HttpResponseData) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SoftDarkBg),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = if (response.code in 200..299) Color(0xFF10B981) else Color(0xFFEF4444),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        "${response.code} ${response.message}",
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.weight(1f))
                Text("${response.time} ms", color = Color.Gray, fontSize = 12.sp)
                Spacer(Modifier.width(8.dp))
                Text("${response.size} B", color = Color.Gray, fontSize = 12.sp)
            }

            Spacer(Modifier.height(16.dp))
            
            var respTab by remember { mutableIntStateOf(0) }
            TabRow(selectedTabIndex = respTab, containerColor = Color.Transparent) {
                Tab(selected = respTab == 0, onClick = { respTab = 0 }, text = { Text("Body") })
                Tab(selected = respTab == 1, onClick = { respTab = 1 }, text = { Text("Headers") })
            }

            if (respTab == 0) {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        val context = LocalContext.current
                        TextButton(onClick = {
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("JSON Response", response.body))
                            android.widget.Toast.makeText(context, "JSON copied", android.widget.Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("COPY JSON", fontSize = 12.sp)
                        }
                    }
                    SelectionContainer {
                        Text(
                            response.body,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF82B1FF),
                            fontSize = 13.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            } else {
                response.headers.forEach { (k, v) ->
                    ResultRow(label = k, value = v)
                }
            }
            
            ResultFooter(
                fullTextToCopy = "Code: ${response.code}\n\nBody:\n${response.body}",
                onCopyAll = { /* Handle copy all */ }
            )
        }
    }
}
