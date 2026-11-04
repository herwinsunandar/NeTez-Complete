package com.herwin.netez.ui.screens

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.herwin.netez.ui.viewmodels.PasswordManagerViewModel

@Composable
fun PasswordManagerScreen(
    onBack: () -> Unit,
    onNavigateToSsh: (String) -> Unit = {},
    onNavigateToSftp: (String) -> Unit = {},
    viewModel: PasswordManagerViewModel = viewModel()
) {
    CredentialVaultScreen(
        onBack = onBack,
        onNavigateToSsh = onNavigateToSsh,
        onNavigateToSftp = onNavigateToSftp,
        viewModel = viewModel
    )
}
