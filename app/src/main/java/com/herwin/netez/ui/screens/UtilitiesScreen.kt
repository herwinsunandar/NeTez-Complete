package com.herwin.netez.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.herwin.netez.R
import com.herwin.netez.ui.components.ToolItem

@Composable
fun UtilitiesScreen(
    onNavigateToIpCalc: () -> Unit,
    onNavigateToQuickNotes: () -> Unit,
    onNavigateToWhois: () -> Unit,
    onNavigateToHttp: () -> Unit,
    onNavigateToHttpLab: () -> Unit,
    onNavigateToWol: () -> Unit,
    onNavigateToEngTools: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(com.herwin.netez.ui.theme.PremiumNavy),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ToolItem(
                title = stringResource(R.string.util_http_lab),
                subtitle = "API Tester & HTTP Request tool",
                icon = Icons.Default.RocketLaunch,
                onClick = onNavigateToHttpLab
            )
        }
        item {
            ToolItem(
                title = stringResource(R.string.util_quick_notes),
                subtitle = "IP Book & Secure network notes",
                icon = Icons.Default.StickyNote2,
                onClick = onNavigateToQuickNotes
            )
        }
        item {
            ToolItem(
                title = stringResource(R.string.util_eng_tools),
                subtitle = "SSH, Telnet, SFTP & TFTP Clients",
                icon = Icons.Default.Terminal,
                onClick = onNavigateToEngTools
            )
        }
        item {
            ToolItem(
                title = stringResource(R.string.util_ip_calc),
                subtitle = "Subnetting calculator",
                icon = Icons.Default.Calculate,
                onClick = onNavigateToIpCalc
            )
        }
        item {
            ToolItem(
                title = stringResource(R.string.util_whois),
                subtitle = "IP Info & Domain lookup",
                icon = Icons.Default.Info,
                onClick = onNavigateToWhois
            )
        }
        item {
            ToolItem(
                title = stringResource(R.string.util_http),
                subtitle = "Check HTTP headers & status",
                icon = Icons.Default.Http,
                onClick = onNavigateToHttp
            )
        }
        item {
            ToolItem(
                title = stringResource(R.string.util_wol),
                subtitle = "Wake up devices on network",
                icon = Icons.Default.Power,
                onClick = onNavigateToWol
            )
        }
    }
}
