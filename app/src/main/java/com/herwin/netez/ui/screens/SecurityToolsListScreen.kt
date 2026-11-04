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
fun SecurityToolsListScreen(
    onBack: () -> Unit,
    onNavigateToTool: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Base64 Encoder/Decoder
            item {
                ToolItem(
                    title = stringResource(R.string.sectool_base64_title),
                    subtitle = stringResource(R.string.sectool_base64_desc),
                    painter = AppIcons.terminal(),
                    onClick = { onNavigateToTool("BASE64") }
                )
            }

            // 2. Checksum Calculator
            item {
                ToolItem(
                    title = stringResource(R.string.sectool_checksum_title),
                    subtitle = stringResource(R.string.sectool_checksum_desc),
                    painter = AppIcons.checkCircle(),
                    onClick = { onNavigateToTool("CHECKSUM") }
                )
            }

            // 3. Hash Calculator
            item {
                ToolItem(
                    title = stringResource(R.string.sectool_hash_title),
                    subtitle = stringResource(R.string.sectool_hash_desc),
                    painter = AppIcons.key(),
                    onClick = { onNavigateToTool("HASH_CALCULATOR") }
                )
            }

            // 4. HMAC Generator
            item {
                ToolItem(
                    title = stringResource(R.string.sectool_hmac_title),
                    subtitle = stringResource(R.string.sectool_hmac_desc),
                    painter = AppIcons.shield(),
                    onClick = { onNavigateToTool("HMAC_GENERATOR") }
                )
            }

            // 5. JWT Decoder
            item {
                ToolItem(
                    title = stringResource(R.string.sectool_jwt_title),
                    subtitle = stringResource(R.string.sectool_jwt_desc),
                    painter = AppIcons.verifiedUser(),
                    onClick = { onNavigateToTool("JWT_DECODER") }
                )
            }

            // 6. Random Token Generator
            item {
                ToolItem(
                    title = stringResource(R.string.sectool_token_title),
                    subtitle = stringResource(R.string.sectool_token_desc),
                    painter = AppIcons.lock(),
                    onClick = { onNavigateToTool("RANDOM_TOKEN") }
                )
            }

            // 7. Regex Tester
            item {
                ToolItem(
                    title = stringResource(R.string.sectool_regex_title),
                    subtitle = stringResource(R.string.sectool_regex_desc),
                    painter = AppIcons.search(),
                    onClick = { onNavigateToTool("REGEX") }
                )
            }

            // 8. Timestamp Converter
            item {
                ToolItem(
                    title = stringResource(R.string.sectool_timestamp_title),
                    subtitle = stringResource(R.string.sectool_timestamp_desc),
                    painter = AppIcons.schedule(),
                    onClick = { onNavigateToTool("TIMESTAMP") }
                )
            }

            // 9. URL Encoder/Decoder
            item {
                ToolItem(
                    title = stringResource(R.string.sectool_url_title),
                    subtitle = stringResource(R.string.sectool_url_desc),
                    painter = AppIcons.link(),
                    onClick = { onNavigateToTool("URL_ENCODER") }
                )
            }

            // 10. UUID Generator
            item {
                ToolItem(
                    title = stringResource(R.string.sectool_uuid_title),
                    subtitle = stringResource(R.string.sectool_uuid_desc),
                    painter = AppIcons.build(),
                    onClick = { onNavigateToTool("UUID_GENERATOR") }
                )
            }
        }
    }
}
