# Run TV Home on another Android TV

TV Home is an ordinary APK, so the launcher itself moves to any Android TV. What does **not** move is
the TCL-specific debloat list and the default dock packages — those are configuration, not code.

Two routes: install the APK you already have (same apps, same weather city), or rebuild with that TV's
packages and city.

## What carries over

| Piece | Portable? | Notes |
|---|---|---|
| Launcher APK | **Yes** | `minSdk 26` (Android 8), leanback optional, signed with the debug key |
| Home / D-pad / glass / hero / mascot | **Yes** | Pure Android views; the mascot is Canvas-drawn, no asset |
| Screensaver (`ClockDream`) | **Yes** | Standard `DreamService` |
| Weather | **Yes**, after editing the city | Open-Meteo forecast + air quality, no key; condition text and AQI |
| Inputs list | Usually | Needs `TvInputManager`; some older TVs need `source_panel_service` in config |
| Wi-Fi name | Usually | Needs `ACCESS_FINE_LOCATION` granted over adb |
| Dock, hidden apps, columns | **No** | TCL package names; edit `config.xml` |
| Debloat list | **No — do not reuse** | Every brand's packages differ |

## Route A — install the existing APK

You need `adb` on the PC and the TV on the same network.

1. **Enable ADB on the TV.** Settings → System → About → tap *Android TV build* 7 times; then
   Developer options → **USB debugging** / **Wireless debugging** on.
2. **Connect.**
   ```bash
   adb connect 192.168.1.50:5555
   adb devices          # accept the prompt on the TV if one appears
   ```
3. **Install.**
   ```bash
   adb install -r app-release.apk
   ```
   The release APK is at
   `tv-vibehack/launcher/app/build/outputs/apk/release/app-release.apk`.
4. **Grant the Wi-Fi permission** (optional, but the pill otherwise says just "Wi-Fi"):
   ```bash
   adb shell pm grant com.gathin.tvhome android.permission.ACCESS_FINE_LOCATION
   ```
5. **Make it Home.**
   ```bash
   adb shell cmd package set-home-activity --user 0 com.gathin.tvhome/.MainActivity
   ```
   On Android 8–9, or if that command is missing, press Home and choose **TV Home** in the picker,
   then **Always**. You can also start the picker yourself:
   ```bash
   adb shell am start -a android.intent.action.MAIN -c android.intent.category.HOME
   ```
6. **Screensaver.**
   ```bash
   adb shell settings put secure screensaver_components com.gathin.tvhome/.ClockDream
   adb shell settings put secure screensaver_enabled 1
   ```
7. **Verify before disabling anything.** Press Home, open an app, return with Back/Home, open Inputs,
   open Settings. Only when all of that works should you disable the stock launcher:
   ```bash
   adb shell pm disable-user --user 0 <stock-launcher-package>
   ```
   Never uninstall it. If Home ever lands on a black screen, `pm enable --user 0 <stock-launcher>`
   brings it back.

The repo's `tv-setup/install-any-tv.ps1` does steps 2–6 in one go.

## Route B — rebuild for that TV

Use this when the dock apps, hidden apps or weather city are different.

1. Install **JDK 17** and the **Android SDK** (platform 34, build-tools).
2. Set `JAVA_HOME` and `ANDROID_HOME`.
3. Edit `tv-vibehack/launcher/app/src/main/res/values/config.xml`:
   - `dock_apps` - that TV's streaming apps, in dock order;
   - `app_order` - the grid order under the dock (optional; the rest is alphabetical);
   - `hidden_apps` - its stock launcher, settings, tuner, promo panels;
   - `live_tv_package` — its tuner app (or leave empty);
   - `weather_city`, `weather_lat`, `weather_lon`, `weather_units`, `weather_aqi`;
   - `columns` / `rows_below_dock` if the screen is unusual.
4. Find package names on the TV:
   ```bash
   adb shell pm list packages
   adb shell cmd package resolve-activity -c android.intent.category.HOME
   ```
5. Build:
   ```bash
   cd tv-vibehack/launcher
   ./gradlew assembleRelease        # Windows: .\gradlew.bat assembleRelease
   ```
6. Follow Route A with the new APK.

## Restore that TV

1. Find the stock launcher and point Home back at it:
   ```bash
   adb shell cmd package resolve-activity -c android.intent.category.HOME
   adb shell pm enable --user 0 <stock-launcher-package>
   adb shell cmd package set-home-activity --user 0 <stock-launcher-package>/<activity>
   ```
2. Put the screensaver back (or just set it in Settings):
   ```bash
   adb shell settings put secure screensaver_components com.google.android.tv/.dream...
   ```
   Easiest: re-enable Google's ambient mode (`dreamx`) and pick it in Settings.
3. Re-enable anything you disabled, one at a time:
   ```bash
   adb shell pm list packages -d
   adb shell pm enable --user 0 <package>
   ```
4. Remove TV Home if you don't want it:
   ```bash
   adb uninstall com.gathin.tvhome
   ```

## Brand notes

