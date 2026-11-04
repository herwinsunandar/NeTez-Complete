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
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. DNS Lookup
        item {
            ToolItem(
                title = stringResource(R.string.diag_dns),
                subtitle = "Resolve domain names to IP addresses",
                painter = AppIcons.dns(),
                onClick = onNavigateToDns
            )
        }
        // 2. HTTP Check
        item {
            ToolItem(
                title = stringResource(R.string.diag_http_check),
                subtitle = "Check website availability & latency",
                painter = AppIcons.language(),
                onClick = onNavigateToHttpCheck
            )
        }
        // 3. ISP Comparison & Rating
        item {
            ToolItem(
                title = stringResource(R.string.diag_isp_comparison),
                subtitle = stringResource(R.string.diag_isp_comparison_desc),
                painter = AppIcons.compare(),
                onClick = onNavigateToIspComparison
            )
        }
        // 4. Ping Test
        item {
            ToolItem(
                title = stringResource(R.string.diag_ping),
                subtitle = "Check single & multi host latency",
                painter = AppIcons.networkCheck(),
                onClick = onNavigateToPing
            )
        }
        // 5. Speed Test
        item {
            ToolItem(
                title = stringResource(R.string.diag_speedtest),
                subtitle = "Measure download & upload bandwidth",
                painter = AppIcons.speed(),
                onClick = onNavigateToSpeed
            )
        }
        // 6. Stability Test
        item {
            ToolItem(
                title = stringResource(R.string.diag_stability),
                subtitle = stringResource(R.string.diag_stability_desc),
                painter = AppIcons.networkCheck(),
                onClick = onNavigateToStability
            )
        }
        // 7. Traceroute
        item {
            ToolItem(
                title = stringResource(R.string.diag_traceroute),
                subtitle = "Trace network hops to destination",
                painter = AppIcons.route(),
                onClick = onNavigateToTraceroute
            )
        }
    }
}
