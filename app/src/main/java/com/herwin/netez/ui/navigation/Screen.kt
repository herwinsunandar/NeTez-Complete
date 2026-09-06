package com.herwin.netez.ui.navigation

import com.herwin.netez.R

sealed class Screen(
    val route: String,
    val titleRes: Int,
    val iconRes: Int
) {
    object Home : Screen("home", R.string.nav_dashboard, iconRes = R.drawable.ic_home)
    object Diagnostic : Screen("diagnostic", R.string.nav_diagnostic, iconRes = R.drawable.ic_analytics)
    object Scanner : Screen("scanner", R.string.nav_scanner, iconRes = R.drawable.ic_scan_menu)
    object Utilities : Screen("utilities", R.string.nav_utilities, iconRes = R.drawable.ic_build)
    object Settings : Screen("settings", R.string.nav_settings, iconRes = R.drawable.ic_settings)
    
    // Sub-screens
    object PingTest : Screen("ping_test?ip={ip}", R.string.diag_ping, iconRes = R.drawable.ic_network_check)
    object Traceroute : Screen("traceroute?host={host}", R.string.diag_traceroute, iconRes = R.drawable.ic_route)
    object DnsLookup : Screen("dns_lookup", R.string.diag_dns, iconRes = R.drawable.ic_dns)
    object HttpCheck : Screen("http_check", R.string.diag_http_check, iconRes = R.drawable.ic_language)
    object SslChecker : Screen("ssl_checker?host={host}", R.string.diag_ssl_checker, iconRes = R.drawable.ic_lock)
    object SpeedTest : Screen("speed_test", R.string.diag_speedtest, iconRes = R.drawable.ic_speed)
    object StabilityTest : Screen("stability_test", R.string.diag_stability, iconRes = R.drawable.ic_network_check)
    object BlacklistCheck : Screen("blacklist_check", R.string.diag_blacklist, iconRes = R.drawable.ic_security)
    object IspComparison : Screen("isp_comparison", R.string.diag_isp_comparison, iconRes = R.drawable.ic_compare)
    object Security : Screen("security", R.string.diag_security, iconRes = R.drawable.ic_security)
    object LeakCheck : Screen("leak_check", R.string.security_leak_check, iconRes = R.drawable.ic_shield)
    object DnsLeakTest : Screen("dns_leak_test", R.string.dns_leak_title, iconRes = R.drawable.ic_dns)
    object PhishingCheck : Screen("phishing_check", R.string.security_phishing_check, iconRes = R.drawable.ic_public)
    object SslReminder : Screen("ssl_reminder", R.string.security_ssl_reminder, iconRes = R.drawable.ic_lock)
    object PasswordStrength : Screen("password_strength", R.string.nav_password_strength, iconRes = R.drawable.ic_key)
    object PasswordManager : Screen("password_manager", R.string.security_password_manager, iconRes = R.drawable.ic_lock)
    object AppPermissionScanner : Screen("app_permission_scanner", R.string.security_app_scanner_title, iconRes = R.drawable.ic_security)
    object WifiSecurityAudit : Screen("wifi_security_audit", R.string.security_wifi_audit_title, iconRes = R.drawable.ic_security)
    object DeviceSecurityCheck : Screen("device_security_check", R.string.device_check_title, iconRes = R.drawable.ic_shield_check)
    object SecurityPostureDetail : Screen("security_posture_detail", R.string.security_posture_detail_title, iconRes = R.drawable.ic_shield)
    
    object LanScanner : Screen("lan_scanner", R.string.scan_lan, iconRes = R.drawable.ic_lan)
    object PortScanner : Screen("port_scanner?ip={ip}", R.string.scan_port, iconRes = R.drawable.ic_build)
    object WifiAnalyzer : Screen("wifi_analyzer", R.string.scan_wifi, iconRes = R.drawable.ic_wifi_analyzer)
    object BluetoothScanner : Screen("bluetooth_scanner", R.string.scan_bluetooth, iconRes = R.drawable.ic_bluetooth)
    object MdnsScanner : Screen("mdns_scanner", R.string.scan_mdns, iconRes = R.drawable.ic_search)
    object ChannelGraph : Screen("channel_graph", R.string.scan_channel_graph, iconRes = R.drawable.ic_channel_graph)
    object UpnpScanner : Screen("upnp_scanner", R.string.scan_upnp, iconRes = R.drawable.ic_router)
    object SnmpScanner : Screen("snmp_scanner", R.string.scan_snmp, iconRes = R.drawable.ic_memory)
    
    object IpCalculator : Screen("ip_calculator", R.string.util_ip_calc, iconRes = R.drawable.ic_calculate)
    object QuickNotes : Screen("quick_notes", R.string.util_quick_notes, iconRes = R.drawable.ic_sticky_note)
    object WhoisInfo : Screen("whois_info?host={host}", R.string.util_whois, iconRes = R.drawable.ic_info)
    object HttpHeader : Screen("http_header", R.string.util_http, iconRes = R.drawable.ic_http_header)
    object HttpLab : Screen("http_lab?url={url}", R.string.util_http_lab, iconRes = R.drawable.ic_rocket)
    object MonitoringLab : Screen("monitoring_lab", R.string.util_monitoring_lab, iconRes = R.drawable.ic_monitor)
    object WakeOnLan : Screen("wol", R.string.util_wol, iconRes = R.drawable.ic_power)
    
    object EngTools : Screen("eng_tools", R.string.util_eng_tools, iconRes = R.drawable.ic_build)
    object SshClient : Screen("ssh_client?ip={ip}", R.string.eng_ssh, iconRes = R.drawable.ic_terminal)
    object TelnetClient : Screen("telnet_client", R.string.eng_telnet, iconRes = R.drawable.ic_terminal)
    object SftpClient : Screen("sftp_client", R.string.eng_sftp, iconRes = R.drawable.ic_file_upload)
    object TftpClient : Screen("tftp_client", R.string.eng_tftp, iconRes = R.drawable.ic_folder)
    object SerialConsole : Screen("serial_console", R.string.eng_serial, iconRes = R.drawable.ic_usb)

    object WifiDetail : Screen("wifi_detail/{bssid}", R.string.wifi_detail, iconRes = R.drawable.ic_radar)
    object CellularDetail : Screen("cellular_detail", R.string.cellular_detail, iconRes = R.drawable.ic_signal_cellular)
    object History : Screen("history", R.string.nav_report, iconRes = R.drawable.ic_analytics)
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Diagnostic,
    Screen.Scanner,
    Screen.Utilities,
    Screen.Security
)
