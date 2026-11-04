package com.herwin.netez.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.herwin.netez.R
import com.herwin.netez.data.SecurityPreferences
import com.herwin.netez.ui.navigation.Screen
import com.herwin.netez.ui.navigation.bottomNavItems
import com.herwin.netez.ui.navigation.isBottomNavDestination
import com.herwin.netez.ui.navigation.resolveCurrentScreen
import com.herwin.netez.ui.viewmodels.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeTezApp() {
    val context = LocalContext.current
    val activity = context as? androidx.fragment.app.FragmentActivity
    val securityPrefs = remember { SecurityPreferences(context) }
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route
    val currentScreen = resolveCurrentScreen(currentRoute)
    
    val dashboardViewModel: DashboardViewModel = viewModel()
    val dashboardState by dashboardViewModel.uiState.collectAsState()

    LaunchedEffect(currentRoute) {
        if (currentRoute == Screen.PasswordManager.route) {
            activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(currentScreen.titleRes),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    if (isBottomNavDestination(currentRoute)) {
                        Box(modifier = Modifier.padding(start = 12.dp)) {
                            AsyncImage(
                                model = R.mipmap.ic_launcher,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    } else {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_back),
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_settings),
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                bottomNavItems.forEach { screen ->
                    val hasBadge = screen == Screen.Security && (dashboardState.breachedEmailCount > 0 || dashboardState.expiringSslCount > 0)
                    val isTabSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true

                    NavigationBarItem(
                        icon = { 
                            BadgedBox(
                                badge = {
                                    if (hasBadge) {
                                        Badge(containerColor = MaterialTheme.colorScheme.error)
                                    }
                                }
                            ) {
                                Icon(painterResource(screen.iconRes), contentDescription = null)
                            }
                        },
                        label = { Text(stringResource(screen.titleRes), style = MaterialTheme.typography.labelSmall) },
                        selected = isTabSelected,
                        onClick = {
                            if (isTabSelected) {
                                // Re-clicking active tab -> pop back to root destination of this tab
                                if (currentRoute != screen.route) {
                                    navController.popBackStack(screen.route, inclusive = false)
                                }
                            } else {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NeTezNavGraph(
            navController = navController,
            activity = activity,
            securityPrefs = securityPrefs,
            modifier = Modifier.padding(innerPadding)
        )
    }
}
