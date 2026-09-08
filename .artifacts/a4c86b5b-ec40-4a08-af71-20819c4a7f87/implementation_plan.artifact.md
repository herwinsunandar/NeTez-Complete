# Implementation Plan - Optimized LAN Discovery Engine

Optimize LAN Scanner discovery speed and accuracy by fixing MulticastLock handling, implementing ARP cache triggers, and improving active probing reliability.

## User Review Required

> [!IMPORTANT]
> **Performance vs Battery:** MulticastLock will be acquired during the scan to ensure mDNS/SSDP discovery works. It will be released immediately after the scan finishes to prevent battery drain.
>
> **Network Load:** We will increase concurrency to 128 workers with short timeouts. This is safe for modern home routers but might trigger "port scanning" alerts on some enterprise firewalls.

## Proposed Changes

### Discovery Engine & Logic

#### [MODIFY] [LanScanner.kt](file:///C:/Users/Lenovo/AndroidStudioProjects/NeTez2%20-%20Copy/app/src/main/java/com/herwin/netez/network/LanScanner.kt)
- **Multicast Lock:** Implement `WifiManager.MulticastLock` acquisition at the start of `scanSubnet` and `scanRange`.
- **ARP Trigger:** Add `triggerArpCache()` to send a UDP broadcast and fast parallel probes to populate the system ARP table before reading it.
- **Improved Probing:**
    - Replace `InetAddress.isReachable()` with a more reliable combination of ARP check and parallel TCP probes.
    - Expand `DISCOVERY_PORTS` to include common service ports (22, 80, 443, 445, 139, 135, 3389) to find "stealth" devices.
- **Diagnostic Logging:** Add `Log.d` markers for `SCAN_START`, `HOST_DISCOVERED` (with method), and `SCAN_COMPLETE` for benchmarking.
- **Async Enrichment:** Ensure hostname resolution and port scanning don't block the initial device discovery emission.

#### [MODIFY] [MdnsManager.kt](file:///C:/Users/Lenovo/AndroidStudioProjects/NeTez2%20-%20Copy/app/src/main/java/com/herwin/netez/network/MdnsManager.kt)
- Add safety checks for `MulticastLock` state.
- Optimize resolve timeouts.

#### [MODIFY] [NetworkUtils.kt](file:///C:/Users/Lenovo/AndroidStudioProjects/NeTez2%20-%20Copy/app/src/main/java/com/herwin/netez/utils/NetworkUtils.kt)
- Improve `getSubnetInfo` to fallback to `NetworkInterface` properties if `WifiManager.dhcpInfo` is incomplete.

## Verification Plan

### Automated Tests
- No new automated tests, verification will be via Logcat analysis.

### Manual Verification (Logcat Benchmark)
1. Open Logcat with filter `LanScanner`.
2. Start a scan.
3. Verify `SCAN_START` shows correct CIDR.
4. Check `HOST_DISCOVERED` logs:
    - Verify devices appear immediately via `method=ARP` or `method=CACHE`.
    - Verify Windows laptops are found via `method=TCP` even if ICMP fails.
5. Compare `DISCOVERY_COMPLETE` duration with previous behavior (expected < 5-10s for /24).
6. Verify hostname enrichment (mDNS/NetBIOS) fills in after the initial discovery.
