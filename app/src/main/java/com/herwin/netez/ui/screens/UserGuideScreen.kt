package com.herwin.netez.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.herwin.netez.data.UserGuideItem
import com.herwin.netez.data.UserGuideProvider
import com.herwin.netez.ui.AppIcons
import com.herwin.netez.ui.theme.PremiumNavy
import com.herwin.netez.ui.theme.SoftDarkBg
import com.herwin.netez.ui.theme.TextGray
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserGuideScreen(
    onBack: () -> Unit,
    onNavigateToRoute: (String) -> Unit
) {
    val allItems = remember { UserGuideProvider.getGuideItems() }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var isIndonesian by remember { mutableStateOf(false) }
    var expandedItemId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val lang = Locale.getDefault().language
        if (lang == "in" || lang == "id") {
            isIndonesian = true
        }
    }

    val filteredItems = remember(searchQuery, selectedCategory, isIndonesian) {
        allItems.filter { item ->
            val matchesCategory = (selectedCategory == "All" || item.category.equals(selectedCategory, ignoreCase = true))
            val query = searchQuery.trim().lowercase()
            val matchesQuery = query.isEmpty() ||
                    item.titleEn.lowercase().contains(query) ||
                    item.titleId.lowercase().contains(query) ||
                    item.functionEn.lowercase().contains(query) ||
                    item.functionId.lowercase().contains(query) ||
                    item.category.lowercase().contains(query)
            matchesCategory && matchesQuery
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isIndonesian) "Petunjuk Penggunaan" else "User Guide & Manual",
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(AppIcons.arrowBack(), contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    // Language Toggle Chip
                    Surface(
                        onClick = { isIndonesian = !isIndonesian },
                        color = Color(0xFF6C8CFF).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF6C8CFF).copy(alpha = 0.4f)),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Text(
                            text = if (isIndonesian) "🇮🇩 ID" else "🇬🇧 EN",
                            color = Color(0xFF6C8CFF),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SoftDarkBg)
            )
        },
        containerColor = PremiumNavy
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Search Input
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            if (isIndonesian) "Cari fitur (misal: Ping, SSL, Hash)..." else "Search features (e.g., Ping, SSL, Hash)...",
                            color = TextGray.copy(alpha = 0.5f),
                            fontSize = 13.sp
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF6C8CFF),
                        unfocusedBorderColor = TextGray.copy(alpha = 0.3f),
                        focusedContainerColor = SoftDarkBg,
                        unfocusedContainerColor = SoftDarkBg
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    leadingIcon = {
                        Icon(AppIcons.search(), contentDescription = null, tint = TextGray)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(AppIcons.close(), contentDescription = null, tint = TextGray)
                            }
                        }
                    }
                )
            }

            // Category Filter Chips
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val categories = listOf("All", "Dashboard", "Diagnostic", "Scanner", "Utilities", "Security", "Tools")
                    categories.forEach { cat ->
                        GuideFilterChipItem(
                            label = cat,
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat }
                        )
                    }
                }
            }

            // Items List
            if (filteredItems.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isIndonesian) "Tidak ada fitur yang cocok dengan pencarian" else "No features matched your search",
                            color = TextGray,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                items(filteredItems, key = { it.id }) { item ->
                    UserGuideAccordionCard(
                        item = item,
                        isIndonesian = isIndonesian,
                        isExpanded = expandedItemId == item.id,
                        onToggleExpand = {
                            expandedItemId = if (expandedItemId == item.id) null else item.id
                        },
                        onTryFeature = { route ->
                            if (!route.isNullOrEmpty()) {
                                onNavigateToRoute(route)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun GuideFilterChipItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (selected) Color(0xFF6C8CFF).copy(alpha = 0.2f) else SoftDarkBg
    val borderColor = if (selected) Color(0xFF6C8CFF) else Color.Transparent
    val textColor = if (selected) Color(0xFF6C8CFF) else TextGray

    Surface(
        onClick = onClick,
        color = containerColor,
        shape = RoundedCornerShape(20.dp),
        border = if (selected) BorderStroke(1.dp, borderColor) else null
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun UserGuideAccordionCard(
    item: UserGuideItem,
    isIndonesian: Boolean,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onTryFeature: (String?) -> Unit
) {
    val title = if (isIndonesian) item.titleId else item.titleEn
    val functionText = if (isIndonesian) item.functionId else item.functionEn
    val steps = if (isIndonesian) item.stepsId else item.stepsEn

    val iconPainter = when (item.iconName) {
        "home" -> AppIcons.home()
        "networkCheck" -> AppIcons.networkCheck()
        "route" -> AppIcons.route()
        "dns" -> AppIcons.dns()
        "speed" -> AppIcons.speed()
        "language" -> AppIcons.language()
        "compare" -> AppIcons.compare()
        "lock" -> AppIcons.lock()
        "security" -> AppIcons.security()
        "lan" -> AppIcons.lan()
        "wifiAnalyzer" -> AppIcons.wifiAnalyzer()
        "bluetooth" -> AppIcons.bluetooth()
        "search" -> AppIcons.search()
        "channelGraph" -> AppIcons.channelGraph()
        "router" -> AppIcons.router()
        "memory" -> AppIcons.memory()
        "calculate" -> AppIcons.calculate()
        "stickyNote" -> AppIcons.stickyNote()
        "info" -> AppIcons.info()
        "httpHeader" -> AppIcons.httpHeader()
        "rocket" -> AppIcons.rocket()
        "power" -> AppIcons.power()
        "monitor" -> AppIcons.monitor()
        "shieldCheck" -> AppIcons.shieldCheck()
        "wifiShield" -> AppIcons.wifiShield()
        "shield" -> AppIcons.shield()
        "key" -> AppIcons.key()
        "timer" -> AppIcons.timer()
        "public" -> AppIcons.public()
        "terminal" -> AppIcons.terminal()
        "link" -> AppIcons.link()
        "verifiedUser" -> AppIcons.verifiedUser()
        "checkCircle" -> AppIcons.checkCircle()
        "schedule" -> AppIcons.schedule()
        else -> AppIcons.build()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleExpand() },
        colors = CardDefaults.cardColors(containerColor = SoftDarkBg),
        shape = RoundedCornerShape(16.dp),
        border = if (isExpanded) BorderStroke(1.dp, Color(0xFF6C8CFF).copy(alpha = 0.4f)) else null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF6C8CFF).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = iconPainter,
                        contentDescription = null,
                        tint = Color(0xFF6C8CFF),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Surface(
                        color = Color.White.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = item.category.uppercase(),
                            color = Color(0xFF6C8CFF),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Icon(
                    painter = if (isExpanded) AppIcons.keyboardArrowUp() else AppIcons.keyboardArrowDown(),
                    contentDescription = null,
                    tint = TextGray,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Expanded Details
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

                    // Function Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (isIndonesian) "📌 Fungsi Utama" else "📌 Function",
                                color = Color(0xFF6C8CFF),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = functionText,
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }

                    // How to Use Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (isIndonesian) "🚀 Cara Penggunaan" else "🚀 How to Use",
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Spacer(Modifier.height(6.dp))
                            steps.forEachIndexed { index, step ->
                                Row(
                                    modifier = Modifier.padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text("${index + 1}. ", color = Color(0xFF4CAF50), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text(step, color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp, lineHeight = 17.sp)
                                }
                            }
                        }
                    }

                    // Try Feature Button
                    if (!item.route.isNullOrEmpty()) {
                        Button(
                            onClick = { onTryFeature(item.route) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C8CFF)),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(AppIcons.playArrow(), null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (isIndonesian) "COBA FITUR INI" else "TRY THIS FEATURE",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
