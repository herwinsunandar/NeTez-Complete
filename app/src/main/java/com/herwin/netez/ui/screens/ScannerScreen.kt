package com.herwin.netez.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.herwin.netez.R
import com.herwin.netez.ui.AppIcons
import com.herwin.netez.ui.components.ToolItem
import com.herwin.netez.ui.theme.PremiumNavy

@Composable
fun ScannerScreen(
    onNavigateToLan: () -> Unit,
    onNavigateToPort: () -> Unit,
    onNavigateToWifi: () -> Unit,
    onNavigateToBluetooth: () -> Unit,
    onNavigateToMdns: () -> Unit,
    onNavigateToChannel: () -> Unit,
    onNavigateToUpnp: () -> Unit = {},
    onNavigateToSnmp: () -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(PremiumNavy)
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // DISCOVERY SECTION
        item {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SectionHeader("DISCOVERY")
                ToolItem(
                    title = stringResource(R.string.scan_lan),
                    subtitle = "Scan and identify all devices in local network",
                    painter = AppIcons.lan(),
                    onClick = onNavigateToLan
                )
                ToolItem(
                    title = stringResource(R.string.scan_port),
                    subtitle = "Find open ports and services on any IP",
                    painter = AppIcons.build(),
                    onClick = onNavigateToPort
                )
                ToolItem(
                    title = stringResource(R.string.scan_bluetooth),
                    subtitle = "Discover nearby BLE and classic devices",
                    painter = AppIcons.bluetooth(),
                    onClick = onNavigateToBluetooth
                )
                ToolItem(
                    title = stringResource(R.string.scan_mdns),
                    subtitle = "Lookup local services using multicast DNS",
                    painter = AppIcons.search(),
                    onClick = onNavigateToMdns
                )
            }
        }

        // WIRELESS SECTION
        item {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SectionHeader("WIRELESS")
                ToolItem(
                    title = stringResource(R.string.scan_wifi),
                    subtitle = "Analyze signal quality and network details",
                    painter = AppIcons.wifiAnalyzer(),
                    onClick = onNavigateToWifi
                )
                ToolItem(
                    title = stringResource(R.string.scan_channel_graph),
                    subtitle = "Visualize channel interference in real-time",
                    painter = AppIcons.channelGraph(),
                    onClick = onNavigateToChannel
                )
            }
        }

        // ADVANCED SECTION
        item {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SectionHeader("ADVANCED")
                ToolItem(
                    title = stringResource(R.string.scan_upnp),
                    subtitle = "Scan for UPnP enabled devices and mappings",
                    painter = AppIcons.router(),
                    onClick = onNavigateToUpnp
                )
                ToolItem(
                    title = stringResource(R.string.scan_snmp),
                    subtitle = "Query SNMP devices for management info",
                    painter = AppIcons.memory(),
                    onClick = onNavigateToSnmp
                )
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = Color(0xFF94A3B8), // TextGray
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
    )
}