| TV | Home package (typical) | What to watch |
|---|---|---|
| TCL G13 (tested) | `com.google.android.apps.tv.launcherx` | The whole repo is tuned for this one |
| Chromecast / Google TV | `com.google.android.apps.tv.launcherx` | Inputs list works; no tuner |
| Sony / Hisense / Philips | brand launcher, e.g. `com.sony.dtv...`, `com.hisense...` | Resolve the exact component before disabling |
| Xiaomi / Mi TV | `com.mitv.tvhome` (varies) | Some remotes send `KEYCODE_ALL_APPS` for Home; enable `HomeKeyService` (opt-in build flag) |
| Fire TV | Amazon launcher | Not tested. Amazon restricts Home replacement; setting a third-party Home is not officially supported |
| Android TV 8–9 | varies | `cmd package set-home-activity` may not exist; use the Home chooser |

## The exact steps used on this TV (agent playbook)

This is the path this repo actually used. It is reproducible; the only machine-specific parts are the
IP, the JDK/SDK paths and the Python virtualenv.

### Environment

```powershell
$env:TV_HOST = '192.168.1.8'
$env:JAVA_HOME = 'C:\Users\autom\dev\jdk-17.0.20.1+1'
$env:ANDROID_HOME = 'C:\Users\autom\dev\android-sdk'
$py = 'tv-vibehack\tools\venv\Scripts\python.exe'
```

The repo's `tv-vibehack/tools/tv.py` is a small ADB client (`adb-shell`) that works when stock `adb`
is unavailable or the TV's pairing is finicky.

### Build

```powershell
Remove-Item tv-vibehack\launcher\app\build -Recurse -Force   # OneDrive locks the incremental merge
tv-vibehack\launcher\gradlew.bat -p tv-vibehack\launcher assembleRelease --console=plain --no-daemon
```

The APK lands in `tv-vibehack\launcher\app\build\outputs\apk\release\app-release.apk`.

### Install and verify

```powershell
& $py -I tv-vibehack\tools\b64push.py app-release.apk /data/local/tmp/tvhome.apk
& $py -I tv-vibehack\tools\tv.py 'pm install -r /data/local/tmp/tvhome.apk'
& $py -I tv-vibehack\tools\tv.py 'rm -f /data/local/tmp/tvhome.apk'
& $py -I tv-vibehack\tools\tv.py 'am force-stop com.gathin.tvhome'
& $py -I tv-vibehack\tools\tv.py 'am start -n com.gathin.tvhome/.MainActivity'
& $py -I tv-vibehack\tools\tv.py 'screencap -p /sdcard/home.png'
& $py -I tv-vibehack\tools\tv.py --pull /sdcard/home.png screenshots\home.png
```

### Home, grants, screensaver

```powershell
& $py -I tv-vibehack\tools\tv.py 'pm grant com.gathin.tvhome android.permission.ACCESS_FINE_LOCATION'
& $py -I tv-vibehack\tools\tv.py 'cmd package set-home-activity --user 0 com.gathin.tvhome/.MainActivity'
& $py -I tv-vibehack\tools\tv.py 'settings put secure screensaver_components com.gathin.tvhome/.ClockDream'
& $py -I tv-vibehack\tools\tv.py 'settings put secure screensaver_enabled 1'
```

### Debloat (TCL only)

`tv-vibehack/examples/tcl-g13-google-tv/debloat.txt` is the exact list. Run **one** `pm` command per
connection:

```powershell
Get-Content debloat.txt | ForEach-Object {
    if ($_ -and -not $_.StartsWith('#')) {
        & $py -I tv-vibehack\tools\tv.py "pm disable-user --user 0 $_"
    }
}
```

On another brand, build a new list by hand. Keep Play Services, Play Store, the keyboard,
remote/pairing, TV input services and the USB media player.

### Pitfalls that cost us time

- **Remote shell quoting.** `tv.py` loses double quotes; filter output locally instead of using
  `grep "..."` on the TV.
- **OneDrive + Gradle.** Incremental resource merges fail with a lock on `merged.dir\values`. Delete
  `app\build` and rebuild; that is why `rebuild-launcher.ps1` always cleans.
- **`screencap` and dreams.** It wakes the TV from Sleep but not always from Dreaming. Poll
  `dumpsys power | grep mWakefulness=` first, and set `screen_off_timeout` low for tests.
- **Doze drops Wi-Fi.** An idle TCL G13 leaves the network and ADB times out. Press a remote button;
  there is no reliable software wake.
- **`pm` chaining.** Do not send `pm enable a; pm enable b` in one shell call; the second is often
  dropped. One per call.
- **The stock launcher still answers Home.** Disable it only after TV Home is verified; if you
  disable it first and the new launcher fails, the screen is empty.

### New-TV checklist

- [ ] ADB connected, `adb devices` shows `device`
- [ ] APK installed, `pm list packages | findstr tvhome`
- [ ] Launcher opens with `am start -n com.gathin.tvhome/.MainActivity`
- [ ] Home button lands on TV Home (picker → Always)
- [ ] Dock apps match packages that exist
- [ ] Inputs lists the TV's real inputs
- [ ] Wi-Fi pill shows the SSID (location granted)
- [ ] Screensaver selected and shows after the timeout
- [ ] Weather shows a temperature (or `--` if offline)
- [ ] Back/Home from every app returns to TV Home
- [ ] Stock launcher disabled **last**
- [ ] `restore-tv.ps1` equivalent written for that TV
