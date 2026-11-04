package com.herwin.netez.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.herwin.netez.R
import com.herwin.netez.data.IspRating
import com.herwin.netez.data.IspReview
import com.herwin.netez.ui.AppIcons
import com.herwin.netez.ui.theme.PremiumNavy
import com.herwin.netez.ui.theme.SoftDarkBg
import com.herwin.netez.ui.theme.TextGray
import com.herwin.netez.ui.viewmodels.IspComparisonState
import com.herwin.netez.ui.viewmodels.IspViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IspComparisonScreen(
    viewModel: IspViewModel = viewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedIspForDetail by remember { mutableStateOf<IspRating?>(null) }
    
    val tabs = listOf(stringResource(R.string.isp_tab_comparison), stringResource(R.string.isp_tab_reviews))

    if (selectedIspForDetail != null) {
        IspDetailDialog(
            isp = selectedIspForDetail!!,
            onDismiss = { selectedIspForDetail = null }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
            tonalElevation = 4.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(AppIcons.locationOn(), null, tint = Color(0xFF6C8CFF), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.isp_header_title, uiState.currentCity),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                
                if (uiState.bestValueIsp != null) {
                    Spacer(Modifier.height(16.dp))
                    Surface(
                        color = Color(0xFF22C55E).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF22C55E).copy(alpha = 0.2f))
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(AppIcons.star(), null, tint = Color(0xFFFBBF24), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.isp_best_value, uiState.bestValueIsp?.ispName ?: ""),
                                color = Color(0xFF22C55E),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = PremiumNavy,
            contentColor = Color(0xFF6C8CFF),
            divider = {}
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontWeight = FontWeight.Bold) }
                )
            }
        }

        if (selectedTab == 0) {
            ComparisonTab(uiState, viewModel, onShowDetail = { selectedIspForDetail = it })
        } else {
            ReviewsTab(uiState, viewModel)
        }
    }
}

@Composable
fun ComparisonTab(uiState: IspComparisonState, viewModel: IspViewModel, onShowDetail: (IspRating) -> Unit) {
    val uriHandler = LocalUriHandler.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(uiState.isps) { isp ->
            IspCard(isp, isMyIsp = uiState.currentIspName.contains(isp.ispName, true), onDetailClick = { onShowDetail(isp) })
        }

        item {
            Spacer(Modifier.height(12.dp))
            Surface(
                modifier = Modifier.fillMaxWidth().clickable { 
                    uriHandler.openUri("https://www.speedtest.net/global-index/indonesia")
                },
                color = Color.White.copy(alpha = 0.03f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(AppIcons.info(), null, tint = Color(0xFF6C8CFF), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    
                    val annotatedString = buildAnnotatedString {
                        append(stringResource(R.string.isp_source_data))
                        withStyle(style = SpanStyle(color = Color(0xFF6C8CFF), textDecoration = TextDecoration.Underline, fontWeight = FontWeight.Bold)) {
                            append("Speedtest.net Benchmark")
                        }
                    }
                    
                    Text(
                        text = annotatedString,
                        fontSize = 11.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun IspCard(isp: IspRating, isMyIsp: Boolean, onDetailClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SoftDarkBg,
        shape = RoundedCornerShape(16.dp),
        border = if (isMyIsp) BorderStroke(2.dp, Color(0xFF6C8CFF)) else null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.05f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(AppIcons.wifi(), null, tint = Color.White)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(isp.ispName, color = Color.White, fontWeight = FontWeight.Bold)
                    if (isMyIsp) {
                        Surface(color = Color(0xFF6C8CFF).copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) {
                            Text("MY ISP", color = Color(0xFF6C8CFF), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                    }
                }
                
                RatingChip(isp.rating)
            }
            
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
            Spacer(Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatItem("Avg Speed", "${isp.avgDownload.toInt()} Mbps", AppIcons.download())
                StatItem("Avg Ping", "${isp.avgPing.toInt()} ms", AppIcons.speed())
                StatItem("Price/Mbps", "Rp ${isp.pricePerMbps.toInt()}", AppIcons.payments())
            }
            
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onDetailClick,
                modifier = Modifier.fillMaxWidth().height(42.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6C8CFF),
                    contentColor = Color.White
                )
            ) {
                Text(stringResource(R.string.isp_btn_detail), fontSize = 12.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun RatingChip(rating: Float) {
    val color = when {
        rating >= 4.5f -> Color(0xFF22C55E)
        rating >= 3.5f -> Color(0xFFFBBF24)
        else -> Color(0xFFEF4444)
    }
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(String.format("%.1f", rating), color = color, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(Modifier.width(4.dp))
            Icon(AppIcons.star(), null, tint = color, modifier = Modifier.size(12.dp))
        }
    }
}

@Composable
fun StatItem(label: String, value: String, icon: Painter) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = TextGray, modifier = Modifier.size(14.dp))
        Spacer(Modifier.height(4.dp))
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Text(label, color = TextGray, fontSize = 10.sp)
    }
}

@Composable
fun ReviewsTab(uiState: IspComparisonState, viewModel: IspViewModel) {
    var showReviewForm by remember { mutableStateOf(false) }
    val currentIsp = uiState.isps.find { uiState.currentIspName.contains(it.ispName, true) } ?: uiState.isps.firstOrNull()
    
    Column(modifier = Modifier.fillMaxSize()) {
        if (!showReviewForm) {
            Box(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                Button(
                    onClick = { showReviewForm = true },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = currentIsp != null
                ) {
                    Icon(AppIcons.add(), null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.isp_btn_rate, currentIsp?.ispName ?: uiState.currentIspName))
                }
            }
        } else {
            currentIsp?.let { isp ->
                ReviewForm(
                    ispName = isp.ispName,
                    onSubmit = { rating, dl, ul, ping, price, comment ->
                        viewModel.submitReview(isp.id, rating, dl, ul, ping, price, comment)
                        showReviewForm = false
                    },
                    onCancel = { showReviewForm = false }
                )
            }
        }
        
        if (uiState.selectedIspReviews.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.isp_empty_reviews), color = TextGray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.selectedIspReviews) { review ->
                    ReviewCard(review)
                }
            }
        }
    }

    LaunchedEffect(currentIsp) {
        currentIsp?.let { viewModel.loadReviews(it.id) }
    }
}

