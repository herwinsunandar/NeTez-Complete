package com.herwin.netez.ui.screens

import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.herwin.netez.network.HttpCheckResult
import com.herwin.netez.ui.AppIcons
import com.herwin.netez.ui.theme.PremiumNavy
import com.herwin.netez.ui.theme.SoftDarkBg
import com.herwin.netez.ui.theme.TextGray
import com.herwin.netez.ui.viewmodels.HttpCheckViewModel
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HttpCheckScreen(viewModel: HttpCheckViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    // Manage local TextFieldValue for cursor control
    var urlValue by remember { mutableStateOf(TextFieldValue(uiState.url)) }
    
    // Sync with ViewModel url changes (e.g. suggestions)
    LaunchedEffect(uiState.url) {
        if (uiState.url != urlValue.text) {
            urlValue = TextFieldValue(
                text = uiState.url,
                selection = TextRange(uiState.url.length)
            )
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(PremiumNavy)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        // 1. Pro Input Card
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SoftDarkBg,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    OutlinedTextField(
                        value = urlValue,
                        onValueChange = { 
                            urlValue = it
                            viewModel.updateUrl(it.text) 
                        },
                        leadingIcon = { 
                            Icon(
                                painter = AppIcons.lock(), 
                                contentDescription = null, 
                                tint = Color(0xFF22C55E), 
                                modifier = Modifier.size(16.dp)
                            ) 
                        },
                        placeholder = { Text("https://google.com", color = TextGray.copy(alpha = 0.5f), fontSize = 15.sp) },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontSize = 15.sp, color = Color.White),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = { viewModel.runCheck() }),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color(0xFF2A2E45),
                            focusedContainerColor = Color(0xFF2A2E45),
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = Color(0xFF6C8CFF),
                            unfocusedTextColor = Color.White,
                            focusedTextColor = Color.White
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp).height(48.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        var methodExpanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(0.35f)) {
                            Surface(
                                onClick = { methodExpanded = true },
                                modifier = Modifier.fillMaxSize(),
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF2A2E45)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(uiState.method, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Icon(AppIcons.arrowDropDown(), null, tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                            }
                            DropdownMenu(
                                expanded = methodExpanded,
                                onDismissRequest = { methodExpanded = false },
                                modifier = Modifier.background(Color(0xFF2A2E45))
                            ) {
                                listOf("GET", "HEAD").forEach { m ->
                                    DropdownMenuItem(
                                        text = { Text(m, color = Color.White) },
                                        onClick = { viewModel.updateMethod(m); methodExpanded = false }
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = { viewModel.runCheck() },
                            modifier = Modifier.weight(0.65f).fillMaxHeight(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C8CFF)),
                            enabled = !uiState.isLoading,
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(AppIcons.search(), null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "CHECK", 
                                        fontWeight = FontWeight.Bold, 
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Pro Status Card
        uiState.result?.let { res ->
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Result", style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
                    Row {
                        IconButton(onClick = { 
                            viewModel.copyToClipboard()
                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(AppIcons.contentCopy(), contentDescription = "Copy Results", tint = Color(0xFF6C8CFF))
                        }
                        IconButton(onClick = { 
                            viewModel.shareResults(context)
                        }) {
                            Icon(AppIcons.share(), contentDescription = "Share Results", tint = Color(0xFF6C8CFF))
                        }
                    }
                }
            }
            item {
                HttpStatusProCard(res)
            }
        }

        // 3. Response History Chart
        if (uiState.history.size >= 2) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF1E2235).copy(alpha = 0.5f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Response History", style = MaterialTheme.typography.labelSmall, color = TextGray, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(80.dp)) {
                            HistoryLineChart(uiState.history)
                        }
                    }
                }
            }
        }

        // 4. Expandable Sections
        uiState.result?.let { res ->
            item {
                ExpandableHttpSection(
                    title = "Response Headers (${res.headers.size})",
                    isExpanded = uiState.isHeadersExpanded,
                    onToggle = { viewModel.toggleHeaders() }
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        res.headers.forEach { (k, v) ->
                            HeaderItem(k, v)
                        }
                    }
                }
            }
            
            if (res.hasSsl) {
                item {
                    ExpandableHttpSection(
                        title = "SSL Certificate",
                        isExpanded = uiState.isSslExpanded,
                        onToggle = { viewModel.toggleSsl() }
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SslDetailItem("Subject", res.sslSubject ?: "Unknown")
                            SslDetailItem("Issuer", res.sslIssuer ?: "Unknown")
                            SslDetailItem("Expiry", "${res.sslExpiryDays ?: 0} days remaining")
                        }
                    }
                }
            }
        }

        // 5. Quick Suggestions
        item {
            Text("Quick Suggestions", style = MaterialTheme.typography.labelSmall, color = TextGray, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            FlowRowComponent(modifier = Modifier.fillMaxWidth()) {
                uiState.recentHosts.forEach { host ->
                    Surface(
                        onClick = { viewModel.updateUrl(host); viewModel.runCheck() },
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.05f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = "https://www.google.com/s2/favicons?domain=$host&sz=32",
                                contentDescription = null,
                                modifier = Modifier.size(16.dp).clip(CircleShape)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(host, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HttpStatusProCard(res: HttpCheckResult) {
    val isOnline = res.isAvailable
    val bandColor = if (isOnline) Color(0xFF1A2E22) else Color(0xFF2E1A1A)
    val statusColor = if (isOnline) Color(0xFF22C55E) else Color(0xFFEF4444)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF1E2235),
        tonalElevation = 4.dp
    ) {
        Column {
            Box(
                modifier = Modifier.fillMaxWidth().background(bandColor).padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (isOnline) AppIcons.lock() else AppIcons.lockOpen(), null, tint = statusColor, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (isOnline) "ONLINE" else "OFFLINE", color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }

            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = "https://www.google.com/s2/favicons?domain=${res.url}&sz=64",
                        contentDescription = null,
                        modifier = Modifier.size(32.dp).clip(CircleShape)
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(res.url.removePrefix("https://").removePrefix("http://"), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(20.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                Spacer(Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    MetricPro(Modifier.weight(1f), "Status", res.statusCode.toString(), statusColor)
                    MetricPro(Modifier.weight(1f), "Response", "${res.responseTime}ms", Color(0xFF6C8CFF))
                    MetricPro(Modifier.weight(1.5f), "Server IP", res.ipAddress, TextGray)
                }

                Spacer(Modifier.height(20.dp))
                Text("Timings", fontSize = 11.sp, color = TextGray, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TimingChip("DNS", if (res.dnsTime > 0) "${res.dnsTime}ms" else "cached")
                    TimingChip("Connect", if (res.connectTime > 0) "${res.connectTime}ms" else "-")
                    TimingChip("TLS", if (res.tlsTime > 0) "${res.tlsTime}ms" else "-")
                    TimingChip("TTFB", if (res.ttfbTime > 0) "${res.ttfbTime}ms" else "-")
                }
            }
        }
    }
}

@Composable
fun MetricPro(modifier: Modifier = Modifier, label: String, value: String, valueColor: Color) {
    Column(modifier = modifier) {
        Text(label, fontSize = 10.sp, color = TextGray)
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun TimingChip(label: String, value: String) {
    Surface(
        color = Color(0xFF2A2E45),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Text("$label ", fontSize = 10.sp, color = TextGray)
            Text(value, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ExpandableHttpSection(title: String, isExpanded: Boolean, onToggle: () -> Unit, content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SoftDarkBg,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        onClick = onToggle
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Icon(if (isExpanded) AppIcons.expandLess() else AppIcons.expandMore(), null, tint = TextGray)
            }
            AnimatedVisibility(visible = isExpanded) {
                content()
            }
        }
    }
}

@Composable
fun HeaderItem(key: String, value: String) {
    Column {
        Text(key, color = Color(0xFF6C8CFF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text(value, color = Color.White, fontSize = 12.sp)
    }
}

@Composable
fun SslDetailItem(label: String, value: String) {
    Row {
        Text("$label: ", color = TextGray, fontSize = 12.sp)
        Text(value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun HistoryLineChart(history: List<HttpCheckResult>) {
    val context = LocalContext.current
    val data = history.reversed()
    
    AndroidView(
        factory = { ctx ->
            LineChart(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                description.isEnabled = false
                legend.isEnabled = false
                setTouchEnabled(true)
                axisRight.isEnabled = false
                axisLeft.isEnabled = false
                xAxis.isEnabled = false
                setDrawGridBackground(false)
                setDrawBorders(false)
            }
        },
        update = { chart ->
            val entries = data.mapIndexed { i, res -> Entry(i.toFloat(), res.responseTime.toFloat()) }
            val dataSet = LineDataSet(entries, "ResponseTime").apply {
                color = android.graphics.Color.parseColor("#22C55E")
                lineWidth = 2f
                setDrawValues(false)
                setDrawCircles(true)
                mode = LineDataSet.Mode.CUBIC_BEZIER
            }
            chart.data = LineData(dataSet)
            chart.invalidate()
        },
        modifier = Modifier.fillMaxSize()
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRowComponent(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = { content() }
    )
}
