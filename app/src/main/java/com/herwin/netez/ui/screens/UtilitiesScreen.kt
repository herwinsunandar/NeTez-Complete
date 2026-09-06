package com.herwin.netez.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.herwin.netez.R
import com.herwin.netez.ui.AppIcons
import com.herwin.netez.ui.components.ToolItem
import com.herwin.netez.ui.theme.PremiumNavy

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
        modifier = Modifier.fillMaxSize().background(PremiumNavy),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ToolItem(
                title = stringResource(R.string.util_monitoring_lab),
                subtitle = "Monitor hosts and services in real-time",
                painter = AppIcons.monitor(),
                onClick = onNavigateToMonitoring
            )
        }
        item {
            ToolItem(
                title = stringResource(R.string.util_http_lab),
                subtitle = "API Tester & HTTP Request tool",
                painter = AppIcons.rocket(),
                onClick = onNavigateToHttpLab
            )
        }
        item {
            ToolItem(
                title = stringResource(R.string.util_quick_notes),
                subtitle = "IP Book & Secure network notes",
                painter = AppIcons.stickyNote(),
                onClick = onNavigateToQuickNotes
            )
        }
        item {
            ToolItem(
                title = stringResource(R.string.util_eng_tools),
                subtitle = "SSH, Telnet, SFTP & TFTP Clients",
                painter = AppIcons.terminal(),
                onClick = onNavigateToEngTools
            )
        }
        item {
            ToolItem(
                title = stringResource(R.string.util_ip_calc),
                subtitle = "Subnetting calculator",
                painter = AppIcons.calculate(),
                onClick = onNavigateToIpCalc
            )
        }
        item {
            ToolItem(
                title = stringResource(R.string.util_whois),
                subtitle = "IP Info & Domain lookup",
                painter = AppIcons.info(),
                onClick = onNavigateToWhois
            )
        }
        item {
            ToolItem(
                title = stringResource(R.string.util_http),
                subtitle = "Check HTTP headers & status",
                painter = AppIcons.httpHeader(),
                onClick = onNavigateToHttp
            )
        }
        item {
            ToolItem(
                title = stringResource(R.string.util_wol),
                subtitle = "Wake up devices on network",
                painter = AppIcons.power(),
                onClick = onNavigateToWol
            )
        }
    }
}
