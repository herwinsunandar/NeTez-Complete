# Peningkatan Identifikasi Vendor & Discovery LAN Scanner

Meningkatkan akurasi identifikasi vendor dan hostname pada LAN Scanner agar setara dengan aplikasi "WiFi Analyzer" atau Fing, terutama untuk mengatasi keterbatasan MAC address di Android modern.

## User Review Required

> [!IMPORTANT]
> Perubahan ini akan menambah intensitas scanning sedikit (ekstra HTTP probe ke gateway dan host tertentu) untuk mendapatkan identifikasi yang lebih akurat. Ini tetap dilakukan secara asynchronous agar tidak menghambat discovery utama.

## Proposed Changes

### Network Engine

#### [MODIFY] [LanScanner.kt](file:///C:/Users/Lenovo/AndroidStudioProjects/NeTez2 - Copy/app/src/main/java/com/herwin/netez/network/LanScanner.kt)
- Memperluas `ssdpCache` dan `mDnsCache` untuk menyimpan objek info lengkap (bukan cuma nama).
- Meningkatkan `getVendorEnhanced` dengan logika **Deep Fingerprinting**:
    - Mendeteksi vendor dari mDNS service types (misal: `_googlecast` -> Google).
    - Mendeteksi vendor dari SSDP Manufacturer/Model.
    - Melakukan HTTP probe lebih cerdas (ekstraksi `<title>` dan header `Server`).
    - Fingerprinting berbasis port (misal: port 62078 -> Apple, port 8008 -> Google Home).
- Menambah delay kecil pada `triggerArpCache` dan variasi port probe untuk meningkatkan kemungkinan ARP table terisi.

#### [MODIFY] [UpnpManager.kt](file:///C:/Users/Lenovo/AndroidStudioProjects/NeTez2 - Copy/app/src/main/java/com/herwin/netez/network/UpnpManager.kt)
- Mengoptimalkan parsing SSDP untuk menangkap data manufacturer secara lebih reliabel.

#### [MODIFY] [NetBiosResolver.kt](file:///C:/Users/Lenovo/AndroidStudioProjects/NeTez2 - Copy/app/src/main/java/com/herwin/netez/network/NetBiosResolver.kt)
- Memastikan parsing MAC dari Adapter Status stabil.

### Data Layer

#### [MODIFY] [VendorDatabase.kt](file:///C:/Users/Lenovo/AndroidStudioProjects/NeTez2 - Copy/app/src/main/java/com/herwin/netez/data/wifi/VendorDatabase.kt)
- Menambah daftar **Hardcoded OUI** yang lebih komprehensif untuk vendor populer di Indonesia (ZTE, FiberHome, Huawei, TP-Link, D-Link, ASUS, Apple, Samsung, Xiaomi, Oppo, Vivo).
- Memperbaiki `loadFromAssets` agar lebih resilien jika folder assets kosong (fallback ke cloud/internal).

#### [MODIFY] [NetworkUtils.kt](file:///C:/Users/Lenovo/AndroidStudioProjects/NeTez2 - Copy/app/src/main/java/com/herwin/netez/utils/NetworkUtils.kt)
- Meningkatkan `httpGet` untuk menangani redirect dan error code (401/403) agar tetap bisa mengambil header.

## Verification Plan

### Automated Tests
- Menjalankan scan di berbagai tipe network (jika tersedia emulator/device).
- Verifikasi Logcat untuk melihat sumber identifikasi baru (misal: `Discovery method: HTTP_BANNER`).

### Manual Verification
- User memeriksa apakah `192.168.1.1` sekarang terdeteksi vendornya (misal "ZTE" atau "FiberHome").
- User memeriksa apakah device lain yang sebelumnya "Unknown Vendor" sekarang muncul identifikasinya.
