package com.herwin.netez.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.herwin.netez.R
import com.herwin.netez.ui.AppIcons
import com.herwin.netez.ui.components.ToolItem

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
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // DISCOVERY SECTION
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeader("Discovery")
                ToolItem(
                    title = stringResource(R.string.scan_bluetooth),
                    subtitle = "Discover nearby BLE and classic devices",
                    painter = AppIcons.bluetooth(),
                    onClick = onNavigateToBluetooth
                )
                ToolItem(
                    title = stringResource(R.string.scan_lan),
                    subtitle = "Scan and identify all devices in local network",
                    painter = AppIcons.lan(),
                    onClick = onNavigateToLan
                )
                ToolItem(
                    title = stringResource(R.string.scan_mdns),
                    subtitle = "Lookup local services using multicast DNS",
                    painter = AppIcons.search(),
                    onClick = onNavigateToMdns
                )
                ToolItem(
                    title = stringResource(R.string.scan_port),
                    subtitle = "Find open ports and services on any IP",
                    painter = AppIcons.build(),
                    onClick = onNavigateToPort
                )
            }
        }

        // WIRELESS SECTION
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeader("Wireless")
                ToolItem(
                    title = stringResource(R.string.scan_channel_graph),
                    subtitle = "Visualize channel interference in real-time",
                    painter = AppIcons.channelGraph(),
                    onClick = onNavigateToChannel
                )
                ToolItem(
                    title = stringResource(R.string.scan_wifi),
                    subtitle = "Analyze signal quality and network details",
                    painter = AppIcons.wifiAnalyzer(),
                    onClick = onNavigateToWifi
                )
            }
        }

        // ADVANCED SECTION
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeader("Advanced")
                ToolItem(
                    title = stringResource(R.string.scan_snmp),
                    subtitle = "Query SNMP devices for management info",
                    painter = AppIcons.memory(),
                    onClick = onNavigateToSnmp
                )
                ToolItem(
                    title = stringResource(R.string.scan_upnp),
                    subtitle = "Scan for UPnP enabled devices and mappings",
                    painter = AppIcons.router(),
                    onClick = onNavigateToUpnp
                )
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
    )
}
