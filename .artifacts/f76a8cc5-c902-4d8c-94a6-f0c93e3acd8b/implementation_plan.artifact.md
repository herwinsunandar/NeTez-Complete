# Implementation Plan - LAN Scanner Performance Optimization

This plan focuses on significantly speeding up the LAN Scanner discovery process by implementing a "Fast-First" strategy, reducing redundant operations, and using asynchronous enrichment.

## User Review Required

> [!IMPORTANT]
> The UI remains 100% unchanged. All optimizations are internal to the discovery engine.
> Devices will appear in the list as soon as they are detected, with details (hostname, vendor, ports) updating asynchronously.

## Proposed Changes

### LAN Discovery Engine Optimization

#### [MODIFY] [LanScanner.kt](file:///C:/Users/Lenovo/AndroidStudioProjects/NeTez2%20-%20Copy/app/src/main/java/com/herwin/netez/network/LanScanner.kt)
- **Fast-First Discovery Strategy**:
    - Introduce a lightweight `fastProbeHost` function that only checks ICMP, high-priority ports (80, 445), and the ARP cache.
    - Emit devices to `onDeviceFound` immediately after the fast probe succeeds.
- **Asynchronous Enrichment**:
    - Discovered devices will trigger background tasks for enrichment (hostname resolution, full port scan, vendor identification).
    - `onDeviceFound` will be called again with enriched data as it becomes available.
- **Optimized ARP Table Handling**:
    - Read and cache the entire system ARP table once at the start of the scan instead of reading it for every IP.
- **Concurrency & Timeout Tuning**:
    - Use a dedicated coroutine dispatcher with controlled concurrency (e.g., 64-128 parallelism).
    - Reduce initial discovery timeouts to ~400ms for faster detection of active hosts.
- **Adaptive Port Scanning**:
    - Initial discovery only probes 1-2 ports. Full port scanning is deferred to the enrichment phase for confirmed active hosts only.
- **Diagnostic Logging**:
    - Enhanced performance metrics in Logcat to track time to first device and total scan duration.

### Network Utility Refinement

#### [MODIFY] [NetworkUtils.kt](file:///C:/Users/Lenovo/AndroidStudioProjects/NeTez2%20-%20Copy/app/src/main/java/com/herwin/netez/utils/NetworkUtils.kt)
- Ensure `getSubnetInfo` is efficient and correctly used by the scanner to limit the scan range to the actual subnet.

## Verification Plan

### Manual Verification
- **Speed Benchmarking**: Measure "Time to first device" and "Total scan time" using Logcat. Compare against the previous slow implementation.
- **Accuracy Test**: Ensure all devices (Android, Windows, IoT) are still detected correctly despite shorter timeouts.
- **Asynchronous Updates**: Observe the device list to ensure devices appear quickly and their details (like hostnames) populate a moment later.
- **UI Integrity**: Verify that the Scan Button, Progress Bar, and Device List animations/interactions remain exactly as they were.
- **Cancellation Test**: Verify that pressing "Stop" immediately halts all discovery and enrichment tasks.
