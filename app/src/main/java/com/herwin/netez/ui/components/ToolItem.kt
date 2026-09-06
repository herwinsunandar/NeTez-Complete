package com.herwin.netez.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.herwin.netez.ui.AppIcons

@Composable
fun ToolItem(
    title: String, 
    subtitle: String, 
    painter: Painter, 
    showBadge: Boolean = false,
    badgeText: String = "NEW",
    isError: Boolean = false,
    showDot: Boolean = false,
    dotColor: Color = Color(0xFF10B981),
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF1E2235),
        border = if (isError) BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)) else null,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF6C8CFF).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painter, 
                    contentDescription = null, 
                    tint = Color(0xFF6C8CFF),
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title, 
                        style = MaterialTheme.typography.titleMedium, 
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                    if (showBadge) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            color = if (isError) Color(0xFFEF4444) else Color(0xFF6C8CFF),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                badgeText, 
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                    
                    if (showDot) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle, 
                    style = MaterialTheme.typography.bodySmall, 
                    color = Color(0xFF8B92B5),
                    fontSize = 13.sp
                )
            }
            
            Icon(
                painter = AppIcons.chevronRight(),
                contentDescription = null,
                tint = Color(0xFF4B516E),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
