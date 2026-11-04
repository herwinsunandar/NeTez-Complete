package com.herwin.netez.ui

import android.app.ActivityOptions
import android.content.Intent
import android.view.View
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.herwin.netez.R
import com.herwin.netez.data.SecurityPreferences
import com.herwin.netez.ui.fragments.SecurityPostureDetailFragment
import com.herwin.netez.ui.navigation.Screen
import com.herwin.netez.ui.screens.*
import com.herwin.netez.ui.viewmodels.*
import com.herwin.netez.utils.BiometricHelper

@Composable
fun NeTezNavGraph(
    navController: NavHostController,
    activity: FragmentActivity?,
    securityPrefs: SecurityPreferences,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToPing = { navController.navigate("ping_test?ip=") },
                onNavigateToSpeed = { navController.navigate(Screen.SpeedTest.route) },
                onNavigateToWhois = { navController.navigate("whois_info?host=") },
                onNavigateToIpCalc = { navController.navigate(Screen.IpCalculator.route) },
                onNavigateToMonitoring = { navController.navigate(Screen.MonitoringLab.route) },
                onNavigateToWifiDetail = { bssid -> navController.navigate("wifi_detail/$bssid") },
                onNavigateToCellularDetail = { navController.navigate(Screen.CellularDetail.route) },
                onNavigateToPasswordManager = {
                    navController.navigate(Screen.PasswordManager.route)
                },
                onNavigateToSslReminder = { navController.navigate(Screen.SslReminder.route) },
                onNavigateToSecurity = { navController.navigate(Screen.Security.route) },
                onNavigateToSecurityPostureDetail = { navController.navigate(Screen.SecurityPostureDetail.route) },
                onNavigateToSecurityIntelligence = { navController.navigate(Screen.SecurityIntelligence.route) },
                onNavigateToEngTools = { navController.navigate(Screen.EngTools.route) },
                onNavigateToSecurityToolsList = { navController.navigate(Screen.SecurityToolsList.route) }
            )
        }
        composable(Screen.SecurityPostureDetail.route) {
            val fragmentId = remember { View.generateViewId() }
            AndroidView(
                factory = { context ->
                    FragmentContainerView(context).apply {
                        id = fragmentId
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { view ->
                    val existing = activity?.supportFragmentManager?.findFragmentById(view.id)
                    if (existing == null) {
                        val fragment = SecurityPostureDetailFragment().apply {
                            onNavigateToDeviceCheck = { navController.navigate(Screen.DeviceSecurityCheck.route) }
                            onNavigateToNetworkAudit = { navController.navigate(Screen.WifiSecurityAudit.route) }
                            onNavigateToAppScanner = { navController.navigate(Screen.AppPermissionScanner.route) }
                            onNavigateToPrivacy = { navController.navigate(Screen.Security.route) }
                            onNavigateToSslReminder = { navController.navigate(Screen.SslReminder.route) }
                        }
                        activity?.supportFragmentManager?.beginTransaction()
                            ?.replace(view.id, fragment)
                            ?.commitAllowingStateLoss()
                    }
                }
            )
        }
        composable(Screen.Diagnostic.route) {
            DiagnosticScreen(
                onNavigateToPing = { navController.navigate("ping_test?ip=") },
                onNavigateToTraceroute = { navController.navigate("traceroute?host=") },
                onNavigateToDns = { navController.navigate(Screen.DnsLookup.route) },
                onNavigateToSpeed = { navController.navigate(Screen.SpeedTest.route) },
                onNavigateToStability = { navController.navigate(Screen.StabilityTest.route) },
                onNavigateToHttpCheck = { navController.navigate(Screen.HttpCheck.route) },
                onNavigateToIspComparison = { navController.navigate(Screen.IspComparison.route) },
                onNavigateToSslChecker = { navController.navigate("ssl_checker?host=") },
                onNavigateToBlacklist = { navController.navigate(Screen.BlacklistCheck.route) }
            )
        }
        composable(Screen.Security.route) {
            SecurityScreen(
                onNavigateToSecurityIntelligence = { navController.navigate(Screen.SecurityIntelligence.route) },
                onNavigateToLeakCheck = { navController.navigate(Screen.LeakCheck.route) },
                onNavigateToDnsLeakTest = { navController.navigate(Screen.DnsLeakTest.route) },
                onNavigateToPhishingCheck = { navController.navigate(Screen.PhishingCheck.route) },
                onNavigateToPasswordStrength = { navController.navigate(Screen.PasswordStrength.route) },
                onNavigateToPasswordManager = {
                    navController.navigate(Screen.PasswordManager.route)
                },
                onNavigateToBlacklist = { navController.navigate(Screen.BlacklistCheck.route) },
                onNavigateToSslReminder = { navController.navigate(Screen.SslReminder.route) },
                onNavigateToAppPermissionScanner = { navController.navigate(Screen.AppPermissionScanner.route) },
                onNavigateToWifiSecurityAudit = { navController.navigate(Screen.WifiSecurityAudit.route) },
                onNavigateToDeviceSecurityCheck = { navController.navigate(Screen.DeviceSecurityCheck.route) },
                onNavigateToSecurityToolsList = { navController.navigate(Screen.SecurityToolsList.route) }
            )
        }
        composable(Screen.SecurityToolsList.route) {
            SecurityToolsListScreen(
                onBack = { navController.popBackStack() },
                onNavigateToTool = { toolType -> navController.navigate("security_tool?toolType=$toolType") }
            )
        }
        composable(
            Screen.SecurityTool.route,
            arguments = listOf(stringArgument("toolType"))
        ) { entry ->
            val toolType = entry.arguments?.getString("toolType") ?: "HASH_CALCULATOR"
            SecurityToolScreen(toolType = toolType, onBack = { navController.popBackStack() })
        }
        composable(Screen.WifiSecurityAudit.route) {
            WifiSecurityScreen(
                onNavigateToLanScanner = { navController.navigate(Screen.LanScanner.route) },
                onNavigateToDnsLeakTest = { navController.navigate(Screen.DnsLeakTest.route) }
            )
        }
        composable(Screen.DeviceSecurityCheck.route) {
            DeviceSecurityCheckScreen()
        }
        composable(Screen.DnsLeakTest.route) {
            DnsLeakTestScreen()
        }
        composable(Screen.AppPermissionScanner.route) {
            AppPermissionScannerScreen()
        }
        composable(Screen.SecurityIntelligence.route) {
            SecurityIntelligenceScreen(
                onBack = { navController.popBackStack() },
                onNavigateToUpgradeToPro = { navController.navigate(Screen.UpgradeToPro.route) }
            )
        }
        composable(Screen.LeakCheck.route) {
            LeakCheckScreen()
        }
        composable(Screen.PhishingCheck.route) {
            PhishingCheckScreen()
        }
        composable(Screen.SslReminder.route) {
            SslReminderScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.PasswordManager.route) {
            CredentialVaultScreen(
                onBack = { navController.popBackStack() },
                onNavigateToSsh = { ip -> navController.navigate("ssh_client?ip=$ip") },
                onNavigateToSftp = { navController.navigate(Screen.SftpClient.route) }
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
                onNavigateToSnmp = { navController.navigate(Screen.SnmpScanner.route) }
            )
        }
        composable(Screen.UpnpScanner.route) { UpnpScannerScreen() }
        composable(Screen.SnmpScanner.route) {
            SnmpScannerScreen(
                onNavigateToPing = { ip -> navController.navigate("ping_test?ip=$ip") },
                onNavigateToSsh = { ip -> navController.navigate("ssh_client?ip=$ip") },
                onNavigateToWol = { navController.navigate(Screen.WakeOnLan.route) }
            )
        }
        composable(Screen.Utilities.route) {
            UtilitiesScreen(
                onNavigateToIpCalc = { navController.navigate(Screen.IpCalculator.route) },
                onNavigateToQuickNotes = { navController.navigate(Screen.QuickNotes.route) },
                onNavigateToWhois = { navController.navigate("whois_info?host=") },
                onNavigateToHttp = { navController.navigate(Screen.HttpHeader.route) },
                onNavigateToHttpLab = { navController.navigate("http_lab?url=") },
                onNavigateToWol = { navController.navigate(Screen.WakeOnLan.route) },
                onNavigateToMonitoring = { navController.navigate(Screen.MonitoringLab.route) },
                onNavigateToEngTools = { navController.navigate(Screen.EngTools.route) }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateToHistory = { navController.navigate(Screen.History.route) },
                onNavigateToUserGuide = { navController.navigate(Screen.UserGuide.route) },
                onNavigateToUpgradeToPro = { navController.navigate(Screen.UpgradeToPro.route) }
            )
        }
        composable(Screen.UpgradeToPro.route) {
            UpgradeToProScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.UserGuide.route) {
            UserGuideScreen(
                onBack = { navController.popBackStack() },
                onNavigateToRoute = { route -> navController.navigate(route) }
            )
        }
        composable(Screen.PingTest.route, arguments = listOf(stringArgument("ip"))) { entry ->
            val ip = entry.arguments?.getString("ip") ?: ""
            val viewModel: PingViewModel = viewModel()
            LaunchedEffect(ip) {
                if (ip.isNotEmpty()) viewModel.updateHost(ip)
            }
            PingScreen(viewModel = viewModel, onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.Traceroute.route, arguments = listOf(stringArgument("host"))) { entry ->
            val host = entry.arguments?.getString("host") ?: ""
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
                onNavigateToPing = { host -> navController.navigate("ping_test?ip=$host") },
                onNavigateToTraceroute = { host -> navController.navigate("traceroute?host=$host") }
            )
        }
        composable(Screen.HttpCheck.route) { HttpCheckScreen() }
        composable(Screen.SslChecker.route, arguments = listOf(stringArgument("host"))) { entry ->
            val host = entry.arguments?.getString("host") ?: ""
            val viewModel: SslCheckerViewModel = viewModel()
            LaunchedEffect(host) {
                if (host.isNotEmpty()) {
                    viewModel.target = host
                    viewModel.checkSsl()
                }
            }
            SslCheckerScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(Screen.BlacklistCheck.route) { BlacklistScreen() }
        composable(Screen.PasswordStrength.route) {
            PasswordStrengthScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.IspComparison.route) {
            IspComparisonScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.SpeedTest.route) { SpeedTestScreen() }
        composable(Screen.StabilityTest.route) {
            val viewModel: StabilityTestViewModel = viewModel()
            StabilityTestScreen(viewModel = viewModel, onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.LanScanner.route) {
            LanScannerScreen(
                onNavigateToPing = { ip -> navController.navigate("ping_test?ip=$ip") },
                onNavigateToPortScan = { ip -> navController.navigate("port_scanner?ip=$ip") },
                onNavigateToWifiAudit = { navController.navigate(Screen.WifiSecurityAudit.route) }
            )
        }
        composable(Screen.PortScanner.route, arguments = listOf(stringArgument("ip"))) { entry ->
            val ip = entry.arguments?.getString("ip") ?: ""
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
        composable(Screen.WifiDetail.route, arguments = listOf(navArgument("bssid") { type = NavType.StringType })) { entry ->
            val bssid = entry.arguments?.getString("bssid") ?: ""
            WifiDetailScreen(bssid = bssid, onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.CellularDetail.route) {
            CellularDetailScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.IpCalculator.route) {
            IpCalculatorScreen(
                onNavigateToPing = { _: String -> navController.navigate(Screen.PingTest.route) },
                onNavigateToScan = { _: String -> navController.navigate(Screen.LanScanner.route) }
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
        composable(Screen.WhoisInfo.route, arguments = listOf(stringArgument("host"))) { entry ->
            val host = entry.arguments?.getString("host") ?: ""
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
        composable(Screen.HttpLab.route, arguments = listOf(stringArgument("url"))) { entry ->
            val url = entry.arguments?.getString("url") ?: ""
            val viewModel: HttpLabViewModel = viewModel()
            LaunchedEffect(url) {
                if (url.isNotEmpty()) viewModel.updateUrl(url)
            }
            HttpLabScreen(onBack = { navController.popBackStack() }, viewModel = viewModel)
        }
        composable(Screen.MonitoringLab.route) {
            val viewModel: MonitoringLabViewModel = viewModel()
            val context = LocalContext.current
            MonitoringLabScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNavigateToDetail = { targetId ->
                    val intent = Intent(context, MonitoringDetailActivity::class.java).apply {
                        putExtra("TARGET_ID", targetId)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    val options = ActivityOptions.makeCustomAnimation(
                        context,
                        android.R.anim.fade_in,
                        android.R.anim.fade_out
                    )
                    context.startActivity(intent, options.toBundle())
                }
            )
        }
        composable(Screen.WakeOnLan.route) { WolScreen() }
        composable(Screen.EngTools.route) {
            EngToolsScreen(
                onNavigateToSsh = { navController.navigate("ssh_client?ip=") },
                onNavigateToTelnet = { navController.navigate(Screen.TelnetClient.route) },
                onNavigateToSftp = { navController.navigate(Screen.SftpClient.route) },
                onNavigateToTftp = { navController.navigate(Screen.TftpClient.route) },
                onNavigateToSerial = { navController.navigate(Screen.SerialConsole.route) }
            )
        }
        composable(Screen.SshClient.route, arguments = listOf(stringArgument("ip"))) { entry ->
            val ip = entry.arguments?.getString("ip") ?: ""
            SshClientScreen(initialIp = ip)
        }
        composable(Screen.TelnetClient.route) { TelnetClientScreen() }
        composable(Screen.SftpClient.route) { SftpClientScreen() }
        composable(Screen.TftpClient.route) { TftpClientScreen() }
        composable(Screen.SerialConsole.route) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color(0xFF0F1220)),
                contentAlignment = Alignment.Center
            ) {
                Text("Serial Console (USB OTG) Coming Soon", color = Color.White)
            }
        }
        composable(Screen.History.route) { HistoryScreen() }
    }
}

private fun stringArgument(name: String) = navArgument(name) {
    type = NavType.StringType
    defaultValue = ""
}
