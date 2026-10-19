# Your TCL Google TV — what changed, and how to change it back

Everything below was applied over Wi-Fi ADB on **19 October 2026**. Nothing was uninstalled and
nothing was flashed, so everything here is reversible.

## The TV

| | |
|---|---|
| Model / device | TCL Google TV, `ro.product.device` = **G13** ("Smart TV", MediaTek `mt5867`) |
| Android | 14 (SDK 34), Google TV |
| RAM | ~1 GB (`ro.config.low_ram=true`) |
| Address | `192.168.1.8:5555` (Wi-Fi, SSID `204_5G`) |
| Stock launcher | `com.google.android.apps.tv.launcherx` — the ad-filled one |
| New launcher | `com.gathin.tvhome` — built from `tv-vibehack`, ~250 KB, no ads, no network |

## What was done

1. **Backed up** the TV's package list and all settings to
   `tv-vibehack\backup\2026-10-19_124247\` before touching anything.
2. **Disabled 24 + 1 packages** for user 0 (never uninstalled). The list is
   `tv-vibehack\examples\tcl-g13-google-tv\debloat.txt`; the 25th,
   `com.google.android.apps.tv.launcherx`, was disabled as part of the Home switch.
3. **Built and installed the new launcher** and made it the default Home.
4. **Extras not in the list:**
   - granted the launcher `ACCESS_FINE_LOCATION` so the Wi-Fi pill can show the network name
     (`204_5G`) — Android only hands out the SSID to an app that holds it;
   - replaced Google TV's ambient-mode screensaver (`com.google.android.apps.tv.dreamx`, which can
     show promos) with the launcher's own drifting **Clock**.

### What was kept

Netflix, Prime Video, YouTube, Apple TV, Disney+ Hotstar, Zee5, Airtel Xstream, YouTube Music,
the Play Store and Play Services, the on-screen keyboard, the USB media player
(`com.mediatek.wwtv.mediaplayer`) and TCL's browser, HDMI and tuner input services, the Live TV app,
Bluetooth and remote pairing, Chromecast, Miracast/AirPlay mirroring, and Google Assistant voice
search.

### What was disabled

TCL telemetry and OTA/analytics (`com.tcl.ttvs`, `com.tcl.logkit`), TCL content and promos
(`partnercustomizer`, `messagebox`, `waterfall.overseas`, `guard`, `keyhelp`, `com.tpa.kb`,
`com.mstar.android.tv.disclaimercustomization`), the silent partner-app installer
(`android.autoinstalls.config.google.gtvpai`), retail/factory demos and first-run wizards
(`fusion.android.tv.demo`, `com.mediatek.MtkDemo`, `com.mediatek.wwtv.setupwizard`,
`com.mediatek.wwtv.setup.customization`), Google feedback and ad infrastructure
(`com.google.android.feedback`, `com.android.tv.feedbackconsent`, `com.android.adservices.api`,
`com.android.ondevicepersonalization.services`, `com.android.federatedcompute.services`,
`com.google.android.apps.tv.dreamx`), preloaded apps you didn't want
(`com.google.android.videos`, `com.google.android.play.games`, `com.google.android.marvin.talkback`),
and the setup wizard `com.google.android.tungsten.setupwraith` — which had to go because on Google
TV it also answers the Home intent and would otherwise keep Home off the new launcher.

## Everyday use

- **Home button** → this launcher. **Up / Down** move exactly one row; Up from the dock reaches the
  top pills; the ends of the pill row don't fall through. The last app you focused is remembered.
- **Inputs** pill (top right) → HDMI 1, HDMI 2, Composite (AV), Live TV.
- **Wi-Fi** pill → shows `204_5G`; hold/select it for Wi-Fi settings.
- **Settings** pill → Android TV's own settings.
- The launcher is deliberately light: black background, no blur, no video, no background services.

You chose **not** to enable the optional accessibility service that maps the remote's physical
Input button to the input switcher, so that button does nothing outside Live TV — use the Inputs
pill instead. To add it later:

```powershell
cd tv-vibehack
$env:TV_HOST='192.168.1.8'
powershell -ExecutionPolicy Bypass -File launcher\build-and-install.ps1 -TvHost 192.168.1.8 -InputKeyService
```

## Change the four dock apps, or their order

They're literally the order of this list in
`tv-vibehack\launcher\app\src\main\res\values\config.xml`:

```xml
<string-array name="dock_apps">
    <item>com.google.android.youtube.tv</item>          <!-- YouTube -->
    <item>com.apple.atve.androidtv.appletv</item>       <!-- Apple TV -->
    <item>in.startv.hotstar</item>                      <!-- JioHotstar -->
    <item>com.amazon.amazonvideo.livingroom</item>      <!-- Prime Video -->
