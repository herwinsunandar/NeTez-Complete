package com.herwin.netez.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.herwin.netez.R
import com.herwin.netez.ui.components.ToolItem

@Composable
fun DiagnosticScreen(
    onNavigateToPing: () -> Unit,
    onNavigateToTraceroute: () -> Unit,
    onNavigateToDns: () -> Unit,
    onNavigateToSpeed: () -> Unit,
    onNavigateToHttpCheck: () -> Unit,
    onNavigateToSslChecker: () -> Unit,
    onNavigateToBlacklist: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(com.herwin.netez.ui.theme.PremiumNavy),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ToolItem(
                title = stringResource(R.string.diag_ping),
                subtitle = "Check connection latency & stability",
                icon = Icons.Default.NetworkCheck,
                onClick = onNavigateToPing
            )
        }
        item {
            ToolItem(
                title = stringResource(R.string.diag_traceroute),
                subtitle = "Trace network hops to destination",
                icon = Icons.Default.Route,
                onClick = onNavigateToTraceroute
            )
        }
        item {
            ToolItem(
                title = stringResource(R.string.diag_speedtest),
                subtitle = "Measure download & upload speed",
                icon = Icons.Default.Speed,
                onClick = onNavigateToSpeed
            )
        }
        item {
            ToolItem(
                title = stringResource(R.string.diag_dns),
                subtitle = "Resolve domain names to IP",
                icon = Icons.Default.Dns,
                onClick = onNavigateToDns
            )
        }
        item {
            ToolItem(
                title = stringResource(R.string.diag_http_check),
                subtitle = "Check website availability & latency",
                icon = Icons.Default.Language,
                onClick = onNavigateToHttpCheck
            )
        }
        item {
            ToolItem(
                title = stringResource(R.string.diag_ssl_checker),
                subtitle = "Inspect SSL/TLS certificates",
                icon = Icons.Default.Lock,
                onClick = onNavigateToSslChecker
            )
        }
        item {
            ToolItem(
                title = stringResource(R.string.diag_blacklist),
                subtitle = stringResource(R.string.blacklist_check_desc),
                icon = Icons.Default.Security,
                onClick = onNavigateToBlacklist
            )
        }
    }
}
