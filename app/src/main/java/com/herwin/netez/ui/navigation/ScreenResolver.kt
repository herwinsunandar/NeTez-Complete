package com.herwin.netez.ui.navigation

private val appScreens = bottomNavItems + listOf(
    Screen.PingTest,
    Screen.Traceroute,
    Screen.DnsLookup,
    Screen.HttpCheck,
    Screen.SslChecker,
    Screen.SpeedTest,
    Screen.StabilityTest,
    Screen.BlacklistCheck,
    Screen.IspComparison,
    Screen.Security,
    Screen.LeakCheck,
    Screen.DnsLeakTest,
    Screen.PhishingCheck,
    Screen.SslReminder,
    Screen.PasswordStrength,
    Screen.PasswordManager,
    Screen.AppPermissionScanner,
    Screen.WifiSecurityAudit,
    Screen.DeviceSecurityCheck,
    Screen.Settings,
    Screen.LanScanner,
    Screen.PortScanner,
    Screen.WifiAnalyzer,
    Screen.BluetoothScanner,
    Screen.MdnsScanner,
    Screen.ChannelGraph,
    Screen.UpnpScanner,
    Screen.SnmpScanner,
    Screen.IpCalculator,
    Screen.QuickNotes,
    Screen.WhoisInfo,
    Screen.HttpHeader,
    Screen.HttpLab,
    Screen.MonitoringLab,
    Screen.WakeOnLan,
    Screen.EngTools,
    Screen.SshClient,
    Screen.TelnetClient,
    Screen.SftpClient,
    Screen.TftpClient,
    Screen.SerialConsole,
    Screen.WifiDetail,
    Screen.CellularDetail,
    Screen.History
)

fun resolveCurrentScreen(route: String?): Screen {
    val routeName = route?.substringBefore('?')?.substringBefore('/')
    return appScreens.firstOrNull { screen ->
        screen.route.substringBefore('?').substringBefore('/') == routeName
    } ?: Screen.Home
}

fun isBottomNavDestination(route: String?): Boolean =
    bottomNavItems.any { it.route == route }
