package com.herwin.netez.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.herwin.netez.R
import com.herwin.netez.ui.AppIcons
import com.herwin.netez.ui.components.ToolItem

@Composable
fun UtilitiesScreen(
    onNavigateToIpCalc: () -> Unit,
    onNavigateToQuickNotes: () -> Unit,
    onNavigateToWhois: () -> Unit,
    onNavigateToHttp: () -> Unit,
    onNavigateToHttpLab: () -> Unit,
    onNavigateToWol: () -> Unit,
    onNavigateToEngTools: () -> Unit,
    onNavigateToMonitoring: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. API Tester / HTTP Lab
        item {
            ToolItem(
                title = stringResource(R.string.util_http_lab),
                subtitle = "API Tester & HTTP Request tool",
                painter = AppIcons.rocket(),
                onClick = onNavigateToHttpLab
            )
        }
        // 2. Engineer Tools
        item {
            ToolItem(
                title = stringResource(R.string.util_eng_tools),
                subtitle = "SSH, Telnet, SFTP & TFTP Clients",
                painter = AppIcons.terminal(),
                onClick = onNavigateToEngTools
            )
        }
        // 3. HTTP Header
        item {
            ToolItem(
                title = stringResource(R.string.util_http),
                subtitle = "Check HTTP headers & status",
                painter = AppIcons.httpHeader(),
                onClick = onNavigateToHttp
            )
        }
        // 4. IP Calculator
        item {
            ToolItem(
                title = stringResource(R.string.util_ip_calc),
                subtitle = "Subnetting calculator",
                painter = AppIcons.calculate(),
                onClick = onNavigateToIpCalc
            )
        }
        // 5. Monitoring Lab
        item {
            ToolItem(
                title = stringResource(R.string.util_monitoring_lab),
                subtitle = "Monitor hosts and services in real-time",
                painter = AppIcons.monitor(),
                onClick = onNavigateToMonitoring
            )
        }
        // 6. Quick Notes / IP Book
        item {
            ToolItem(
                title = stringResource(R.string.util_quick_notes),
                subtitle = "IP Book & Secure network notes",
                painter = AppIcons.stickyNote(),
                onClick = onNavigateToQuickNotes
            )
        }
        // 7. Wake on LAN
        item {
            ToolItem(
                title = stringResource(R.string.util_wol),
                subtitle = "Wake up devices on network",
                painter = AppIcons.power(),
                onClick = onNavigateToWol
            )
        }
        // 8. Whois & IP Info
        item {
            ToolItem(
                title = stringResource(R.string.util_whois),
                subtitle = "IP Info & Domain lookup",
                painter = AppIcons.info(),
                onClick = onNavigateToWhois
            )
        }
    }
}
