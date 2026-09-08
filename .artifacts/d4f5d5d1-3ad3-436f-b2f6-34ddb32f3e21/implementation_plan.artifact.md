# App Permission Scanner Implementation Plan

Create a new security tool that scans installed applications for high-risk permission combinations, providing a risk score and the ability to uninstall apps directly.

## Proposed Changes

### [Data Layer]

#### [NEW] [AppPermissionInfo.kt](file:///C:/Users/Lenovo/AndroidStudioProjects/NeTez2%20-%20Copy/app/src/main/java/com/herwin/netez/data/AppPermissionInfo.kt)
- Data class to store app details: name, package name, icon (as Drawable or similar), permissions, risk score, and risk category.

### [Network/Service Layer]

#### [NEW] [AppScannerManager.kt](file:///C:/Users/Lenovo/AndroidStudioProjects/NeTez2%20-%20Copy/app/src/main/java/com/herwin/netez/network/AppScannerManager.kt)
- Logic to use `PackageManager.getInstalledApplications()`.
- Logic to get permissions via `PackageInfo`.
- Risk scoring logic:
    - `READ_SMS` + `READ_CONTACTS` + `RECORD_AUDIO` = 95 (Pinjol Suspect).
    - `CAMERA` + `LOCATION` = 60.

### [UI Layer - ViewModels]

#### [NEW] [AppPermissionViewModel.kt](file:///C:/Users/Lenovo/AndroidStudioProjects/NeTez2%20-%20Copy/app/src/main/java/com/herwin/netez/ui/viewmodels/AppPermissionViewModel.kt)
- Manage scanning state and progress.
- Filter logic (All, Dangerous, Suspicious).
- Expose `StateFlow` for UI.

### [UI Layer - Screens]

#### [NEW] [AppPermissionScannerScreen.kt](file:///C:/Users/Lenovo/AndroidStudioProjects/NeTez2%20-%20Copy/app/src/main/java/com/herwin/netez/ui/screens/AppPermissionScannerScreen.kt)
- List-based UI matching `LanScannerScreen`.
- Filter chips: All, Dangerous, Suspicious.
- App cards with icon, name, permission tags, risk score, and Uninstall button.
- Uninstall intent handler.

#### [MODIFY] [SecurityScreen.kt](file:///C:/Users/Lenovo/AndroidStudioProjects/NeTez2%20-%20Copy/app/src/main/java/com/herwin/netez/ui/screens/SecurityScreen.kt)
- Add "App Permission Scanner" `ToolItem`.

#### [MODIFY] [Screen.kt](file:///C:/Users/Lenovo/AndroidStudioProjects/NeTez2%20-%20Copy/app/src/main/java/com/herwin/netez/ui/navigation/Screen.kt)
- Add `AppPermissionScanner` route.

#### [MODIFY] [NavGraph.kt](file:///C:/Users/Lenovo/AndroidStudioProjects/NeTez2%20-%20Copy/app/src/main/java/com/herwin/netez/ui/NavGraph.kt)
- Add composable for the new screen.

### [Resources]

#### [MODIFY] [strings.xml](file:///C:/Users/Lenovo/AndroidStudioProjects/NeTez2%20-%20Copy/app/src/main/res/values/strings.xml)
- Add necessary strings for the new feature.

## Verification Plan

### Manual Verification
1.  Navigate to Security -> App Permission Scanner.
2.  Verify scanning progress and completion.
3.  Test filters (All, Dangerous, Suspicious).
4.  Verify risk scoring for known apps (if any match the criteria).
5.  Test "Uninstall" button - it should open the system uninstaller for the selected app.
6.  Verify the dark theme colors match the requirements.
