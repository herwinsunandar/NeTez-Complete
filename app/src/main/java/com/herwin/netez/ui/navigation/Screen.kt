package com.herwin.netez.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.herwin.netez.R

sealed class Screen(val route: String, val titleRes: Int, val icon: ImageVector) {
    object Home : Screen("home", R.string.nav_dashboard, Icons.Default.Home)
    object Diagnostic : Screen("diagnostic", R.string.nav_diagnostic, Icons.Default.Analytics)
    object Scanner : Screen("scanner", R.string.nav_scanner, Icons.Default.Radar)
    object Utilities : Screen("utilities", R.string.nav_utilities, Icons.Default.Build)
    object Settings : Screen("settings", R.string.nav_settings, Icons.Default.Settings)
    
    // Sub-screens
    object PingTest : Screen("ping_test?ip={ip}", R.string.diag_ping, Icons.Default.NetworkCheck)
    object Traceroute : Screen("traceroute?host={host}", R.string.diag_traceroute, Icons.Default.Route)
    object DnsLookup : Screen("dns_lookup", R.string.diag_dns, Icons.Default.Dns)
    object HttpCheck : Screen("http_check", R.string.diag_http_check, Icons.Default.Language)
    object SslChecker : Screen("ssl_checker?host={host}", R.string.diag_ssl_checker, Icons.Default.Lock)
    object SpeedTest : Screen("speed_test", R.string.diag_speedtest, Icons.Default.Speed)
    object BlacklistCheck : Screen("blacklist_check", R.string.diag_blacklist, Icons.Default.Security)
    
    object LanScanner : Screen("lan_scanner", R.string.scan_lan, Icons.Default.Lan)
    object PortScanner : Screen("port_scanner?ip={ip}", R.string.scan_port, Icons.Default.Build)
    object WifiAnalyzer : Screen("wifi_analyzer", R.string.scan_wifi, Icons.Default.Radar)
    object BluetoothScanner : Screen("bluetooth_scanner", R.string.scan_bluetooth, Icons.Default.Bluetooth)
    object MdnsScanner : Screen("mdns_scanner", R.string.scan_mdns, Icons.Default.Search)
    object ChannelGraph : Screen("channel_graph", R.string.scan_channel_graph, Icons.Default.AutoGraph)
    object UpnpScanner : Screen("upnp_scanner", R.string.scan_upnp, Icons.Default.Router)
    object SnmpScanner : Screen("snmp_scanner", R.string.scan_snmp, Icons.Default.Memory)
    
    object IpCalculator : Screen("ip_calculator", R.string.util_ip_calc, Icons.Default.Calculate)
    object QuickNotes : Screen("quick_notes", R.string.util_quick_notes, Icons.Default.StickyNote2)
    object WhoisInfo : Screen("whois_info?host={host}", R.string.util_whois, Icons.Default.Info)
    object HttpHeader : Screen("http_header", R.string.util_http, Icons.Default.Http)
    object HttpLab : Screen("http_lab?url={url}", R.string.util_http_lab, Icons.Default.RocketLaunch)
    object WakeOnLan : Screen("wol", R.string.util_wol, Icons.Default.Power)
    
    object EngTools : Screen("eng_tools", R.string.util_eng_tools, Icons.Default.Build)
    object SshClient : Screen("ssh_client", R.string.eng_ssh, Icons.Default.Terminal)
    object TelnetClient : Screen("telnet_client", R.string.eng_telnet, Icons.Default.Terminal)
    object SftpClient : Screen("sftp_client", R.string.eng_sftp, Icons.Default.FileUpload)
    object TftpClient : Screen("tftp_client", R.string.eng_tftp, Icons.Default.Folder)

    object WifiDetail : Screen("wifi_detail/{bssid}", R.string.wifi_detail, Icons.Default.Radar)
    object CellularDetail : Screen("cellular_detail", R.string.cellular_detail, Icons.Default.SignalCellularAlt)
    object History : Screen("history", R.string.nav_report, Icons.Default.Analytics)
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Diagnostic,
    Screen.Scanner,
    Screen.Utilities,
    Screen.Settings
)
