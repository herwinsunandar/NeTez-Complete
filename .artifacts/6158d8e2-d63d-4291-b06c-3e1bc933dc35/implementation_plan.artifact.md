# Rencana Implementasi: Perbaikan Identifikasi Vendor pada WiFi Scanner

Masalah "Unknown Vendor" sering terjadi karena daftar OUI (prefix MAC address) yang tidak lengkap atau karena pembatasan akses MAC address pada Android versi terbaru (10+). Rencana ini bertujuan untuk memperkaya database lokal dan meningkatkan deteksi berbasis hostname untuk perangkat Huawei dan vendor lainnya.

## Perubahan yang Diusulkan

### Utils & Network

#### [MODIFY] [NetworkUtils.kt](file:///C:/Users/Lenovo/AndroidStudioProjects/NeTez/app/src/main/java/com/herwin/netez/utils/NetworkUtils.kt)
- Memperbarui fungsi `getVendor` dengan daftar OUI Huawei yang lebih lengkap.
- Menambahkan daftar OUI untuk vendor populer lainnya yang belum ada (Xiaomi, OPPO, Vivo, dll).
- Memperbaiki kesalahan penulisan (typo) pada entri OUI yang sudah ada.

#### [MODIFY] [LanScanner.kt](file:///C:/Users/Lenovo/AndroidStudioProjects/NeTez/app/src/main/java/com/herwin/netez/network/LanScanner.kt)
- Meningkatkan fungsi `inferVendor` untuk mendeteksi perangkat melalui hostname dengan pola yang lebih cerdas (misal: awalan model perangkat Huawei).
- Menambahkan logika fallback untuk mendeteksi vendor melalui port atau layanan mDNS jika MAC address tidak tersedia (Android 10+).

## Rencana Verifikasi

### Pengujian Otomatis
- Menjalankan unit test untuk `NetworkUtils.getVendor` dengan berbagai sample MAC address Huawei yang baru ditambahkan.
- Menjalankan unit test untuk `inferVendor` dengan berbagai contoh hostname.

### Verifikasi Manual
- Meminta user menjalankan scan ulang pada jaringan yang memiliki perangkat Huawei untuk memastikan vendor terdeteksi dengan benar.
- Memastikan tidak ada penurunan performa saat proses scanning berlangsung.
