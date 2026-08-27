# Rencana Implementasi Aplikasi NeTez

Aplikasi NeTez adalah alat bantu jaringan (Network Tools) komprehensif untuk Android. Rencana ini mencakup arsitektur, fitur, dan tahapan pengembangan untuk membangun aplikasi sesuai permintaan.

## User Review Required

> [!IMPORTANT]
> **Izin Pengguna:** Fitur seperti "LAN Scanner" dan "WiFi Analyzer" memerlukan izin `ACCESS_FINE_LOCATION` dan `NEARBY_DEVICES` (pada Android baru). Pengguna harus memberikan izin ini agar fitur berfungsi.

> [!WARNING]
> **Fitur Speed Test:** Implementasi Speed Test yang akurat biasanya memerlukan server pengujian. Untuk MVP, saya akan menggunakan pendekatan pengunduhan file kecil dari URL publik atau menyarankan integrasi SDK pihak ketiga jika diperlukan hasil yang sangat profesional.

## Arsitektur & Teknologi

- **UI Framework:** Jetpack Compose dengan Material 3.
- **Arsitektur:** MVVM (Model-View-ViewModel).
- **Navigation:** Jetpack Navigation Compose.
- **Dependency Injection:** Hilt (Opsional, untuk kesederhanaan awal bisa menggunakan Manual DI).
- **Lokalitas:** Dukungan Bahasa Inggris (en) dan Indonesia (in) melalui file `strings.xml`.
- **Penyimpanan:** Room Database untuk riwayat pengujian.

## Tahapan Pengembangan

---

### Tahap 1: Fondasi & UI Shell
- Setup dependensi (Navigation, Icons, Room, Lifecycle).
- Implementasi Multi-bahasa (English & Indonesia).
- Membuat Bottom Navigation dengan 4 Tab: Diagnostic, Scanner, Utilities, Log & Report.
- Setup Tema Warna (Dark/Light mode).

### Tahap 2: Dashboard (Home)
- Implementasi kartu status koneksi (IP, SSID, Gateway).
- Tombol Quick Action.
- Ringkasan riwayat terakhir.

### Tahap 3: TAB A - Diagnostic
- **Ping Test:** Implementasi ICMP ping atau Socket ping dengan grafik sederhana.
- **DNS Lookup:** Resolusi hostname ke IP.
- **Speed Test:** Pengujian kecepatan dasar.

### Tahap 4: TAB B - Scanner
- **LAN Scanner:** Pemindaian subnet lokal menggunakan ARP/Reachability.
- **Port Scanner:** Pemindaian port TCP pada IP target.
- **WiFi Analyzer:** Menampilkan daftar Access Point sekitar (Memerlukan izin Lokasi).

### Tahap 5: TAB C - Utilities
- **IP Calculator:** Logika perhitungan subnetting (CIDR, Wildcard, dll).
- **HTTP Header Checker:** Mengambil header respon dari URL.
- **Wake on LAN:** Mengirimkan paket Magic Packet (UDP).

### Tahap 6: TAB D - Log & Report
- Riwayat hasil tes tersimpan di database.
- Ekspor data sederhana.

---

## Verifikasi Plan

### Pengujian Otomatis
- Unit Test untuk logika IP Calculator.
- Unit Test untuk parsing DNS.

### Pengujian Manual
- Verifikasi perpindahan bahasa sesuai sistem HP.
- Pengujian fitur Ping ke alamat IP yang dikenal (misal: 8.8.8.8).
- Verifikasi pemindaian LAN pada jaringan WiFi yang sama.
