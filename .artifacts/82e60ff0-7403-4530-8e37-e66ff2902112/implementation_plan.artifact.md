# WiFi Security Audit Enhancement Plan

Enhance the WiFi Security Audit feature with a structured risk assessment, weighted scoring, and professional UI components as per the new specification.

## User Review Required

> [!IMPORTANT]
> The scoring logic will shift from a simple average to a weighted category-based score (100 total points). This may result in different security grades for the same network compared to the previous version.

> [!WARNING]
> Some checks (like Router Management Security) perform active port scanning on the gateway. This is limited only to the currently connected gateway.

## Proposed Changes

### Core Logic & Data Models

#### [MODIFY] [WifiSecurityEngine.kt](file:///C:/Users/Lenovo/AndroidStudioProjects/NeTez2%20-%20Copy/app/src/main/java/com/herwin/netez/network/WifiSecurityEngine.kt)
- Update `WifiCheckStatus` enum to include `UNKNOWN`.
- Update `WifiCheckItem` to include `category`, `score`, `maxScore`, `recommendation`, and `durationMs`.
- Update `WifiAuditResult` to include category scores, security grade, and checklist progress.
- Implement the 20+ checks described in the spec:
    - Encryption (WPA3, WPA2, WPA, WEP, OPEN)
    - Cipher detection (CCMP, GCMP, TKIP)
    - Authentication (SAE, PSK, 802.1X)
    - WPS & PMF detection
    - SSID & Hidden SSID analysis
    - Rogue/Suspicious AP detection (comparing nearby BSSIDs)
    - Gateway service exposure (Telnet, HTTP, HTTPS, SSH, UPnP)
    - DNS & Android Private DNS detection
    - MAC Randomization & Captive Portal detection
    - LAN Exposure (integrated with existing scanner)
- Implement the weighted scoring architecture:
    - Encryption & Auth (35)
    - WiFi Config (20)
    - AP Trust (15)
    - Network Security (15)
    - Privacy (10)
    - Network Exposure (5)

### ViewModel & UI

#### [MODIFY] [WifiSecurityViewModel.kt](file:///C:/Users/Lenovo/AndroidStudioProjects/NeTez2%20-%20Copy/app/src/main/java/com/herwin/netez/ui/viewmodels/WifiSecurityViewModel.kt)
- Update UI state handling for the new `WifiAuditResult`.
- Enhance report generation for copy/share functionality.
- Implement BSSID/IP copy logic.

#### [MODIFY] [WifiSecurityScreen.kt](file:///C:/Users/Lenovo/AndroidStudioProjects/NeTez2%20-%20Copy/app/src/main/java/com/herwin/netez/ui/screens/WifiSecurityScreen.kt)
- Redesign the "Current WiFi Card" with copy buttons and better signal/frequency display.
- Add the "Security Score Card" with category breakdown and progress bars.
- Update the "Findings Summary" and "Audit Detail List".
- Implement the "Secure WiFi Checklist" section.
- Add expandable detail cards with "Why it matters" and "Recommendation" sections.
- Integrate "Fix Actions" (Open Router, DNS Settings, LAN Scanner).

#### [MODIFY] [strings.xml](file:///C:/Users/Lenovo/AndroidStudioProjects/NeTez2%20-%20Copy/app/src/main/res/values/strings.xml)
- Add new strings for the 20+ checks, recommendations, and status labels (SAFE, WARNING, CRITICAL, INFO, UNKNOWN).
- Add strings for category names and checklist items.

## Verification Plan

### Automated Tests
- Unit tests for `WifiSecurityEngine` scoring logic with mocked `ScanResult` and `WifiInfo`.
- Verification of category weight calculations.

### Manual Verification
- Deploy to device and run audit on different WiFi networks (Open, WPA2, WPA3 if available).
- Verify UI rendering of score card and detail sections.
- Test "Copy Report" and "Share" buttons.
- Test "Open Router" and "Private DNS" navigation actions.
