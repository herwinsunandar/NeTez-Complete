package com.herwin.netez.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.herwin.netez.utils.NetworkUtils

@Composable
fun CellularDetailScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val details = NetworkUtils.getCellularDetails(context)
    val ipAddress = NetworkUtils.getLocalIpAddress() ?: "Unknown"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            DetailCategory("Network Operator") {
                DetailRow("Operator", details["Operator"] ?: "Unknown", Icons.Default.Business)
                DetailRow("Local IP", ipAddress, Icons.Default.Language)
            }
        }

        item {
            DetailCategory("Connection Type") {
                DetailRow("Network Type", details["Network Type"] ?: "Unknown", Icons.Default.SignalCellularAlt)
                DetailRow("Data State", details["Data State"] ?: "Unknown", Icons.Default.DataUsage)
            }
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Cellular details are managed by your provider. Exact signal dBm might require special permissions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
