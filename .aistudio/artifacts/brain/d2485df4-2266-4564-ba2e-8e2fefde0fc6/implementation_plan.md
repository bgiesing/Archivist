# Implementation Plan: Tablet, Foldable, Chromebook & Google TV Optimization

Comprehensive design and architectural plan to optimize Archivist for large screens (Pixel Tablet, Foldables, Chromebooks) and 10-foot interfaces (Google TV / Android TV), addressing taskbar overlapping, multi-column layouts, side-by-side panels, and remote/keyboard navigation.

---

## 1. User Review Required & Clarifications Confirmed
- **Setup Wizard on Wide Screens**: Two-column layout with hero visual and branding on the left, interactive feature/authorization cards and actions on the right.
- **Navigation Rail Label**: Shortened from "Automation" to **"Rules"** to fit neatly within the M3 Navigation Rail without clipping or awkward line wrapping.
- **Google TV & Keyboard Navigation**: Enhanced D-pad focus outlines (`Modifier.focusable()`, dynamic focus border indicators) and TV manifest declarations for Android TV/Chromebook compatibility.

---

## 2. Proposed Changes & Architecture

### A. Window Insets & Tablet Taskbar Safe Areas
* **Problem**: Currently `Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0))` ignores Android's persistent tablet taskbar, causing bottom buttons (`Continue`, `Skip`, grid item actions) to be partially hidden under the system taskbar.
* **Solution**:
  - In `ArchivistMainScreen.kt`, `SetupWizardScreen.kt`, and sub-screens, configure Scaffolds to respect `WindowInsets.safeDrawing` or `WindowInsets.navigationBars`.
  - Apply `Modifier.navigationBarsPadding()` / `Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))` to bottom action bars and floating button containers so they remain fully visible and clickable above the tablet taskbar.

### B. Setup Wizard Two-Column Layout for Wide Screens (`width >= 600.dp`)
* **File**: `app/src/main/java/com/example/ui/screens/SetupWizardScreen.kt`
* **Changes**:
  - Detect screen size via `BoxWithConstraints`.
  - On compact screens (< 600dp): Keep the vertical onboarding sequence with safe padding.
  - On wide screens (>= 600dp):
    - **Left Column**: Prominent hero illustration, branding, step indicators, and high-level value proposition.
    - **Right Column**: Scrollable feature card with privilege toggle (Shizuku permission, usage access, automation frequency), and cleanly pinned action buttons ("Continue", "Skip", "Get Started") with taskbar padding.

### C. Navigation Rail Label & Icon Optimization
* **File**: `app/src/main/java/com/example/ui/ArchivistMainScreen.kt`
* **Changes**:
  - Update `Screen.RULES.title` from `"Automation"` to `"Rules"`.
  - Ensure `NavigationRail` and `NavigationBar` use the concise title with correct badge counter positioning.

### D. App Manager: Side-by-Side 2-Column Storage & Status Panels
* **File**: `app/src/main/java/com/example/ui/screens/AppManagerScreen.kt`
* **Changes**:
  - On wide screens (>= 600dp):
    - Display the **Device Storage Card** and **Privileged Status Card** side-by-side in a 2-column `Row(modifier = Modifier.fillMaxWidth())`.
    - Keep the status card fully expanded without collapsible toggle on wide screens since there is ample vertical and horizontal space.
    - Automatically reveal the app list/grid immediately without needing to scroll past stacked full-width cards.
  - **Default to Grid View on Big Screens**:
    - When `maxWidth >= 600.dp`, default the view mode to `ViewMode.GRID` if no preference is explicitly saved.
    - Optimize `LazyVerticalGrid` with `GridCells.Adaptive(minSize = 170.dp)` for tablet and desktop window widths.

### E. Rules / Automation Multi-Column Layout
* **File**: `app/src/main/java/com/example/ui/screens/RulesScreen.kt`
* **Changes**:
  - On wide screens, lay out the Rule configuration cards in an adaptive 2-column grid or side-by-side arrangement:
    - Column 1: Master switch, Storage Thresholds, and Inactivity Sliders.
    - Column 2: Minimum App Size, Notification Settings, and System App toggles.
  - Bottom action buttons ("Dry Run Preview" & "Run Check Now") positioned side-by-side with appropriate max width.

### F. Google TV & Chromebook Remote / Keyboard Navigation
* **Files**: `app/src/main/AndroidManifest.xml`, Theme & Composable components
* **Changes**:
  - Declare `<uses-feature android:name="android.software.leanback" android:required="false" />` and `<uses-feature android:name="android.hardware.touchscreen" android:required="false" />` so the app is installable on Android TV / Google TV.
  - Add reusable focus modifier: `Modifier.focusBorder()` that displays an animated M3 primary outline when focused via D-pad or keyboard tab.
  - Ensure interactive buttons, list/grid cards, and chips support D-pad directional navigation and Enter/DPAD_CENTER activation.

---

## 3. Verification Plan
1. **Compilation Check**: Run `compile_applet` to confirm all layout and inset changes compile cleanly.
2. **Local JVM & Robolectric Tests**: Run `gradle testDebugUnitTest` to ensure all state and preference tests pass without regressions.
3. **Screen Size Variations**: Validate responsiveness across Compact (<600dp), Medium (600-840dp), and Expanded (>840dp) widths.
4. **Insets Verification**: Verify the bottom navigation buttons sit above taskbar insets on tablet configurations.
