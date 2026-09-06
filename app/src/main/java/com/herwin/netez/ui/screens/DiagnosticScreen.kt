package com.herwin.netez.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.herwin.netez.R
import com.herwin.netez.ui.AppIcons
import com.herwin.netez.ui.components.ToolItem
import com.herwin.netez.ui.theme.PremiumNavy

@Composable
fun DiagnosticScreen(
    onNavigateToPing: () -> Unit,
    onNavigateToTraceroute: () -> Unit,
    onNavigateToDns: () -> Unit,
    onNavigateToSpeed: () -> Unit,
    onNavigateToHttpCheck: () -> Unit,
    onNavigateToSslChecker: () -> Unit,
    onNavigateToBlacklist: () -> Unit,
    onNavigateToStability: () -> Unit,
    onNavigateToIspComparison: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(PremiumNavy),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ToolItem(
                title = stringResource(R.string.diag_ping),
                subtitle = "Check single & multi host latency",
                painter = AppIcons.networkCheck(),
                onClick = onNavigateToPing
            )
        }
        item {
            ToolItem(
                title = stringResource(R.string.diag_traceroute),
                subtitle = "Trace network hops to destination",
                painter = AppIcons.route(),
                onClick = onNavigateToTraceroute
            )
        }
        item {
            ToolItem(
                title = stringResource(R.string.diag_speedtest),
                subtitle = "Measure download & upload speed",
                painter = AppIcons.speed(),
                onClick = onNavigateToSpeed
            )
        }
        item {
            ToolItem(
                title = stringResource(R.string.diag_stability),
                subtitle = stringResource(R.string.diag_stability_desc),
                painter = AppIcons.networkCheck(),
                onClick = onNavigateToStability
            )
        }
        item {
            ToolItem(
                title = stringResource(R.string.diag_dns),
                subtitle = "Resolve domain names to IP",
                painter = AppIcons.dns(),
                onClick = onNavigateToDns
            )
        }
        item {
            ToolItem(
                title = stringResource(R.string.diag_http_check),
                subtitle = "Check website availability & latency",
                painter = AppIcons.language(),
                onClick = onNavigateToHttpCheck
            )
        }
        item {
            ToolItem(
                title = stringResource(R.string.diag_isp_comparison),
                subtitle = stringResource(R.string.diag_isp_comparison_desc),
                painter = AppIcons.compare(),
                onClick = onNavigateToIspComparison
            )
        }
    }
}
