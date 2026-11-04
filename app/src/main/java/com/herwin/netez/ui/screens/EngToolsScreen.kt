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
fun EngToolsScreen(
    onNavigateToSsh: () -> Unit,
    onNavigateToTelnet: () -> Unit,
    onNavigateToSftp: () -> Unit,
    onNavigateToTftp: () -> Unit,
    onNavigateToSerial: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Serial Console
        item {
            ToolItem(
                title = stringResource(R.string.eng_serial),
                subtitle = stringResource(R.string.eng_serial_desc),
                painter = AppIcons.usb(),
                onClick = onNavigateToSerial
            )
        }
        // 2. SFTP Client
        item {
            ToolItem(
                title = stringResource(R.string.eng_sftp),
                subtitle = "Secure file transfer protocol",
                painter = AppIcons.fileUpload(),
                onClick = onNavigateToSftp
            )
        }
        // 3. SSH Client
        item {
            ToolItem(
                title = stringResource(R.string.eng_ssh),
                subtitle = "SSH Terminal with Session support",
                painter = AppIcons.terminal(),
                onClick = onNavigateToSsh
            )
        }
        // 4. Telnet Client
        item {
            ToolItem(
                title = stringResource(R.string.eng_telnet),
                subtitle = "Simple Telnet remote access",
                painter = AppIcons.terminal(),
                onClick = onNavigateToTelnet
            )
        }
        // 5. TFTP Client
        item {
            ToolItem(
                title = stringResource(R.string.eng_tftp),
                subtitle = "Trivial file transfer protocol",
                painter = AppIcons.folder(),
                onClick = onNavigateToTftp
            )
        }
    }
}