</string-array>
```

Reorder or swap the package names, save, then rebuild and reinstall:

```powershell
powershell -ExecutionPolicy Bypass -File tv-setup\rebuild-launcher.ps1
```

The same file also controls the number of grid `columns` (4 now), `rows_below_dock`, which apps are
kept out of the grid (`hidden_apps`), and the `live_tv_package`. Find a package name with:

```powershell
cd tv-vibehack; tools\venv\Scripts\python.exe -I tools\tv.py 'pm list packages'
```

## Change the wallpaper / background

The launcher has no wallpaper image: it draws a **pure black** window background, and the coloured
halo behind the big app name is sampled automatically from whichever dock app is focused. To change
the background, edit `tv-vibehack\launcher\app\src\main\res\values\styles.xml`:

```xml
<style name="Theme.TvHome" parent="android:Theme.Material.NoActionBar.Fullscreen">
    <item name="android:windowBackground">@android:color/black</item>   <!-- change me -->
    <item name="android:colorBackground">@android:color/black</item>
```

Replace `@android:color/black` with a colour literal such as `#0B0B10`, or with your own drawable in
`launcher\app\src\main\res\drawable\`. Then run `rebuild-launcher.ps1`.

## Re-enable a single app

Nothing was uninstalled, so one command brings any of them back:

```powershell
cd tv-vibehack
$env:TV_HOST='192.168.1.8'
tools\venv\Scripts\python.exe -I tools\tv.py 'pm enable --user 0 com.tcl.messagebox'
```

Run **one `pm` command per call** — chained `pm ...; pm ...` can silently stop after the first. See
what is currently off with `pm list packages -d`. The most likely ones you'd want back:

| Package | What it is |
|---|---|
| `com.google.android.apps.tv.dreamx` | Google TV ambient-mode screensaver (can show promos) |
| `com.google.android.videos` | Google TV "Movies & TV" store |
| `com.tcl.ttvs` | TCL service **and the firmware/OTA updater** |
| `com.google.android.marvin.talkback` | TalkBack screen reader |
| `com.google.android.tungsten.setupwraith` | first-run setup wizard — **re-enable before a factory reset** |

## Put the TV completely back

```powershell
powershell -ExecutionPolicy Bypass -File tv-setup\restore-tv.ps1
```

That re-enables all 24 packages, re-enables and re-selects `com.google.android.apps.tv.launcherx` as
Home, restores the animation scales and the original screensaver setting, and prints what is still
disabled. Add `-UninstallLauncher` to also remove the custom launcher.

## If something goes wrong

- **Home lands on a black screen or a factory test screen.** The stock launcher is still installed;
  bring it back and it will take Home again:
  ```powershell
  tools\venv\Scripts\python.exe -I tools\tv.py 'pm enable --user 0 com.google.android.apps.tv.launcherx'
  ```
- **The TV stops answering on `192.168.1.8:5555`.** Android TV we dozes its Wi-Fi when the screen
  has been off a while. Press a button on the remote to wake it and it comes back. If it doesn't,
  check the TV is on and still joined to `204_5G`.
- **A screenshot comes out empty.** `screencap` returns nothing while Live TV or an HDMI input is on
  screen on TCL; press Home first.
- **Wi-Fi pill shows just "Wi-Fi".** The location permission was lost (usually after a reinstall).
  Re-run `tv-setup\rebuild-launcher.ps1`, which grants it and verifies it.
- **Last resort, without any tools:** the TV's own Settings → Apps page can re-enable anything you
  disabled. A factory reset is the final fallback, not the first.

## One leftover step: the Clock screensaver

The TV went off the network before this could be applied, so it is still the plain fallback colour.
Run this once, whenever the TV is on and reachable:

```powershell
powershell -ExecutionPolicy Bypass -File tv-setup\set-clock-screensaver.ps1
```

It points the screensaver at `com.gathin.tvhome/.ClockDream` — a large thin clock and date over an
ambient glow that drifts to a new spot each minute so nothing burns in. It starts after the TV has
been idle for `screen_off_timeout` (to change that idle time, use
`tools\venv\Scripts\python.exe -I tools\tv.py 'settings put system screen_off_timeout 600000'`, in
milliseconds). The restore script undoes it either way.

## When you're finished

**Switch USB debugging off on the TV** (Settings → Device Preferences → Developer options). The
launcher keeps working without it, and while it's on the TV's USB ports may lose power / drop into
device mode.
