package com.herwin.netez.data

data class UserGuideItem(
    val id: String,
    val titleEn: String,
    val titleId: String,
    val category: String, // "Dashboard", "Diagnostic", "Scanner", "Utilities", "Security", "Tools"
    val iconName: String,
    val functionEn: String,
    val functionId: String,
    val stepsEn: List<String>,
    val stepsId: List<String>,
    val route: String? = null
)

object UserGuideProvider {
    fun getGuideItems(): List<UserGuideItem> {
        return listOf(
            // 1. Dashboard
            UserGuideItem(
                id = "dashboard",
                titleEn = "Dashboard Overview",
                titleId = "Ikhtisar Dashboard",
                category = "Dashboard",
                iconName = "home",
                functionEn = "Monitors real-time network status (Wi-Fi/Cellular), IP details, ISP, signal strength, data usage traffic graph, and overall security score.",
                functionId = "Memantau status jaringan real-time (Wi-Fi/Seluler), detail IP, ISP, kekuatan sinyal, grafik traffic data, serta skor ringkasan keamanan.",
                stepsEn = listOf(
                    "Open app to automatically view current connection details.",
                    "Tap on Wi-Fi or Cellular cards to open detailed network specs.",
                    "Observe real-time upload/download traffic in the usage chart.",
                    "Use Quick Action shortcuts at the bottom for instant testing."
                ),
                stepsId = listOf(
                    "Buka aplikasi untuk melihat detail koneksi saat ini secara otomatis.",
                    "Ketuk kartu Wi-Fi atau Seluler untuk membuka spesifikasi detail jaringan.",
                    "Amati grafik traffic data upload/download secara real-time.",
                    "Gunakan pintasan Quick Action di bagian bawah untuk pengujian instan."
                ),
                route = "home"
            ),

            // 2. Diagnostic
            UserGuideItem(
                id = "ping_test",
                titleEn = "Ping Test",
                titleId = "Ping Test",
                category = "Diagnostic",
                iconName = "networkCheck",
                functionEn = "Measures network latency (ms), packet loss percentage, and connection stability to target host or IP.",
                functionId = "Menguji latensi jaringan (ms), persentase packet loss, dan stabilitas koneksi ke host atau IP target.",
                stepsEn = listOf(
                    "Enter domain or IP address (e.g., google.com).",
                    "Adjust packet count and interval if needed.",
                    "Tap START PING to initiate ICMP packet test.",
                    "View min/avg/max latency and packet loss summary."
                ),
                stepsId = listOf(
                    "Masukkan nama domain atau alamat IP (misal: google.com).",
                    "Atur jumlah paket dan interval jika diperlukan.",
                    "Ketuk START PING untuk memulai pengujian paket ICMP.",
                    "Lihat ringkasan latensi min/avg/max dan packet loss."
                ),
                route = "ping_test?ip="
            ),
            UserGuideItem(
                id = "traceroute",
                titleEn = "Traceroute",
                titleId = "Traceroute",
                category = "Diagnostic",
                iconName = "route",
                functionEn = "Traces intermediate network router hops from your device to destination server.",
                functionId = "Melacak rute rintisan (hop) router jaringan yang dilewati data dari perangkat ke server tujuan.",
                stepsEn = listOf(
                    "Enter target domain or IP.",
                    "Tap START TRACE to begin hop discovery.",
                    "Inspect hop numbers, IP addresses, RTT latency, and hostnames."
                ),
                stepsId = listOf(
                    "Masukkan domain atau IP target.",
                    "Ketuk START TRACE untuk memulai pencarian rute.",
                    "Inspeksi nomor hop, alamat IP, latensi RTT, dan nama host."
                ),
                route = "traceroute?host="
            ),
            UserGuideItem(
                id = "dns_lookup",
                titleEn = "DNS Lookup",
                titleId = "DNS Lookup",
                category = "Diagnostic",
                iconName = "dns",
                functionEn = "Queries DNS records (A, AAAA, MX, TXT, NS, CNAME, SOA, PTR) using secure DoH resolvers (Cloudflare, Google, Quad9).",
                functionId = "Melakukan kueri record DNS (A, AAAA, MX, TXT, NS, CNAME, SOA, PTR) menggunakan DoH aman (Cloudflare, Google, Quad9).",
                stepsEn = listOf(
                    "Enter domain name (e.g., wikipedia.org).",
                    "Select DNS record type or choose ALL.",
                    "Select preferred DoH provider.",
                    "Tap SEARCH to view DNS answers and resolved IPs."
                ),
                stepsId = listOf(
                    "Masukkan nama domain (misal: wikipedia.org).",
                    "Pilih jenis record DNS atau pilih ALL.",
                    "Pilih penyedia DoH yang diinginkan.",
                    "Ketuk SEARCH untuk melihat respon DNS dan IP yang terurai."
                ),
                route = "dns_lookup"
            ),
            UserGuideItem(
                id = "speed_test",
                titleEn = "Speed Test",
                titleId = "Speed Test",
                category = "Diagnostic",
                iconName = "speed",
                functionEn = "Measures real-time download speed, upload speed, latency ping, and jitter.",
                functionId = "Mengukur kecepatan download, upload, latensi ping, dan jitter secara real-time.",
                stepsEn = listOf(
                    "Tap START TEST button.",
                    "Wait for download measurement to complete.",
                    "Wait for upload measurement to complete.",
                    "Review total bandwidth results and connection quality."
                ),
                stepsId = listOf(
                    "Ketuk tombol START TEST.",
                    "Tunggu hingga pengujian kecepatan download selesai.",
                    "Tunggu hingga pengujian kecepatan upload selesai.",
                    "Tinjau hasil bandwidth dan kualitas koneksi."
                ),
                route = "speed_test"
            ),
            UserGuideItem(
                id = "stability_test",
                titleEn = "Stability Test",
                titleId = "Test Stabilitas",
                category = "Diagnostic",
                iconName = "networkCheck",
                functionEn = "Long-duration ping testing (1m, 5m, 15m, 1h) to detect latency spikes, jitter, and connection drops.",
                functionId = "Pengujian ping durasi panjang (1m, 5m, 15m, 1h) untuk mendeteksi spike latensi, jitter, dan putus koneksi.",
                stepsEn = listOf(
                    "Enter target host/IP.",
                    "Select test duration (e.g., 5 mins).",
                    "Tap START STABILITY TEST.",
                    "Observe latency trend graph and stability rating."
                ),
                stepsId = listOf(
                    "Masukkan host/IP target.",
                    "Pilih durasi tes (misal: 5 menit).",
                    "Ketuk START STABILITY TEST.",
                    "Amati grafik tren latensi dan peringkat stabilitas."
                ),
                route = "stability_test"
            ),
            UserGuideItem(
                id = "http_check",
                titleEn = "HTTP Check",
                titleId = "HTTP Check",
                category = "Diagnostic",
                iconName = "language",
                functionEn = "Inspects HTTP response status codes, response time, server headers, and SSL status.",
                functionId = "Memeriksa status respon HTTP, waktu respon web, header server, dan status SSL.",
                stepsEn = listOf(
                    "Enter website URL (e.g., https://example.com).",
                    "Choose method (GET/HEAD).",
                    "Tap CHECK WEBSITE.",
                    "Review status code (200 OK, 404, etc.) and response headers."
                ),
                stepsId = listOf(
                    "Masukkan URL situs (misal: https://example.com).",
                    "Pilih metode (GET/HEAD).",
                    "Ketuk CHECK WEBSITE.",
                    "Tinjau kode status (200 OK, 404, dll) dan header respon."
                ),
                route = "http_check"
            ),
            UserGuideItem(
                id = "isp_comparison",
                titleEn = "ISP Comparison",
                titleId = "Komparasi ISP",
                category = "Diagnostic",
                iconName = "compare",
                functionEn = "Compares ISP performance ratings, average speed, latency, and user reviews.",
                functionId = "Membandingkan peringkat performa, kecepatan rata-rata, latensi, dan ulasan ISP.",
                stepsEn = listOf(
                    "Search or select an ISP from the list.",
                    "View performance score and community ratings.",
                    "Submit your own rating or review for your ISP."
                ),
                stepsId = listOf(
                    "Cari atau pilih ISP dari daftar.",
                    "Lihat skor performa dan ulasan komunitas.",
                    "Kirim rating atau ulasan Anda untuk ISP Anda."
                ),
                route = "isp_comparison"
            ),
            UserGuideItem(
                id = "ssl_checker",
                titleEn = "SSL/TLS Checker",
                titleId = "Pemeriksa SSL/TLS",
                category = "Diagnostic",
                iconName = "lock",
                functionEn = "Inspects SSL certificate details, validity timeline, issuer, and signature algorithm.",
                functionId = "Menginspeksi detail sertifikat SSL, masa berlaku, penerbit, dan algoritma tanda tangan.",
                stepsEn = listOf(
                    "Enter hostname and port (default 443).",
                    "Tap CHECK.",
                    "Examine validity days left, issuer, and trust chain."
                ),
                stepsId = listOf(
                    "Masukkan nama host dan port (default 443).",
                    "Ketuk CHECK.",
                    "Periksa sisa hari berlaku, penerbit, dan rantai kepercayaan."
                ),
                route = "ssl_checker?host="
            ),
            UserGuideItem(
                id = "blacklist_check",
                titleEn = "Blacklist Check",
                titleId = "Cek Daftar Hitam (Blacklist)",
                category = "Diagnostic",
                iconName = "security",
                functionEn = "Checks if your public IP or domain is listed in major spam blacklists (Spamhaus, Spamcop, AbuseIPDB, Shodan).",
                functionId = "Memeriksa apakah IP publik atau domain Anda terdaftar di daftar hitam spam (Spamhaus, Spamcop, AbuseIPDB, Shodan).",
                stepsEn = listOf(
                    "Enter public IP address or domain.",
                    "Tap CHECK BLACKLIST.",
                    "Review list of DNSBL databases and status (CLEAN / LISTED)."
                ),
                stepsId = listOf(
                    "Masukkan alamat IP publik atau domain.",
                    "Ketuk CHECK BLACKLIST.",
                    "Tinjau daftar database DNSBL dan statusnya (CLEAN / LISTED)."
                ),
                route = "blacklist_check"
            ),

            // 3. Scanner
            UserGuideItem(
                id = "lan_scanner",
                titleEn = "LAN Scanner",
                titleId = "Pemindai Jaringan LAN",
                category = "Scanner",
                iconName = "lan",
                functionEn = "Scans all active connected devices on your Wi-Fi network (IP, MAC Address, OUI Vendor, Hostname, Open Ports).",
                functionId = "Memindai seluruh perangkat aktif yang terhubung di jaringan Wi-Fi lokal (IP, MAC Address, Vendor OUI, Hostname, Port Terbuka).",
                stepsEn = listOf(
                    "Connect to your Wi-Fi network.",
                    "Tap SCAN NETWORK.",
                    "View discovered devices, vendor names, and ping response times.",
                    "Tap on a device to inspect open ports or perform WoL."
                ),
                stepsId = listOf(
                    "Hubungkan ke jaringan Wi-Fi Anda.",
                    "Ketuk SCAN NETWORK.",
                    "Lihat perangkat yang ditemukan, nama vendor, dan waktu respon ping.",
                    "Ketuk perangkat untuk memeriksa port terbuka atau lakukan WoL."
                ),
                route = "lan_scanner"
            ),
            UserGuideItem(
                id = "port_scanner",
                titleEn = "Port Scanner",
                titleId = "Pemindai Port TCP",
                category = "Scanner",
                iconName = "build",
                functionEn = "Scans open TCP ports on a target IP address (e.g., 22 SSH, 80 HTTP, 443 HTTPS, 3389 RDP).",
                functionId = "Memindai port TCP terbuka pada IP target (misal: 22 SSH, 80 HTTP, 443 HTTPS, 3389 RDP).",
                stepsEn = listOf(
                    "Enter target IP address.",
                    "Choose port range (Top 100 ports or custom range).",
                    "Tap SCAN PORTS.",
                    "View open ports and identified service names."
                ),
                stepsId = listOf(
                    "Masukkan alamat IP target.",
                    "Pilih rentang port (Top 100 port atau rentang kustom).",
                    "Ketuk SCAN PORTS.",
                    "Lihat port terbuka dan nama layanan yang teridentifikasi."
                ),
                route = "port_scanner?ip="
            ),
            UserGuideItem(
                id = "wifi_analyzer",
                titleEn = "WiFi Analyzer",
                titleId = "Pemindai Wi-Fi",
                category = "Scanner",
                iconName = "wifiAnalyzer",
                functionEn = "Scans surrounding Wi-Fi access points (SSID, BSSID, dBm signal strength, Channel, WPA2/WPA3 encryption).",
                functionId = "Memindai access point Wi-Fi di sekitar (SSID, BSSID, kekuatan sinyal dBm, Channel, enkripsi WPA2/WPA3).",
                stepsEn = listOf(
                    "Ensure Location & Wi-Fi are enabled.",
                    "Open screen to view surrounding Wi-Fi networks.",
                    "Inspect signal strength in dBm and security types."
                ),
                stepsId = listOf(
                    "Pastikan Lokasi & Wi-Fi aktif.",
                    "Buka layar untuk melihat jaringan Wi-Fi di sekitar.",
                    "Inspeksi kekuatan sinyal dalam dBm dan tipe keamanan."
                ),
                route = "wifi_analyzer"
            ),
            UserGuideItem(
                id = "bluetooth_scanner",
                titleEn = "Bluetooth BLE Scanner",
                titleId = "Pemindai Bluetooth BLE",
                category = "Scanner",
                iconName = "bluetooth",
                functionEn = "Discovers nearby Bluetooth & Bluetooth Low Energy (BLE) devices, RSSI signal strength, and device types.",
                functionId = "Menemukan perangkat Bluetooth & BLE terdekat, kekuatan sinyal RSSI, dan tipe perangkat.",
                stepsEn = listOf(
                    "Ensure Bluetooth is enabled.",
                    "Tap START SCAN.",
                    "View discovered BLE beacons and smart devices."
                ),
                stepsId = listOf(
                    "Pastikan Bluetooth aktif.",
                    "Ketuk START SCAN.",
                    "Lihat beacon BLE dan perangkat pintar yang terdeteksi."
                ),
                route = "bluetooth_scanner"
            ),
            UserGuideItem(
                id = "mdns_scanner",
                titleEn = "mDNS / Bonjour Scanner",
                titleId = "Pemindai mDNS / Bonjour",
                category = "Scanner",
                iconName = "search",
                functionEn = "Discovers local ZeroConfig / Bonjour / AirPlay / Chromecast / Printer services.",
                functionId = "Menemukan layanan lokal ZeroConfig / Bonjour / AirPlay / Chromecast / Printer.",
                stepsEn = listOf(
                    "Tap DISCOVER SERVICES.",
                    "Inspect local mDNS services published on the Wi-Fi network."
                ),
                stepsId = listOf(
                    "Ketuk DISCOVER SERVICES.",
                    "Inspeksi layanan mDNS lokal yang dipublikasikan di jaringan Wi-Fi."
                ),
                route = "mdns_scanner"
            ),
            UserGuideItem(
                id = "channel_graph",
                titleEn = "Wi-Fi Channel Graph",
                titleId = "Grafik Saluran Wi-Fi",
                category = "Scanner",
                iconName = "channelGraph",
                functionEn = "Visualizes 2.4GHz & 5GHz Wi-Fi channel interference to select optimal router channel.",
                functionId = "Menampilkan grafik interferensi saluran Wi-Fi 2.4GHz & 5GHz untuk memilih saluran router terbaik.",
                stepsEn = listOf(
                    "Select band (2.4GHz or 5GHz).",
                    "Observe Wi-Fi signal overlap curves across channels.",
                    "Identify least congested channel for router configuration."
                ),
                stepsId = listOf(
                    "Pilih frekuensi (2.4GHz atau 5GHz).",
                    "Amati kurva tumpang tindih sinyal Wi-Fi di seluruh saluran.",
                    "Identifikasi saluran yang paling sepi untuk konfigurasi router."
                ),
                route = "channel_graph"
            ),
            UserGuideItem(
                id = "upnp_scanner",
                titleEn = "UPnP Scanner",
                titleId = "Pemindai UPnP",
                category = "Scanner",
                iconName = "router",
                functionEn = "Discovers UPnP (Universal Plug and Play) devices like Routers, Smart TVs, NAS, and IP Cameras.",
                functionId = "Menemukan perangkat UPnP seperti Router, Smart TV, NAS, dan IP Camera.",
                stepsEn = listOf(
                    "Tap SCAN UPNP.",
                    "View SSDP response headers, device model names, and control URLs."
                ),
                stepsId = listOf(
                    "Ketuk SCAN UPNP.",
                    "Lihat header respon SSDP, nama model perangkat, dan URL kontrol."
                ),
                route = "upnp_scanner"
            ),
            UserGuideItem(
                id = "snmp_scanner",
                titleEn = "SNMP Scanner",
                titleId = "Pemindai SNMP",
                category = "Scanner",
                iconName = "memory",
                functionEn = "Scans network devices supporting SNMP protocol using community strings (default: public).",
                functionId = "Memindai perangkat jaringan yang mendukung protokol SNMP menggunakan community string (default: public).",
                stepsEn = listOf(
                    "Enter target subnet or IP.",
                    "Enter community string (default: public).",
                    "Tap SCAN SNMP to discover managed network hardware."
                ),
                stepsId = listOf(
                    "Masukkan subnet atau IP target.",
                    "Masukkan community string (default: public).",
                    "Ketuk SCAN SNMP untuk menemukan perangkat keras terkelola."
                ),
                route = "snmp_scanner"
            ),

            // 4. Utilities
            UserGuideItem(
                id = "ip_calculator",
                titleEn = "IP Subnet Calculator",
                titleId = "Kalkulator Subnet IP",
                category = "Utilities",
                iconName = "calculate",
                functionEn = "Calculates Subnet Mask, Network Address, Broadcast, Wildcard, First/Last Host, & Total Hosts from CIDR notation.",
                functionId = "Menghitung Subnet Mask, Network Address, Broadcast, Wildcard, Host Pertama/Terakhir, & Total Host dari CIDR.",
                stepsEn = listOf(
                    "Enter IP and CIDR prefix (e.g., 192.168.1.1/24).",
                    "Instantly review network range and usable IP count."
                ),
                stepsId = listOf(
                    "Masukkan IP dan notasi CIDR (misal: 192.168.1.1/24).",
                    "Hasil rentang jaringan dan jumlah IP yang dapat digunakan langsung muncul."
                ),
                route = "ip_calculator"
            ),
            UserGuideItem(
                id = "quick_notes",
                titleEn = "Quick Notes",
                titleId = "Catatan Cepat (Quick Notes)",
                category = "Utilities",
                iconName = "stickyNote",
                functionEn = "Secure local notepad to save IP addresses, domains, test outputs, or technical notes.",
                functionId = "Buku catatan lokal aman untuk menyimpan alamat IP, domain, hasil tes, atau catatan teknis.",
                stepsEn = listOf(
                    "Tap + Add Note.",
                    "Enter title, body content, and select tag/type.",
                    "Tap Save to store note locally."
                ),
                stepsId = listOf(
                    "Ketuk + Add Note.",
                    "Isi judul, isi catatan, dan pilih tag/tipe.",
                    "Ketuk Save untuk menyimpan catatan secara lokal."
                ),
                route = "quick_notes"
            ),
            UserGuideItem(
                id = "whois_info",
                titleEn = "WHOIS & IP Geolocation",
                titleId = "WHOIS & Geolokasi IP",
                category = "Utilities",
                iconName = "info",
                functionEn = "Queries domain registration details (RDAP) and IP Geolocation (ISP, Org, City, Country, AS Number).",
                functionId = "Kueri detail pendaftaran domain (RDAP) dan Geolokasi IP (ISP, Organisasi, Kota, Negara, Nomor AS).",
                stepsEn = listOf(
                    "Enter IP address or domain name.",
                    "Tap SEARCH.",
                    "View registrar, expiration date, geolocation map coordinates, and ASN."
                ),
                stepsId = listOf(
                    "Masukkan alamat IP atau nama domain.",
                    "Ketuk SEARCH.",
                    "Lihat registrar, tanggal kedaluwarsa, koordinat geolokasi, dan ASN."
                ),
                route = "whois_info?host="
            ),
            UserGuideItem(
                id = "http_header",
                titleEn = "HTTP Header Checker",
                titleId = "Pemeriksa HTTP Header",
                category = "Utilities",
                iconName = "httpHeader",
                functionEn = "Inspects server HTTP response headers (Server, Content-Type, HSTS, CSP, Cookies).",
                functionId = "Memeriksa header respon HTTP server (Server, Content-Type, HSTS, CSP, Cookies).",
                stepsEn = listOf(
                    "Enter website URL.",
                    "Tap FETCH HEADERS.",
                    "Review list of response headers and security directives."
                ),
                stepsId = listOf(
                    "Masukkan URL situs.",
                    "Ketuk FETCH HEADERS.",
                    "Tinjau daftar header respon dan direktif keamanan."
                ),
                route = "http_header"
            ),
            UserGuideItem(
                id = "http_lab",
                titleEn = "API Tester / HTTP Lab",
                titleId = "API Tester / HTTP Lab",
                category = "Utilities",
                iconName = "rocket",
                functionEn = "Full-featured HTTP client to test API endpoints (GET, POST, PUT, DELETE) with custom headers, body, and auth tokens.",
                functionId = "Klien HTTP lengkap untuk menguji endpoint API (GET, POST, PUT, DELETE) dengan custom header, body, dan auth token.",
                stepsEn = listOf(
                    "Enter API endpoint URL.",
                    "Select HTTP method (GET, POST, etc.).",
                    "Add Headers, Query Params, or JSON Body.",
                    "Tap SEND REQUEST to inspect status, response body, and timing."
                ),
                stepsId = listOf(
                    "Masukkan URL endpoint API.",
                    "Pilih metode HTTP (GET, POST, dll).",
                    "Tambahkan Header, Query Params, atau JSON Body.",
                    "Ketuk SEND REQUEST untuk memeriksa status, isi respon, dan latensi."
                ),
                route = "http_lab?url="
            ),
            UserGuideItem(
                id = "wake_on_lan",
                titleEn = "Wake on LAN (WoL)",
                titleId = "Wake on LAN (WoL)",
                category = "Utilities",
                iconName = "power",
                functionEn = "Remotely powers on target computers/servers on your local network using Magic Packet.",
                functionId = "Membangunkan komputer/server target dari jarak jauh di jaringan lokal menggunakan Magic Packet.",
                stepsEn = listOf(
                    "Enter target computer MAC address (e.g., AA:BB:CC:DD:EE:FF).",
                    "Enter port (default 9).",
                    "Tap WAKE UP to send WoL packet."
                ),
                stepsId = listOf(
                    "Masukkan MAC address komputer target (misal: AA:BB:CC:DD:EE:FF).",
                    "Masukkan port (default 9).",
                    "Ketuk WAKE UP untuk mengirim paket WoL."
                ),
                route = "wol"
            ),
            UserGuideItem(
                id = "monitoring_lab",
                titleEn = "Monitoring Lab",
                titleId = "Monitoring Lab",
                category = "Utilities",
                iconName = "monitor",
                functionEn = "Background server monitoring (PING, HTTP, PORT) with automated background checks and alerts when target is down.",
                functionId = "Pemantauan server latar belakang (PING, HTTP, PORT) dengan pindaian otomatis dan notifikasi jika server down.",
                stepsEn = listOf(
                    "Tap + Add Target.",
                    "Select monitoring type (PING, HTTP, PORT), host, and check interval.",
                    "Save target to begin background monitoring and view uptime SLA graph."
                ),
                stepsId = listOf(
                    "Ketuk + Add Target.",
                    "Pilih tipe pemantauan (PING, HTTP, PORT), host, dan interval pindaian.",
                    "Simpan target untuk memulai pemantauan latar belakang dan lihat grafik uptime SLA."
                ),
                route = "monitoring_lab"
            ),
            UserGuideItem(
                id = "eng_tools",
                titleEn = "Engineering Remote Tools",
                titleId = "Engineering Remote Tools",
                category = "Utilities",
                iconName = "build",
                functionEn = "Remote administration tools including SSH Client, Telnet Client, SFTP Client, TFTP Client, and Serial USB OTG Console.",
                functionId = "Alat administrasi jarak jauh termasuk SSH Client, Telnet Client, SFTP Client, TFTP Client, dan Serial USB OTG Console.",
                stepsEn = listOf(
                    "Select remote protocol (SSH, Telnet, SFTP, TFTP, Serial).",
                    "Enter host IP, port, username, and password/key.",
                    "Tap Connect to open interactive terminal session or file transfer."
                ),
                stepsId = listOf(
                    "Pilih protokol remote (SSH, Telnet, SFTP, TFTP, Serial).",
                    "Masukkan IP host, port, username, dan password/key.",
                    "Ketuk Connect untuk membuka sesi terminal interaktif atau transfer file."
                ),
                route = "eng_tools"
            ),

            // 5. Security & Security Tools
            UserGuideItem(
                id = "device_security_check",
                titleEn = "Device Security Check",
                titleId = "Audit Keamanan Perangkat",
                category = "Security",
                iconName = "shieldCheck",
                functionEn = "Audits Android OS security state (Root access, Lock screen, USB Debugging, Device Encryption, Developer Options, Play Protect).",
                functionId = "Mengaudit status keamanan OS Android (Akses Root, Kunci Layar, USB Debugging, Enkripsi Perangkat, Opsi Pengembang, Play Protect).",
                stepsEn = listOf(
                    "Tap SCAN NOW.",
                    "Review overall Security Score (out of 100).",
                    "Inspect individual pass/warning/critical audit checks."
                ),
                stepsId = listOf(
                    "Ketuk SCAN NOW.",
                    "Tinjau Skor Keamanan keseluruhan (dari 100).",
                    "Inspeksi hasil pemeriksaan aman/peringatan/kritis."
                ),
                route = "device_security_check"
            ),
            UserGuideItem(
                id = "wifi_security_audit",
                titleEn = "Wi-Fi Security Audit",
                titleId = "Audit Keamanan Wi-Fi",
                category = "Security",
                iconName = "wifiShield",
                functionEn = "Audits Wi-Fi security parameters (WPA2/WPA3 encryption, WPS vulnerabilities, PMF, Router admin exposure, Rogue APs).",
                functionId = "Mengaudit parameter keamanan Wi-Fi (Enkripsi WPA2/WPA3, kerentanan WPS, PMF, eksposur Admin Router, Rogue AP).",
                stepsEn = listOf(
                    "Ensure connected to Wi-Fi network.",
                    "Tap AUDIT WIFI.",
                    "Review Risk Score and priority recommendations to fix Wi-Fi vulnerabilities."
                ),
                stepsId = listOf(
                    "Pastikan terhubung ke jaringan Wi-Fi.",
                    "Ketuk AUDIT WIFI.",
                    "Tinjau Skor Risiko dan rekomendasi prioritas untuk memperbaiki kerentanan Wi-Fi."
                ),
                route = "wifi_security_audit"
            ),
            UserGuideItem(
                id = "dns_leak_test",
                titleEn = "DNS Leak Test",
                titleId = "Tes DNS Leak",
                category = "Security",
                iconName = "dns",
                functionEn = "Verifies whether DNS queries leak to your ISP while connected to a VPN.",
                functionId = "Memeriksa apakah kueri DNS Anda bocor ke ISP saat menggunakan VPN.",
                stepsEn = listOf(
                    "Enable your VPN.",
                    "Tap TEST NOW.",
                    "Review resolved DNS servers and ISP visibility status (SAFE / LEAKED)."
                ),
                stepsId = listOf(
                    "Aktifkan VPN Anda.",
                    "Ketuk TEST NOW.",
                    "Tinjau server DNS terurai dan status transparansi ISP (SAFE / LEAKED)."
                ),
                route = "dns_leak_test"
            ),
            UserGuideItem(
                id = "email_leak_check",
                titleEn = "Email Leak Check",
                titleId = "Pemeriksa Kebocoran Email",
                category = "Security",
                iconName = "shield",
                functionEn = "Checks if email addresses have been involved in known data breaches using XposedOrNot database.",
                functionId = "Memeriksa apakah alamat email pernah terlibat dalam kebocoran data menggunakan database XposedOrNot.",
                stepsEn = listOf(
                    "Enter email address or import CSV list.",
                    "Tap CHECK LEAKS.",
                    "Review breached database names, risk scores, and compromised data types."
                ),
                stepsId = listOf(
                    "Masukkan alamat email atau impor daftar CSV.",
                    "Ketuk CHECK LEAKS.",
                    "Tinjau nama database yang bocor, skor risiko, dan jenis data yang bocor."
                ),
                route = "leak_check"
            ),
            UserGuideItem(
                id = "security_intelligence",
                titleEn = "Security Intelligence",
                titleId = "Security Intelligence",
                category = "Security",
                iconName = "security",
                functionEn = "Unified security assessment that automatically classifies Domain, URL, or Email inputs and executes parallel multi-vector security checks (Reputation, Blacklist, SSL/TLS, DNS Security, Phishing, Exposure, and Data Leaks). Generates a complete report with findings, impact, actionable recommendations, and exportable PDF report.",
                functionId = "Evaluasi keamanan terpadu yang secara otomatis mengidentifikasi input Domain, URL, atau Email dan menjalankan analisis keamanan paralel (Reputasi, Blacklist, SSL/TLS, Keamanan DNS, Indikator Phishing, Eksposur, dan Kebocoran Data). Menghasilkan laporan lengkap berisi temuan, dampak, rekomendasi perbaikan, serta ekspor PDF resmi.",
                stepsEn = listOf(
                    "Enter target domain (e.g. example.com), URL (e.g. https://example.com/login), or email address.",
                    "Tap SCAN to launch asynchronous multi-vector security analysis.",
                    "Observe live real-time check progress and status indicators.",
                    "Review Assessment Summary, Category Status Cards, Security Findings with Actionable Recommendations, and Passed Checks.",
                    "Tap PDF REPORT to export a full professional PDF report or Share Summary with colleagues."
                ),
                stepsId = listOf(
                    "Masukkan domain target (misal example.com), URL (misal https://example.com/login), atau alamat email.",
                    "Ketuk SCAN untuk memulai analisis keamanan paralel otomatis.",
                    "Amati kemajuan pemindaian live dan indikator status per kategori secara real-time.",
                    "Tinjau Ringkasan Assessment, Kartu Status Kategori, Temuan Keamanan lengkap dengan Rekomendasi Perbaikan, dan Poin Lolos.",
                    "Ketuk LAPORAN PDF untuk mengunduh dokumen PDF resmi atau Bagikan Ringkasan Laporan."
                ),
                route = "security_intelligence"
            ),
            UserGuideItem(
                id = "password_strength",
                titleEn = "Password Strength Evaluator",
                titleId = "Evaluator Kekuatan Password",
                category = "Security",
                iconName = "key",
                functionEn = "Evaluates password entropy, character complexity, and checks against HIBP Pwned Passwords database without uploading raw password.",
                functionId = "Mengevaluasi entropi password, variasi karakter, dan mengecek ke database HIBP Pwned Passwords tanpa mengunggah password asli.",
                stepsEn = listOf(
                    "Type a password into the input field.",
                    "Observe strength meter score, entropy bits, and estimated crack time.",
                    "Check Pwned status to see if password appeared in data breaches."
                ),
                stepsId = listOf(
                    "Ketikkan password di kolom input.",
                    "Amati skor indikator kekuatan, bit entropi, dan estimasi waktu peretasan.",
                    "Cek status Pwned untuk melihat apakah password pernah bocor."
                ),
                route = "password_strength"
            ),
            UserGuideItem(
                id = "password_manager",
                titleEn = "Credential Vault",
                titleId = "Brankas Kredensial",
                category = "Security",
                iconName = "lock",
                functionEn = "Encrypted 100% local vault to store passwords, server credentials, SSH keys, SFTP accounts, API secrets, database credentials, network devices, and secure notes using AES-256-GCM encryption protected by Android Keystore and Biometric Fingerprint/Face authentication.",
                functionId = "Brankas lokal terenkripsi 100% untuk menyimpan kata sandi, kredensial server, kunci SSH, akun SFTP, API secret, database, perangkat jaringan, dan catatan rahasia menggunakan enkripsi AES-256-GCM yang dilindungi Android Keystore dan Biometrik Sidik Jari/Wajah.",
                stepsEn = listOf(
                    "Authenticate via Fingerprint/Face or Device Security.",
                    "Tap + Add Credential and select Type (Website, Server, SSH, SFTP, API, Database, Network Device, Secure Note, Custom).",
                    "For SSH/SFTP credentials, import SSH Private Keys (.pem/.key) or enter passwords.",
                    "Tap CONNECT USING NETEZ on saved SSH/SFTP credentials to directly launch NeTez terminal tools with pre-filled credentials.",
                    "Copy passwords or secrets safely with 30-second automatic clipboard clearing."
                ),
                stepsId = listOf(
                    "Autentikasi via Sidik Jari/Wajah atau Keamanan Perangkat.",
                    "Ketuk + Add Credential dan pilih Tipe (Situs, Server, SSH, SFTP, API, Database, Perangkat Jaringan, Catatan Rahasia, Custom).",
                    "Untuk kredensial SSH/SFTP, impor SSH Private Key (.pem/.key) atau masukkan kata sandi.",
                    "Ketuk HUBUNGKAN LEWAT NETEZ pada kredensial SSH/SFTP untuk langsung membuka terminal NeTez dengan kredensial terisi otomatis.",
                    "Salin kata sandi atau secret secara aman dengan pembersihan otomatis clipboard dalam 30 detik."
                ),
                route = "password_manager"
            ),
            UserGuideItem(
                id = "ssl_reminder",
                titleEn = "SSL/TLS Monitor",
                titleId = "SSL/TLS Monitor",
                category = "Security",
                iconName = "timer",
                functionEn = "Monitors domain SSL certificate expiration (My Domains) and performs instant security audits (Instant Check: Grade A+/A/B/C/F, TLS 1.3/1.2, Ciphers, Chain, Vulns).",
                functionId = "Memantau masa berlaku sertifikat SSL domain (My Domains) dan melakukan pindaian keamanan instan (Instant Check: Grade A+/A/B/C/F, TLS 1.3/1.2, Cipher, Chain, Kerentanan).",
                stepsEn = listOf(
                    "Tab My Domains: Add domains to get daily expiry & Grade F background alerts.",
                    "Tab Instant Check: Enter domain URL and tap CHECK NOW.",
                    "Review Security Grade, TLS versions, active cipher, trust chain, and vulnerability report."
                ),
                stepsId = listOf(
                    "Tab My Domains: Tambahkan domain untuk mendapatkan peringatan kedaluwarsa & Grade F di latar belakang.",
                    "Tab Instant Check: Masukkan URL domain dan ketuk CEK SEKARANG.",
                    "Tinjau Grade Keamanan, versi TLS, cipher aktif, rantai kepercayaan, dan laporan kerentanan."
                ),
                route = "ssl_reminder"
            ),
            UserGuideItem(
                id = "phishing_check",
                titleEn = "Phishing Check",
                titleId = "Pemeriksa Phishing",
                category = "Security",
                iconName = "public",
                functionEn = "Inspects suspicious links against URLhaus (200k+ malware DB), OpenPhish, PhishStats, and Heuristic pattern rules.",
                functionId = "Memeriksa tautan mencurigakan dari database URLhaus (200rb+ database malware), OpenPhish, PhishStats, dan aturan pola Heuristik.",
                stepsEn = listOf(
                    "Paste suspicious URL into input field.",
                    "Tap CHECK PHISHING.",
                    "Review risk score and detailed verification layers."
                ),
                stepsId = listOf(
                    "Tempelkan URL mencurigakan ke kolom input.",
                    "Ketuk CHECK PHISHING.",
                    "Tinjau skor risiko dan lapisan verifikasi detail."
                ),
                route = "phishing_check"
            ),
            UserGuideItem(
                id = "app_permission_scanner",
                titleEn = "App Permission Scanner",
                titleId = "Pemindai Izin Aplikasi",
                category = "Security",
                iconName = "security",
                functionEn = "Scans installed applications for dangerous permission combinations (SMS, Contacts, Camera, Location, Audio).",
                functionId = "Memindai aplikasi terinstal untuk mengidentifikasi kombinasi izin berbahaya (SMS, Kontak, Kamera, Lokasi, Audio).",
                stepsEn = listOf(
                    "Tap START SCAN.",
                    "Filter by Dangerous, Suspicious, or All apps.",
                    "Inspect requested permissions and uninstall or review app settings."
                ),
                stepsId = listOf(
                    "Ketuk START SCAN.",
                    "Filter berdasarkan aplikasi Berbahaya, Mencurigakan, atau Semua.",
                    "Inspeksi izin yang diminta dan uninstal atau buka pengaturan aplikasi."
                ),
                route = "app_permission_scanner"
            ),

            // 6. Security Tools Sub-menu Utilities
            UserGuideItem(
                id = "tool_hash",
                titleEn = "Hash Calculator",
                titleId = "Kalkulator Hash",
                category = "Tools",
                iconName = "key",
                functionEn = "Generates MD5, SHA-1, SHA-256, and SHA-512 cryptographic hashes for text or files.",
                functionId = "Menghasilkan hash kriptografi MD5, SHA-1, SHA-256, dan SHA-512 untuk teks atau berkas.",
                stepsEn = listOf(
                    "Enter text or tap Pick File to select a document.",
                    "Select algorithm chip (MD5, SHA-1, SHA-256, SHA-512).",
                    "Tap GENERATE HASH to view hex digest.",
                    "Copy, Share, or Save result to Quick Notes."
                ),
                stepsId = listOf(
                    "Masukkan teks atau ketuk Pick File untuk memilih dokumen.",
                    "Pilih algoritma (MD5, SHA-1, SHA-256, SHA-512).",
                    "Ketuk GENERATE HASH untuk melihat hasil digest.",
                    "Salin, Bagikan, atau Simpan hasil ke Quick Notes."
                ),
                route = "security_tool?toolType=HASH_CALCULATOR"
            ),
            UserGuideItem(
                id = "tool_base64",
                titleEn = "Base64 Encoder / Decoder",
                titleId = "Enkoder / Dekoder Base64",
                category = "Tools",
                iconName = "terminal",
                functionEn = "Encodes text/files into Base64 or decodes Base64 strings back to raw text with URL-Safe options.",
                functionId = "Mengenkode teks/berkas ke Base64 atau mendekode string Base64 kembali ke teks asli dengan pilihan URL-Safe.",
                stepsEn = listOf(
                    "Select mode (Encode or Decode).",
                    "Enter text or select a file.",
                    "Toggle URL-Safe option if needed.",
                    "Tap ENCODE / DECODE to process."
                ),
                stepsId = listOf(
                    "Pilih mode (Encode atau Decode).",
                    "Masukkan teks atau pilih berkas.",
                    "Aktifkan opsi URL-Safe jika diperlukan.",
                    "Ketuk ENCODE / DECODE untuk memproses."
                ),
                route = "security_tool?toolType=BASE64"
            ),
            UserGuideItem(
                id = "tool_url",
                titleEn = "URL Encoder / Decoder",
                titleId = "Enkoder / Dekoder URL",
                category = "Tools",
                iconName = "link",
                functionEn = "Encodes special characters in URL strings or decodes percent-encoded URL parameters.",
                functionId = "Mengenkode karakter khusus dalam string URL atau mendekode parameter URL terenkode persen.",
                stepsEn = listOf(
                    "Select Encode or Decode mode.",
                    "Enter URL string.",
                    "Tap ENCODE / DECODE."
                ),
                stepsId = listOf(
                    "Pilih mode Encode atau Decode.",
                    "Masukkan string URL.",
                    "Ketuk ENCODE / DECODE."
                ),
                route = "security_tool?toolType=URL_ENCODER"
            ),
            UserGuideItem(
                id = "tool_jwt",
                titleEn = "JWT Decoder",
                titleId = "Dekoder JWT",
                category = "Tools",
                iconName = "verifiedUser",
                functionEn = "Parses JSON Web Tokens (JWT) into formatted JSON Header & Payload, and evaluates token expiration status.",
                functionId = "Membedah JSON Web Token (JWT) menjadi JSON Header & Payload terformat, serta mengecek status masa berlaku token.",
                stepsEn = listOf(
                    "Paste 3-part JWT string (eyJhbGci...).",
                    "Tap DECODE JWT.",
                    "Inspect pretty-printed Header, Payload claims, and Expiry badge (VALID / EXPIRED)."
                ),
                stepsId = listOf(
                    "Tempelkan string JWT 3 bagian (eyJhbGci...).",
                    "Ketuk DECODE JWT.",
                    "Inspeksi Header, claim Payload, dan status masa berlaku (VALID / EXPIRED)."
                ),
                route = "security_tool?toolType=JWT_DECODER"
            ),
            UserGuideItem(
                id = "tool_uuid",
                titleEn = "UUID Generator",
                titleId = "Generator UUID",
                category = "Tools",
                iconName = "build",
                functionEn = "Generates v4 (Random) or v1 (Time-based) Universally Unique Identifiers.",
                functionId = "Menghasilkan Universally Unique Identifier v4 (Random) atau v1 (Time-based).",
                stepsEn = listOf(
                    "Select UUID version (v4 or v1).",
                    "Tap GENERATE UUID.",
                    "Copy generated UUID string."
                ),
                stepsId = listOf(
                    "Pilih versi UUID (v4 atau v1).",
                    "Ketuk GENERATE UUID.",
                    "Salin string UUID yang dihasilkan."
                ),
                route = "security_tool?toolType=UUID_GENERATOR"
            ),
            UserGuideItem(
                id = "tool_token",
                titleEn = "Random Token Generator",
                titleId = "Generator Token Acak",
                category = "Tools",
                iconName = "lock",
                functionEn = "Generates cryptographically secure random tokens with length slider (8-128), charsets (Alphanumeric, Hex, Numeric, Alpha, Symbols), and entropy rating.",
                functionId = "Menghasilkan token acak aman terenkripsi dengan slider panjang (8-128), charset (Alphanumeric, Hex, Numeric, Alpha, Symbols), dan rating entropi.",
                stepsEn = listOf(
                    "Adjust token length slider (e.g., 32 chars).",
                    "Select charset and toggle special symbols.",
                    "Tap GENERATE TOKEN.",
                    "Review token and calculated entropy bits."
                ),
                stepsId = listOf(
                    "Atur slider panjang token (misal: 32 karakter).",
                    "Pilih charset dan aktifkan simbol khusus jika perlu.",
                    "Ketuk GENERATE TOKEN.",
                    "Tinjau token dan bit entropi terhitung."
                ),
                route = "security_tool?toolType=RANDOM_TOKEN"
            ),
            UserGuideItem(
                id = "tool_hmac",
                titleEn = "HMAC Generator",
                titleId = "Generator HMAC",
                category = "Tools",
                iconName = "shield",
                functionEn = "Generates Hash-based Message Authentication Codes using secret key and HMAC algorithms (HmacSHA256, HmacSHA512, HmacSHA1, HmacMD5).",
                functionId = "Menghasilkan Kode Autentikasi Pesan Berbasis Hash menggunakan secret key dan algoritma HMAC (HmacSHA256, HmacSHA512, HmacSHA1, HmacMD5).",
                stepsEn = listOf(
                    "Enter text message.",
                    "Enter secret key.",
                    "Select HMAC algorithm.",
                    "Tap GENERATE HMAC to view hex signature."
                ),
                stepsId = listOf(
                    "Masukkan pesan teks.",
                    "Masukkan secret key.",
                    "Pilih algoritma HMAC.",
                    "Ketuk GENERATE HMAC untuk melihat tanda tangan hex."
                ),
                route = "security_tool?toolType=HMAC_GENERATOR"
            ),
            UserGuideItem(
                id = "tool_checksum",
                titleEn = "Checksum Calculator",
                titleId = "Kalkulator Checksum",
                category = "Tools",
                iconName = "checkCircle",
                functionEn = "Calculates file integrity verification checksums (MD5, SHA-256, CRC32, Adler32).",
                functionId = "Menghitung checksum verifikasi integritas berkas/teks (MD5, SHA-256, CRC32, Adler32).",
                stepsEn = listOf(
                    "Enter text or select a file.",
                    "Choose checksum type (CRC32, Adler32, SHA-256, MD5).",
                    "Tap GENERATE HASH / CALCULATE."
                ),
                stepsId = listOf(
                    "Masukkan teks atau pilih berkas.",
                    "Pilih jenis checksum (CRC32, Adler32, SHA-256, MD5).",
                    "Ketuk GENERATE HASH / CALCULATE."
                ),
                route = "security_tool?toolType=CHECKSUM"
            ),
            UserGuideItem(
                id = "tool_timestamp",
                titleEn = "Timestamp Converter",
                titleId = "Konverter Timestamp",
                category = "Tools",
                iconName = "schedule",
                functionEn = "Converts Unix epoch timestamps (seconds/millis) to ISO8601, UTC, Local time, and relative duration.",
                functionId = "Mengonversi timestamp Unix epoch (detik/milis) ke ISO8601, UTC, Waktu Lokal, dan durasi relatif.",
                stepsEn = listOf(
                    "Enter epoch timestamp or ISO date string (or tap Insert Current Time).",
                    "Tap CONVERT TIMESTAMP.",
                    "Review formatted UTC, Local time, and Relative duration."
                ),
                stepsId = listOf(
                    "Masukkan timestamp epoch atau string ISO (atau ketuk Insert Current Time).",
                    "Ketuk CONVERT TIMESTAMP.",
                    "Tinjau format UTC, Waktu lokal, dan Durasi relatif."
                ),
                route = "security_tool?toolType=TIMESTAMP"
            ),
            UserGuideItem(
                id = "tool_regex",
                titleEn = "Regex Tester",
                titleId = "Penguji Regex",
                category = "Tools",
                iconName = "search",
                functionEn = "Tests regular expression patterns against sample text with flag options (Case Insensitive, Multiline, DotAll) and group extraction.",
                functionId = "Menguji pola Regular Expression pada teks dengan pilihan flag (Case Insensitive, Multiline, DotAll) dan ekstraksi grup.",
                stepsEn = listOf(
                    "Enter Regex Pattern (e.g., \\d+).",
                    "Select Regex flags if needed.",
                    "Enter sample test text.",
                    "Tap TEST REGEX to inspect match count, indices, and captured groups."
                ),
                stepsId = listOf(
                    "Masukkan Pola Regex (misal: \\d+).",
                    "Pilih flag Regex jika diperlukan.",
                    "Masukkan teks sampel.",
                    "Ketuk TEST REGEX untuk memeriksa jumlah cocok, indeks, dan grup yang ditangkap."
                ),
                route = "security_tool?toolType=REGEX"
            )
        )
    }
}
