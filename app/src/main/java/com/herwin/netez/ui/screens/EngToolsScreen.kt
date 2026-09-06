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
fun EngToolsScreen(
    onNavigateToSsh: () -> Unit,
    onNavigateToTelnet: () -> Unit,
    onNavigateToSftp: () -> Unit,
    onNavigateToTftp: () -> Unit,
    onNavigateToSerial: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(PremiumNavy),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ToolItem(
                title = stringResource(R.string.eng_ssh),
                subtitle = "SSH Terminal with Session support",
                painter = AppIcons.terminal(),
                onClick = onNavigateToSsh
            )
        }
        item {
            ToolItem(
                title = stringResource(R.string.eng_telnet),
                subtitle = "Simple Telnet remote access",
                painter = AppIcons.terminal(),
                onClick = onNavigateToTelnet
            )
        }
        item {
            ToolItem(
                title = stringResource(R.string.eng_sftp),
                subtitle = "Secure file transfer protocol",
                painter = AppIcons.fileUpload(),
                onClick = onNavigateToSftp
            )
        }
        item {
            ToolItem(
                title = stringResource(R.string.eng_tftp),
                subtitle = "Trivial file transfer protocol",
                painter = AppIcons.folder(),
                onClick = onNavigateToTftp
            )
        }
        item {
            ToolItem(
                title = stringResource(R.string.eng_serial),
                subtitle = stringResource(R.string.eng_serial_desc),
                painter = AppIcons.usb(),
                onClick = onNavigateToSerial
            )
        }
    }
}
