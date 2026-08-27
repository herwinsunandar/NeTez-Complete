package com.herwin.netez.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.herwin.netez.ui.navigation.Screen
import com.herwin.netez.ui.navigation.bottomNavItems
import com.herwin.netez.ui.screens.*
import com.herwin.netez.ui.viewmodels.PingViewModel
import com.herwin.netez.ui.viewmodels.PortScannerViewModel
import com.herwin.netez.ui.viewmodels.HttpLabViewModel
import com.herwin.netez.ui.viewmodels.TracerouteViewModel
import com.herwin.netez.ui.viewmodels.WhoisViewModel
import com.herwin.netez.ui.viewmodels.SslCheckerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeTezApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val currentScreen = remember(currentDestination) {
        bottomNavItems.find { it.route == currentDestination?.route } ?: when {
            currentDestination?.route?.contains("wifi_detail") == true -> Screen.WifiDetail
            currentDestination?.route == Screen.PingTest.route -> Screen.PingTest
            currentDestination?.route == Screen.Traceroute.route -> Screen.Traceroute
            currentDestination?.route == Screen.DnsLookup.route -> Screen.DnsLookup
            currentDestination?.route == Screen.SpeedTest.route -> Screen.SpeedTest
            currentDestination?.route == Screen.LanScanner.route -> Screen.LanScanner
            currentDestination?.route == Screen.PortScanner.route -> Screen.PortScanner
            currentDestination?.route == Screen.IpCalculator.route -> Screen.IpCalculator
            currentDestination?.route == Screen.QuickNotes.route -> Screen.QuickNotes
            currentDestination?.route == Screen.WhoisInfo.route -> Screen.WhoisInfo
            currentDestination?.route == Screen.HttpHeader.route -> Screen.HttpHeader
            currentDestination?.route == Screen.HttpCheck.route -> Screen.HttpCheck
            currentDestination?.route == Screen.SslChecker.route -> Screen.SslChecker
            currentDestination?.route == Screen.BlacklistCheck.route -> Screen.BlacklistCheck
            currentDestination?.route == Screen.WakeOnLan.route -> Screen.WakeOnLan
            currentDestination?.route == Screen.EngTools.route -> Screen.EngTools
            currentDestination?.route == Screen.SshClient.route -> Screen.SshClient
            currentDestination?.route == Screen.TelnetClient.route -> Screen.TelnetClient
            currentDestination?.route == Screen.SftpClient.route -> Screen.SftpClient
            currentDestination?.route == Screen.TftpClient.route -> Screen.TftpClient
            currentDestination?.route == Screen.History.route -> Screen.History
            currentDestination?.route == Screen.CellularDetail.route -> Screen.CellularDetail
            currentDestination?.route == Screen.UpnpScanner.route -> Screen.UpnpScanner
            currentDestination?.route == Screen.SnmpScanner.route -> Screen.SnmpScanner
            currentDestination?.route == Screen.MdnsScanner.route -> Screen.MdnsScanner
            else -> Screen.Home
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
                    if (!bottomNavItems.any { it.route == currentDestination?.route }) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF252A40),
                tonalElevation = 8.dp
            ) {
                bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(stringResource(screen.titleRes), style = MaterialTheme.typography.labelSmall) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) { 
                HomeScreen(
                    onNavigateToPing = { navController.navigate("ping_test?ip=") },
                    onNavigateToSpeed = { navController.navigate(Screen.SpeedTest.route) },
                    onNavigateToWhois = { navController.navigate("whois_info?host=") },
                    onNavigateToIpCalc = { navController.navigate(Screen.IpCalculator.route) },
                    onNavigateToWifiDetail = { bssid -> navController.navigate("wifi_detail/$bssid") },
                    onNavigateToCellularDetail = { navController.navigate(Screen.CellularDetail.route) }
                ) 
            }
            composable(Screen.Diagnostic.route) { 
                DiagnosticScreen(
                    onNavigateToPing = { navController.navigate("ping_test?ip=") },
                    onNavigateToTraceroute = { navController.navigate("traceroute?host=") },
                    onNavigateToDns = { navController.navigate(Screen.DnsLookup.route) },
                    onNavigateToSpeed = { navController.navigate(Screen.SpeedTest.route) },
                    onNavigateToHttpCheck = { navController.navigate(Screen.HttpCheck.route) },
                    onNavigateToSslChecker = { navController.navigate("ssl_checker?host=") },
                    onNavigateToBlacklist = { navController.navigate(Screen.BlacklistCheck.route) }
                ) 
            }
            composable(Screen.Scanner.route) { 
                ScannerScreen(
                    onNavigateToLan = { navController.navigate(Screen.LanScanner.route) },
                    onNavigateToPort = { navController.navigate("port_scanner?ip=") },
                    onNavigateToWifi = { navController.navigate(Screen.WifiAnalyzer.route) },
                    onNavigateToBluetooth = { navController.navigate(Screen.BluetoothScanner.route) },
                    onNavigateToMdns = { navController.navigate(Screen.MdnsScanner.route) },
                    onNavigateToChannel = { navController.navigate(Screen.ChannelGraph.route) },
                    onNavigateToUpnp = { navController.navigate(Screen.UpnpScanner.route) },
                    onNavigateToWol = { navController.navigate(Screen.WakeOnLan.route) },
                    onNavigateToSnmp = { navController.navigate(Screen.SnmpScanner.route) }
                )
            }
            composable(Screen.UpnpScanner.route) { UpnpScannerScreen() }
            composable(Screen.SnmpScanner.route) { SnmpScannerScreen() }
            composable(Screen.Utilities.route) { 
                UtilitiesScreen(
                    onNavigateToIpCalc = { navController.navigate(Screen.IpCalculator.route) },
                    onNavigateToQuickNotes = { navController.navigate(Screen.QuickNotes.route) },
                    onNavigateToWhois = { navController.navigate("whois_info?host=") },
                    onNavigateToHttp = { navController.navigate(Screen.HttpHeader.route) },
                    onNavigateToHttpLab = { navController.navigate("http_lab?url=") },
                    onNavigateToWol = { navController.navigate(Screen.WakeOnLan.route) },
                    onNavigateToEngTools = { navController.navigate(Screen.EngTools.route) }
                )
            }
            composable(Screen.Settings.route) { 
                SettingsScreen(onNavigateToHistory = { navController.navigate(Screen.History.route) }) 
            }
            
            composable(
                route = Screen.PingTest.route,
                arguments = listOf(navArgument("ip") { type = NavType.StringType; defaultValue = "" })
            ) { backStackEntry ->
                val ip = backStackEntry.arguments?.getString("ip") ?: ""
                val viewModel: PingViewModel = viewModel()
                LaunchedEffect(ip) {
                    if (ip.isNotEmpty()) viewModel.updateHost(ip)
                }
                PingScreen(viewModel = viewModel, onNavigateBack = { navController.popBackStack() }) 
            }
            composable(
                route = Screen.Traceroute.route,
                arguments = listOf(navArgument("host") { type = NavType.StringType; defaultValue = "" })
            ) { backStackEntry ->
                val host = backStackEntry.arguments?.getString("host") ?: ""
                val viewModel: TracerouteViewModel = viewModel()
                LaunchedEffect(host) {
                    if (host.isNotEmpty()) {
                        viewModel.updateHost(host)
                        viewModel.toggleTrace()
                    }
                }
                TracerouteScreen(viewModel = viewModel)
            }
            composable(Screen.DnsLookup.route) { 
                DnsScreen(
                    onNavigateToPing = { host ->
                        navController.navigate("ping_test?ip=$host")
                    },
                    onNavigateToTraceroute = { host ->
                        navController.navigate("traceroute?host=$host")
                    }
                )
            }
            composable(Screen.HttpCheck.route) { HttpCheckScreen() }
            composable(
                route = Screen.SslChecker.route,
                arguments = listOf(navArgument("host") { type = NavType.StringType; defaultValue = "" })
            ) { backStackEntry ->
                val host = backStackEntry.arguments?.getString("host") ?: ""
                val viewModel: SslCheckerViewModel = viewModel()
                LaunchedEffect(host) {
                    if (host.isNotEmpty()) {
                        viewModel.target = host
                        viewModel.checkSsl()
                    }
                }
                SslCheckerScreen(viewModel = viewModel, onBack = { navController.popBackStack() }) 
            }
            composable(Screen.BlacklistCheck.route) { 
                BlacklistScreen() 
            }
            composable(Screen.SpeedTest.route) { SpeedTestScreen() }
            
            composable(Screen.LanScanner.route) { 
                LanScannerScreen(
                    onNavigateToPing = { ip -> navController.navigate("ping_test?ip=$ip") },
                    onNavigateToPortScan = { ip -> navController.navigate("port_scanner?ip=$ip") }
                )
            }
            composable(
                route = Screen.PortScanner.route,
                arguments = listOf(navArgument("ip") { type = NavType.StringType; defaultValue = "" })
            ) { backStackEntry ->
                val ip = backStackEntry.arguments?.getString("ip") ?: ""
                val viewModel: PortScannerViewModel = viewModel()
                LaunchedEffect(ip) {
                    if (ip.isNotEmpty()) viewModel.updateIp(ip)
                }
                PortScannerScreen(viewModel = viewModel) 
            }
            composable(Screen.WifiAnalyzer.route) { 
                WifiScannerScreen(onNavigateToDetail = { bssid ->
                    navController.navigate("wifi_detail/$bssid")
                }) 
            }
            composable(Screen.BluetoothScanner.route) { BleScannerScreen() }
            composable(Screen.MdnsScanner.route) { MdnsScannerScreen() }
            composable(Screen.ChannelGraph.route) { ChannelGraphScreen() }
            
            composable(
                route = Screen.WifiDetail.route,
                arguments = listOf(navArgument("bssid") { type = NavType.StringType })
            ) { backStackEntry ->
                val bssid = backStackEntry.arguments?.getString("bssid") ?: ""
                WifiDetailScreen(bssid = bssid, onNavigateBack = { navController.popBackStack() })
            }

            composable(Screen.CellularDetail.route) {
                CellularDetailScreen(onNavigateBack = { navController.popBackStack() })
            }
            
            composable(Screen.IpCalculator.route) { 
                IpCalculatorScreen(
                    onNavigateToPing = { _: String -> 
                        navController.navigate(Screen.PingTest.route) 
                    },
                    onNavigateToScan = { _: String ->
                        navController.navigate(Screen.LanScanner.route)
                    }
                ) 
            }
            composable(Screen.QuickNotes.route) {
                QuickNotesScreen(
                    onPing = { ip -> navController.navigate("ping_test?ip=$ip") },
                    onTraceroute = { host -> navController.navigate("traceroute?host=$host") },
                    onWhois = { host -> navController.navigate("whois_info?host=$host") },
                    onSslCheck = { host -> navController.navigate("ssl_checker?host=$host") },
                    onTestApi = { url -> navController.navigate("http_lab?url=$url") }
                )
            }
            composable(
                route = Screen.WhoisInfo.route,
                arguments = listOf(navArgument("host") { type = NavType.StringType; defaultValue = "" })
            ) { backStackEntry ->
                val host = backStackEntry.arguments?.getString("host") ?: ""
                val viewModel: WhoisViewModel = viewModel()
                LaunchedEffect(host) {
                    if (host.isNotEmpty()) {
                        viewModel.updateQuery(host)
                        viewModel.fetchInfo()
                    }
                }
                WhoisScreen(viewModel = viewModel)
            }
            composable(Screen.HttpHeader.route) { HttpHeaderScreen() }
            composable(
                route = Screen.HttpLab.route,
                arguments = listOf(navArgument("url") { type = NavType.StringType; defaultValue = "" })
            ) { backStackEntry ->
                val url = backStackEntry.arguments?.getString("url") ?: ""
                val viewModel: HttpLabViewModel = viewModel()
                LaunchedEffect(url) {
                    if (url.isNotEmpty()) viewModel.updateUrl(url)
                }
                HttpLabScreen(onBack = { navController.popBackStack() }, viewModel = viewModel)
            }
            composable(Screen.WakeOnLan.route) { WolScreen() }
            
            composable(Screen.EngTools.route) {
                EngToolsScreen(
                    onNavigateToSsh = { navController.navigate(Screen.SshClient.route) },
                    onNavigateToTelnet = { navController.navigate(Screen.TelnetClient.route) },
                    onNavigateToSftp = { navController.navigate(Screen.SftpClient.route) },
                    onNavigateToTftp = { navController.navigate(Screen.TftpClient.route) }
                )
            }
            composable(Screen.SshClient.route) { SshClientScreen() }
            composable(Screen.TelnetClient.route) { TelnetClientScreen() }
            composable(Screen.SftpClient.route) { SftpClientScreen() }
            composable(Screen.TftpClient.route) { TftpClientScreen() }
            
            composable(Screen.History.route) { HistoryScreen() }
        }
    }
}
