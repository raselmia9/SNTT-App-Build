# SNTT App Build

Android application for SNTT Tank Calibration & UI with automated APK build using GitHub Actions.

## Features
- **App Bar / Header:** Includes Title and dedicated Download icon button in the header.
- **Download Manager:** Fetches raw data/scripts sequentially from the configured JSON list.
- **TinyDB Persistence:** Saves downloaded data into Android SharedPreferences (using MIT App Inventor / Kodular `TinyDB1` conventions) under each specified TinyDB tag.
- **Progress Tracking:**
  - Real-time overall progress bar (0% - 100%).
  - Live count for Success, Failed, and Skipped.
  - Interactive item list showing Pending, Downloading, Saved, Skipped, and Failed status badges.
- **WebView Ready:** Pre-configured WebView with `TinyDB` JavascriptInterface (`window.TinyDB.getValue(tag)` & `window.TinyDB.storeValue(tag, value)`).
- **Automated CI/CD:** GitHub Actions workflow automatically compiles `app-debug.apk` on every push to `main` and creates a release asset and artifact for direct mobile download.

## Download Items
1. SNTT All Tank
2. SGTT All Tank (Skipped - None)
3. BAT 01
4. BAT 02
5. BAT 03
6. Binzaman 01
7. Binzaman 02
8. Binzaman 03
9. Binzaman 04
10. Mesra
11. Kingfisher 03
12. Kingfisher 04
13. M.T Rukh
14. Shanghai 07
15. O.T SHANGHAI-01
16. SNTT HOME UI.html
