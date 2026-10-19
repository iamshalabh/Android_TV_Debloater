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
     show promos) with the launcher's own **Clock** dream, then extended it (greeting, time-of-day
     colour, 3×3 drift, second glow);
   - put your name in the greeting: **GOOD AFTERNOON, VEDARTH**;
   - unhid the **Live TV** app so it is a normal tile as well as an Inputs entry;
   - made **sideloading** usable: `tv-setup\install-apk.ps1` installs an APK from a file or a GitHub
     URL, and TCL's browser is allowed to install unknown apps.

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
- **Live TV** is also a normal tile in the grid now (you asked for that), and still appears at the
  bottom of the Inputs menu.
- **Wi-Fi** pill → shows `204_5G`; hold/select it for Wi-Fi settings.
- **Settings** pill → Android TV's own settings.
- The greeting reads **GOOD AFTERNOON, VEDARTH** (and morning/evening/night by the clock). The comma
  is the normal English separator between a greeting and a name; change the name in `config.xml`
  (see [Your name](#your-name-in-the-greeting)).
- The launcher is deliberately light: black background, no blur, no video, no background services.

You chose **not** to enable the optional accessibility service that maps the remote's physical
Input button to the input switcher, so that button does nothing outside Live TV — use the Inputs
pill instead. To add it later:

```powershell
cd tv-vibehack
$env:TV_HOST='192.168.1.8'
powershell -ExecutionPolicy Bypass -File launcher\build-and-install.ps1 -TvHost 192.168.1.8 -InputKeyService
```

## Install apps from GitHub or other sources

There was no way to install anything that isn't in the Play Store: the TV has Android's APK installer
(`com.google.android.packageinstaller`) but **no file manager and no download UI**, so "open the
downloaded file on the TV" simply isn't available. Two paths now exist.

**1. From this PC — works for any APK, including GitHub release assets.** Point the script at a file
or a URL; it downloads if needed, pushes the APK to the TV and installs it:

```powershell
powershell -ExecutionPolicy Bypass -File tv-setup\install-apk.ps1 C:\Downloads\some-app.apk
powershell -ExecutionPolicy Bypass -File tv-setup\install-apk.ps1 https://github.com/owner/repo/releases/download/v1.2/app-androidtv.apk
```

Installed apps appear in the launcher grid automatically (press Home if it hasn't refreshed). To
remove one: `tv-vibehack\tools\venv\Scripts\python.exe -I tools\tv.py 'pm uninstall <package>'`.

**2. On the TV itself.** I enabled "install unknown apps" for TCL's browser:

```powershell
tools\venv\Scripts\python.exe -I tools\tv.py 'appops set com.tcl.browser REQUEST_INSTALL_PACKAGES allow'
```

so a direct APK link opened in the TV's browser can be downloaded and installed. In practice the
browser on this set is clunky for that; the PC route is the one to use.

**Things that will bite**

- Get the **Android TV / leanback** build of an app, not the phone build. A phone APK installs fine
  but opens with no remote-navigable UI.
- Apps published as `.apks` bundles or as a folder of splits (many GitHub releases) don't install with
  `pm install`; look for a *universal* APK, or install the split set with
  `pm install-multiple` (the script does not do this yet).
- Play Protect may scan the first sideload. If it blocks, allow it once on screen, or relax it with
  `settings put global verifier_verify_adb_installs 0` (less safe — prefer allowing).
- ADB installs go to user 0, same as everything else here. Nothing is rooted.

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

There are two ways, and the first needs no rebuild at all.

**Theme overlay (no rebuild).** Upstream added a JSON overlay: a `config.json` with
`"background_color": "#0B0B10"` and/or `"wallpaper": true` next to a `wallpaper.jpg`, pushed to the
TV. The launcher applies it the next time Home starts. Keep the picture dark, 1920×1080 and under
~300 KB — a 1 GB TV decodes it into memory on every Home start, which is why black is still the
cheapest option here.

```powershell
cd tv-vibehack
$env:TV_HOST='192.168.1.8'
powershell -ExecutionPolicy Bypass -File tools\theme.ps1 -TvHost 192.168.1.8 themes\aurora
powershell -ExecutionPolicy Bypass -File tools\theme.ps1 -TvHost 192.168.1.8 --reset   # back to black
```

See `tv-vibehack\themes\README.md` for every key and the two bundled themes (`aurora`, `six-columns`).

**Baked in.** For a permanent change, edit
`tv-vibehack\launcher\app\src\main\res\values\styles.xml`:

```xml
<style name="Theme.TvHome" parent="android:Theme.Material.NoActionBar.Fullscreen">
    <item name="android:windowBackground">@android:color/black</item>   <!-- change me -->
    <item name="android:colorBackground">@android:color/black</item>
```

Replace `@android:color/black` with a colour literal such as `#0B0B10`, or with your own drawable in
`launcher\app\src\main\res\drawable\`. Then run `rebuild-launcher.ps1`.

## Your name in the greeting

`config.xml` has:

```xml
<string name="user_name" translatable="false">Vedarth</string>
```

The greeting becomes "GOOD AFTERNOON, VEDARTH" (the whole line is upper-cased by the layout). Leave it
empty to get the plain greeting back, or change the name, then run `rebuild-launcher.ps1`.

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

## The Clock screensaver

Applied and verified on the TV (`dream2.png`): it is `com.gathin.tvhome/.ClockDream`. Re-apply it any
time with `tv-setup\set-clock-screensaver.ps1`.

It draws everything with vectors, so **no bitmaps and no extra RAM**:

- your greeting and name above a large thin clock and the date;
- two soft radial glows that drift apart, with a palette that follows the time of day — teal/green in
  the morning, cool blue/teal in the afternoon, warm orange/pink in the evening, deep indigo at night;
- the clock steps through a **3×3 grid** with a little jitter once a minute, so every part of the
  screen gets used and nothing burns in (pure random clustering is why the old version could sit in
  the middle for ages);
- each change is a short fade on the minute tick. Nothing animates continuously, so the CPU idles
  between ticks.

**Ideas that would still be cheap, if you want more.** Roughly in order of cost:

| Idea | Cost | Notes |
|---|---|---|
| More palettes / a "scene" per hour | free | Just more colour sets; zero memory |
| Alternate clock typography (thin / wide / numerals) | free | Same font, different weight/size |
| A dimmed photo behind the clock | ~3.6 MB at 720p, one bitmap | The most expensive option here, but still small; needs a dark 1280×720 JPEG and a black scrim |
| Slow Ken-Burns drift on that one photo | no extra RAM | GPU only; cheap on a still image |
| Weather or the next calendar event | a little CPU/network | Needs an API key and `INTERNET`; the launcher currently has no network permission on purpose |
| Now-playing art from the TV | not worth it | Requires a media-session listener and constant redraws |

Tell me which of those you want and I'll add it.

It starts after the TV has been idle for `screen_off_timeout` (currently 10 minutes = 600000 ms):

```powershell
cd tv-vibehack
$env:TV_HOST='192.168.1.8'
tools\venv\Scripts\python.exe -I tools\tv.py 'settings put system screen_off_timeout 600000'
```

The restore script puts Google's ambient mode back either way.

## When you're finished

**Switch USB debugging off on the TV** (Settings → Device Preferences → Developer options). The
launcher keeps working without it, and while it's on the TV's USB ports may lose power / drop into
device mode.
