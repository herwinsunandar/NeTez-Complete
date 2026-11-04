package com.herwin.netez.ui.screens

import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
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
import com.herwin.netez.network.HttpCheckResult
import com.herwin.netez.ui.AppIcons
import com.herwin.netez.ui.theme.TechMonospace
import com.herwin.netez.ui.viewmodels.HttpCheckViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HttpCheckScreen(viewModel: HttpCheckViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var urlValue by remember { mutableStateOf(TextFieldValue(uiState.url)) }

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
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        // 1. Pro Input Card
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
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
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        placeholder = {
                            Text(
                                text = "e.g. https://google.com or example.com",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                fontSize = 13.sp
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Go
                        ),
                        keyboardActions = KeyboardActions(onGo = { viewModel.runCheck() }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        var methodExpanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(0.35f)) {
                            Surface(
                                onClick = { methodExpanded = true },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = uiState.method,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontFamily = TechMonospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Icon(
                                        painter = AppIcons.arrowDropDown(),
                                        contentDescription = "Method",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            DropdownMenu(
                                expanded = methodExpanded,
                                onDismissRequest = { methodExpanded = false },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                            ) {
                                listOf("GET", "HEAD").forEach { m ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = m,
                                                fontFamily = TechMonospace,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
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

                        Button(
                            onClick = { viewModel.runCheck() },
                            modifier = Modifier.weight(0.65f).height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            enabled = !uiState.isLoading,
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        painter = AppIcons.search(),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
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

        // 2. Status Card
        uiState.result?.let { res ->
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Result Summary",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row {
                        IconButton(onClick = {
                            viewModel.copyToClipboard()
                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(
                                painter = AppIcons.contentCopy(),
                                contentDescription = "Copy Results",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = {
                            viewModel.shareResults(context)
                        }) {
                            Icon(
                                painter = AppIcons.share(),
                                contentDescription = "Share Results",
                                tint = MaterialTheme.colorScheme.primary
                            )
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
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    tonalElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Response History Graph",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                        ) {
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
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        res.headers.forEach { (k, v) ->
                            HeaderItem(k, v)
                        }
                    }
                }
            }

            if (res.hasSsl) {
                item {
                    ExpandableHttpSection(
                        title = "SSL Certificate Details",
                        isExpanded = uiState.isSslExpanded,
                        onToggle = { viewModel.toggleSsl() }
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
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
            Text(
                text = "Quick Host Shortcuts",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            FlowRowComponent(modifier = Modifier.fillMaxWidth()) {
                uiState.recentHosts.forEach { host ->
                    Surface(
                        onClick = {
                            viewModel.updateUrl(host)
                            viewModel.runCheck()
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = "https://www.google.com/s2/favicons?domain=$host&sz=32",
                                contentDescription = null,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = host,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp,
                                fontFamily = TechMonospace,
                                fontWeight = FontWeight.Medium
                            )
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
    val statusColor = if (isOnline) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        tonalElevation = 2.dp
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(statusColor.copy(alpha = 0.15f))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = if (isOnline) AppIcons.lock() else AppIcons.lockOpen(),
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (isOnline) "ONLINE" else "OFFLINE",
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = "https://www.google.com/s2/favicons?domain=${res.url}&sz=64",
                        contentDescription = null,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = res.url.removePrefix("https://").removePrefix("http://"),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = TechMonospace,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricPro(Modifier.weight(1f), "Status", res.statusCode.toString(), statusColor)
                    MetricPro(Modifier.weight(1f), "Response", "${res.responseTime} ms", MaterialTheme.colorScheme.primary)
                    MetricPro(Modifier.weight(1.5f), "Server IP", res.ipAddress, MaterialTheme.colorScheme.onSurface)
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Timings",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
fun MetricPro(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    valueColor: Color
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontFamily = TechMonospace,
            fontWeight = FontWeight.Bold,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun TimingChip(label: String, value: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            Text(
                text = "$label ",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                fontSize = 10.sp,
                fontFamily = TechMonospace,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ExpandableHttpSection(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        onClick = onToggle
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Icon(
                    painter = if (isExpanded) AppIcons.expandLess() else AppIcons.expandMore(),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
        Text(
            text = key,
            color = MaterialTheme.colorScheme.primary,
            fontFamily = TechMonospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurface,
            fontFamily = TechMonospace,
            fontSize = 12.sp
        )
    }
}

@Composable
fun SslDetailItem(label: String, value: String) {
    Row {
        Text(
            text = "$label: ",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurface,
            fontFamily = TechMonospace,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun HistoryLineChart(history: List<HttpCheckResult>) {
    val data = history.reversed()

    AndroidView(
        factory = { ctx ->
            LineChart(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
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