@Composable
fun ReviewForm(ispName: String, onSubmit: (Float, Double, Double, Double, Double, String) -> Unit, onCancel: () -> Unit) {
    var rating by remember { mutableFloatStateOf(0f) }
    var download by remember { mutableStateOf("") }
    var upload by remember { mutableStateOf("") }
    var ping by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        color = SoftDarkBg,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.isp_rate_title, ispName), color = Color.White, fontWeight = FontWeight.Bold)
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                repeat(5) { i ->
                    Icon(
                        if (i < rating) AppIcons.star() else AppIcons.starBorder(),
                        null,
                        tint = if (i < rating) Color(0xFFFBBF24) else TextGray,
                        modifier = Modifier.size(32.dp).clickable { rating = i + 1f }
                    )
                }
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReviewField(Modifier.weight(1f), download, "DL (Mbps)") { download = it }
                ReviewField(Modifier.weight(1f), upload, "UL (Mbps)") { upload = it }
                ReviewField(Modifier.weight(1f), ping, "Ping (ms)") { ping = it }
            }
            
            ReviewField(Modifier.fillMaxWidth(), price, "Harga / Bulan (Rp)") { price = it }
            
            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it },
                modifier = Modifier.fillMaxWidth().height(100.dp),
                placeholder = { Text(stringResource(R.string.isp_field_comment), color = TextGray) },
                colors = OutlinedTextFieldDefaults.colors(unfocusedTextColor = Color.White, focusedTextColor = Color.White)
            )
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.btn_cancel)) }
                Button(
                    onClick = { 
                        onSubmit(
                            rating, 
                            download.toDoubleOrNull() ?: 0.0,
                            upload.toDoubleOrNull() ?: 0.0,
                            ping.toDoubleOrNull() ?: 0.0,
                            price.toDoubleOrNull() ?: 0.0,
                            comment
                        )
                    }, 
                    modifier = Modifier.weight(1f),
                    enabled = rating > 0
                ) { 
                    Text("Submit") 
                }
            }
        }
    }
}

@Composable
fun ReviewField(modifier: Modifier, value: String, label: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text(label, fontSize = 10.sp) },
        colors = OutlinedTextFieldDefaults.colors(unfocusedTextColor = Color.White, focusedTextColor = Color.White),
        singleLine = true
    )
}

@Composable
fun ReviewCard(review: IspReview) {
    val sdf = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White.copy(alpha = 0.03f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(review.userName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(sdf.format(Date(review.timestamp)), color = TextGray, fontSize = 10.sp)
            }
            Spacer(Modifier.height(4.dp))
            RatingChip(review.rating)
            
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SmallStat("DL", "${review.downloadActual.toInt()}M")
                SmallStat("UL", "${review.uploadActual.toInt()}M")
                SmallStat("Ping", "${review.pingActual.toInt()}ms")
            }
            
            if (review.comment.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(review.comment, color = Color.White, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun SmallStat(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("$label: ", color = TextGray, fontSize = 10.sp)
        Text(value, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IspDetailDialog(isp: IspRating, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SoftDarkBg,
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(AppIcons.analytics(), null, tint = Color(0xFF6C8CFF))
                Spacer(Modifier.width(12.dp))
                Text(isp.ispName, color = Color.White, fontWeight = FontWeight.ExtraBold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    DetailStat("Rating", String.format("%.1f", isp.rating), AppIcons.star(), Color(0xFFFBBF24))
                    DetailStat("Speed", "${isp.avgDownload.toInt()}M", AppIcons.speed(), Color(0xFF10B981))
                    DetailStat("Reviews", isp.reviewCount.toString(), AppIcons.groups(), Color(0xFF6C8CFF))
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

                Text(stringResource(R.string.isp_pros), color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    isp.pros.split(",").filter { it.isNotBlank() }.forEach { pro ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(AppIcons.checkCircle(), null, tint = Color(0xFF10B981), modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(pro.trim(), color = Color.White, fontSize = 12.sp)
                        }
                    }
                }

                Text(stringResource(R.string.isp_cons), color = Color(0xFFEF4444), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    isp.cons.split(",").filter { it.isNotBlank() }.forEach { con ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(AppIcons.cancel(), null, tint = Color(0xFFEF4444), modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(con.trim(), color = Color.White, fontSize = 12.sp)
                        }
                    }
                }

                Surface(
                    color = Color.White.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.isp_disclaimer, isp.city),
                        modifier = Modifier.padding(12.dp),
                        color = TextGray,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) {
                Text(stringResource(R.string.btn_close))
            }
        }
    )
}

@Composable
fun DetailStat(label: String, value: String, icon: Painter, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        Text(value, color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
        Text(label, color = TextGray, fontSize = 10.sp)
    }
}
