package com.herwin.netez.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.herwin.netez.data.entities.SslDomain
import com.herwin.netez.ui.AppIcons
import com.herwin.netez.ui.theme.SoftDarkBg
import com.herwin.netez.ui.theme.TextGray
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SslDomainDetailBottomSheet(
    domain: SslDomain,
    onDismiss: () -> Unit,
    onRecheck: () -> Unit,
    onDelete: () -> Unit
) {
    val sdf = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
    val statusColor = when {
        domain.daysLeft <= 0 -> Color(0xFFEF4444)
        domain.daysLeft <= 14 -> Color(0xFFF59E0B)
        else -> Color(0xFF10B981)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SoftDarkBg,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            Column {
                Text(domain.domain, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Text("Issuer: ${domain.issuer}", fontSize = 12.sp, color = TextGray)
            }

            // Expiry Info
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Days Remaining", color = TextGray, fontSize = 12.sp)
                        Text("${domain.daysLeft} Days", color = statusColor, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (domain.daysLeft.toFloat() / 365f).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(8.dp).background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp)),
                        color = statusColor,
                        trackColor = Color.Transparent
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("Expires on: ${sdf.format(Date(domain.expiryDate))}", color = Color.White, fontSize = 13.sp)
                }
            }

            // Group Info
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(AppIcons.groups(), null, tint = Color(0xFF6C8CFF), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Group: ${domain.groupName}", color = Color.White, fontSize = 14.sp)
            }

            // Reminder Settings
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Reminder Days", color = TextGray, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    domain.reminderDays.split(',').forEach { day ->
                        Surface(
                            color = Color(0xFF6C8CFF).copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFF6C8CFF).copy(alpha = 0.3f))
                        ) {
                            Text(
                                "$day Days", 
                                color = Color(0xFF6C8CFF), 
                                fontSize = 10.sp, 
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onRecheck,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C8CFF)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("RECHECK", fontWeight = FontWeight.Bold)
                }
                
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("DELETE", fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(Modifier.height(24.dp))
        }
    }
}
