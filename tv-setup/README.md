# Your TCL Google TV — what changed, and how to change it back

Everything below was applied over Wi-Fi ADB on **19 October 2026**. Nothing was removed from the
system partition and nothing was flashed, so everything here is reversible. Only two packages are
uninstalled for user 0 — **YouTube Music** (a system app, so it comes back with `pm install-existing`)
and **TV Bro** (installed by this project) — and both are recoverable; see
[Put the TV completely back](#put-the-tv-completely-back).

## The TV

| | |
|---|---|
| Model / device | TCL Google TV, `ro.product.device` = **G13** ("Smart TV", MediaTek `mt5867`) |
| Android | 14 (SDK 34), Google TV |
| RAM | ~1 GB (`ro.config.low_ram=true`) |
| Address | `192.168.1.8:5555` (Wi-Fi, SSID `204_5G`) |
| Stock launcher | `com.google.android.apps.tv.launcherx` — the ad-filled one |
| New launcher | `com.gathin.tvhome` — built from `tv-vibehack`, **~250 KB**, no ads, no libraries, no key |
| Grid apps | **Stremio** 1.10.4 official Android TV APK (`com.stremio.one`, sideloaded) and **Spotify** (`com.spotify.tv.android`, Play Store) in the grid |

## What was done

1. **Backed up** the TV's package list and all settings to
   `tv-vibehack\backup\2026-10-19_124247\` before touching anything.
2. **Disabled 24 packages** for user 0 (never uninstalled), plus the stock launcher as part of the
   Home switch. The list is `tv-vibehack\examples\tcl-g13-google-tv\debloat.txt`. One of them,
   **TCL Channel**, was later enabled again at your request, so **23 from the list + the stock
   launcher** are off now — `pm list packages -d` shows 24 lines.
3. **Built and installed the new launcher** and made it the default Home.
4. **Extras and later changes:**
   - granted the launcher `ACCESS_FINE_LOCATION` so the Wi-Fi pill can show the network name
     (`204_5G`) — Android only hands out the SSID to an app that holds it;
   - replaced Google TV's ambient-mode screensaver (`com.google.android.apps.tv.dreamx`, which can
     show promos) with the launcher's own **Clock** dream, then extended it;
   - put your name in the greeting — now **Good evening, Vedarth**, sentence case and above the date,
     with only the big **68 sp** clock sitting on the left;
   - swapped the MediaTek tuner tile for **TCL Channel** — the tuner still appears as **Live TV** in
     the Inputs menu;
   - reordered the top bar to **Wi-Fi → TV → gear → reorder**, and shortened the inputs pill from
     "Inputs" to **TV**;
   - **removed TV Bro** (and TCL BrowseHere earlier) and installed **Stremio** in its place, before
     Spotify in the grid; **uninstalled YouTube Music for your user** at your request;
   - moved the launcher to **Schibsted Grotesk** (subset to Latin, 104 KB) — the best balance of
     character and legibility from a shortlist that included Inter, Urbanist, Instrument Sans and a
     serif (Newsreader), compared in `docs\fonts.png`;
   - rebuilt the pills, Inputs dropdown and tiles with layered **glass** — gradients and rims, no blur;
   - turned the screensaver's digits into a Canvas-drawn **analog clock** with a ticking second hand,
     and added **Jaipur weather** to Home and the screensaver (Open-Meteo, no API key), including a
     plain-language condition and the **US AQI**;
   - added an on-Home **reorder mode** (the top-right icon) so any app, including lower rows, can be
     moved to any grid slot with the remote;
   - added a small **pixel companion** that hops to whichever tile you focus — drawn on a Canvas, so it
     costs no bitmap;
   - made **sideloading** usable: `tv-setup\install-apk.ps1` installs an APK from a file or a GitHub
     URL.

### What was kept

Netflix, Prime Video, YouTube, Apple TV, Disney+ Hotstar, Zee5, Airtel Xstream,
the Play Store and Play Services, the on-screen keyboard, the USB media player
(`com.mediatek.wwtv.mediaplayer`), **TCL Channel**, **Stremio**, **Spotify**, HDMI and tuner input
services, the Live TV tuner app, Bluetooth and remote pairing, Chromecast, Miracast/AirPlay
mirroring, and Google Assistant voice search.

### What was disabled

TCL telemetry and OTA/analytics (`com.tcl.ttvs`, `com.tcl.logkit`), TCL content and promos
(`partnercustomizer`, `messagebox`, `guard`, `keyhelp`, `com.tpa.kb`,
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
- **Inputs** → the **TV** pill (second from the left, after Wi-Fi) → HDMI 1, HDMI 2, Composite (AV),
  Live TV.
- **TCL Channel** is a tile in the grid. The MediaTek tuner no longer has a tile — that is the swap
  you asked for — but it is still the **Live TV** entry in the Inputs menu, so the tuner works.
- **Stremio** and **Spotify** are tiles in the grid. TV Bro was removed; TCL BrowseHere was removed
  earlier — there is no bundled browser now.
- **Reorder** → the top-right icon on Home. Focus stays on the icon (nothing is pre-selected), press
  **Down** into the grid, move to any app in any row, press **OK** to pick it up, move it anywhere with
  the D-pad, **OK** again to drop it, then **Back** to finish. **Long-press** the reorder icon to reset
  the grid to the configured order. The four dock apps stay fixed.
- **Wi-Fi** pill → shows `204_5G`; hold/select it for Wi-Fi settings.
- **Gear icon** → opens Android TV's own settings.
- The greeting reads **Good evening, Vedarth** (morning/afternoon/evening/night by the clock), sentence
  case, above the date. Change the name in `config.xml` (see
  [Your name](#your-name-in-the-greeting)).
- **Weather** sits under the date on Home and on the screensaver: **Jaipur 25°C · Clear · AQI 132** —
  a plain-language condition instead of an icon, plus the US AQI. It comes from Open-Meteo (weather
  and air quality), needs no account or key, and refreshes at most every 10 minutes. Offline, the last
  reading simply stays.
- The little **pixel companion** sits just above the top-right of whichever tile you focus and hops to
  the next one. It is drawn from rectangles on a Canvas — no image, no idle animation.
- The launcher is deliberately light: black background, no blur, no video, no background services.
  The only extras are one HTTPS request pair for the weather and the screensaver's one redraw per
  second.

You chose **not** to enable the optional accessibility service that maps the remote's physical
Input button to the input switcher, so that button does nothing outside Live TV — use the Inputs
pill instead. To add it later:

```powershell
cd tv-vibehack
$env:TV_HOST='192.168.1.8'
powershell -ExecutionPolicy Bypass -File launcher\build-and-install.ps1 -TvHost 192.168.1.8 -InputKeyService
```

## Do I need the Play Store?

**Yes — leave it enabled.** It is what installs and updates Netflix, Prime Video, YouTube, Apple TV,
Hotstar and the rest, and Play Services is what DRM playback, casting and the phone remote lean on.
When idle it costs almost nothing; disabling it would freeze every app at its current version and
prevent new Play apps from being installed.

Sideloaded apps are separate: they never appear in the Play Store, and they update from wherever you
got them (or by installing the newer APK with `install-apk.ps1`).

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

**2. On the TV itself.** There is currently no browser or file manager on the TV, so the on-device
route needs one installed first — or simply use the PC route above, which works for everything.

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

## Change the grid order

The row under the dock follows `app_order` in the same `config.xml`. Apps listed there come first,
in that order; every app you leave out follows alphabetically. The current default is:

```xml
<string-array name="app_order">
    <item>tv.airtel.xstream.tvapp</item>            <!-- Airtel Xstream Play -->
    <item>com.tcl.waterfall.overseas</item>         <!-- TCL Channel -->
    <item>com.netflix.ninja</item>                  <!-- Netflix -->
    <item>com.graymatrix.did</item>                 <!-- ZEE5 -->
    <item>com.stremio.one</item>                    <!-- Stremio -->
    <item>com.spotify.tv.android</item>             <!-- Spotify -->
</string-array>
```

Move the lines (or add packages) and rebuild — or skip the rebuild with a theme overlay:

```json
{ "app_order": ["tv.airtel.xstream.tvapp", "com.tcl.waterfall.overseas", "com.netflix.ninja"] }
```

Save that as `config.json` in a theme folder and push it with `tools\theme.ps1`; Home picks it up
the next time it starts. Dock apps stay in the dock even if they also appear in `app_order`.

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

## Fonts and glass

The launcher ships **Schibsted Grotesk**, subset to Latin (**104 KB**). It won the shortlist in
`docs\fonts.png` — Inter, Urbanist, Instrument Sans, Newsreader and Space Grotesk — because it has
more character than Inter at clock size yet still holds up at the 12–15 px labels.

Apple's SF Pro and Anthropic's custom Sans/Serif are proprietary and cannot be bundled, so those
directions were judged through their closest free stand-ins (Newsreader for the Claude-style serif;
Space Grotesk for Styrene's quirks). The serif reads editorial rather than TV, and Space Grotesk was
too quirky for the pills.

To change the font, drop a different variable font in at
`launcher\app\src\main\res\font\schibsted.ttf` (keep the name), or rename it and update the
`@font/schibsted` references, then run `rebuild-launcher.ps1`.

The pills, the Inputs dropdown and the tile placeholders are layered drawables: a translucent
gradient, a bright 1 dp rim and a faint inner sheen. That is the iOS "glass" look **without** a
backdrop blur — real blur would cost GPU time on every frame on this 1 GB MediaTek chip. Here, glass
is free.

## Your name in the greeting

`config.xml` has:

```xml
<string name="user_name" translatable="false">Vedarth</string>
```

The greeting becomes "Good evening, Vedarth" (sentence case; the time of day chooses morning /
afternoon / evening / night), shown above the date on the right. Leave `user_name` empty to get the
plain greeting back, or change the name, then run `rebuild-launcher.ps1`.

## Weather, city and units

`config.xml` also has:

```xml
<string name="weather_city" translatable="false">Jaipur</string>
<string name="weather_lat" translatable="false">26.9124</string>
<string name="weather_lon" translatable="false">75.7873</string>
<string name="weather_units" translatable="false">celsius</string>
<bool name="weather_aqi" translatable="false">true</bool>
```

Change the label and the coordinates (Google Maps → right-click the spot → copy the two numbers), set
`weather_units` to `fahrenheit` for °F, then rebuild. Leave `weather_city` empty to hide the line and
skip the request entirely. Set `weather_aqi` to `false` to drop the AQI and skip its request.

The line shows the temperature, a **plain-language condition** (Clear, Partly cloudy, Rain, Fog …,
mapped from the WMO code) and, when enabled, the **US AQI** — for example
`Jaipur 25°C · Clear · AQI 132`. There is no icon; text reads better on this TV and needs no image.

**What it costs.** The APK grew by ~3 KB total. Memory does not measurably change: the JSON and TLS
buffers exist only during a fetch, then are collected. The AQI is one small extra GET on the same
thread and cache, and adds a few bytes of string — no bitmap, no second cache. The launcher gains the
`INTERNET` permission — the only network it uses, and the only thing it ever sends is the request
itself (no identifiers, no account). If the TV is offline, the last reading stays and nothing retries
until the next tick.

## Re-enable a single app

Almost nothing was uninstalled, so one command brings any *disabled* app back:

```powershell
cd tv-vibehack
$env:TV_HOST='192.168.1.8'
tools\venv\Scripts\python.exe -I tools\tv.py 'pm enable --user 0 com.tcl.messagebox'
```

Run **one `pm` command per call** — chained `pm ...; pm ...` can silently stop after the first. See
what is currently off with `pm list packages -d`. The most likely ones you'd want back:

| Package | What it is |
|---|---|
| `com.tcl.waterfall.overseas` | **TCL Channel** — enabled again, in the grid |
| `com.google.android.youtube.tvmusic` | **YouTube Music** — uninstalled for user 0; bring it back with `pm install-existing --user 0 com.google.android.youtube.tvmusic` |
| `com.phlox.tvwebbrowser` | **TV Bro** — this project installed and then removed it; reinstall with `install-apk.ps1` if you want a browser again |
| `com.tcl.browser` | TCL BrowseHere — removed for user 0; return it with `pm install-existing --user 0 com.tcl.browser` |
| `com.google.android.apps.tv.dreamx` | Google TV ambient-mode screensaver (can show promos) |
| `com.google.android.videos` | Google TV "Movies & TV" store |
| `com.tcl.ttvs` | TCL service **and the firmware/OTA updater** |
| `com.google.android.marvin.talkback` | TalkBack screen reader |
| `com.google.android.tungsten.setupwraith` | first-run setup wizard — **re-enable before a factory reset** |

## Put the TV completely back

```powershell
powershell -ExecutionPolicy Bypass -File tv-setup\restore-tv.ps1
```

That re-enables the packages in the list (including TCL Channel), re-enables and re-selects
`com.google.android.apps.tv.launcherx` as Home, restores the animation scales and the original
screensaver setting, brings TCL BrowseHere back for your user, un-restricts TCL Channel, and prints
what is still disabled. Add `-UninstallLauncher` to also remove the custom launcher.

Two things the script cannot undo by itself, because they were uninstalled rather than disabled:

- **YouTube Music** (system app): `tools\venv\Scripts\python.exe -I tools\tv.py 'pm install-existing --user 0 com.google.android.youtube.tvmusic'`
  It is also listed in `hidden_apps`, so it stays out of the grid even after restoring.
- **TV Bro** (installed by this project): remove it with `pm uninstall com.phlox.tvwebbrowser`, or
  reinstall it with `tv-setup\install-apk.ps1` if you want a browser again.

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

Applied and verified on the TV (`docs\screensaver.png`): it is `com.gathin.tvhome/.ClockDream`.
Re-apply it any time with `tv-setup\set-clock-screensaver.ps1`. It uses the same Schibsted Grotesk
font as Home, and everything is drawn with vectors.

- a **centred 360 dp analog dial** — thin rim, twelve hour marks, hour and minute hands, and a warm
  second hand — above the greeting, date and weather. The weather line uses the same temperature,
  plain-language condition and AQI as Home;
- two soft radial glows that drift apart, with a palette that follows the time of day — teal/green in
  the morning, cool blue/teal in the afternoon, warm orange/pink in the evening, deep indigo at night;
- the whole block sways ±16 × ±10 dp on a **3×3 cycle** once a minute, so the static pixels move
  without the composition leaving the centre;
- each move is a short fade on the minute tick, and the glows breathe on an 8-second cross-fade.

**What it costs.** The dial is a Canvas view: a handful of `Paint` objects, no bitmaps, no extra font.
While the screensaver is on it redraws its 360 dp circle once a second for the second hand —
negligible on this chip, but it is the one thing here that is not literally idle. To get back to zero,
remove the second hand from `AnalogClockView.onDraw` and the per-second tick with it.

### Optional photo slideshow

There is a photo layer behind the clock, **off by default**. It downloads a random
[Lorem Picsum](https://picsum.photos) photo (Unsplash-sourced) at the screen's resolution and changes
it every 10 minutes, dimmed under a scrim so the clock stays readable.

```xml
<string name="ambient_photos" translatable="false">off</string>   <!-- "on" to enable -->
<string name="ambient_photo_url" translatable="false">https://picsum.photos/{w}/{h}?random={n}</string>
<string name="ambient_photo_minutes" translatable="false">10</string>
```

**The honest cost**, which is why it ships off: the dream must hold one decoded bitmap while it runs
— **1.8 MB at 720p, 4.0 MB at 1080p** (`RGB_565`) — and briefly a second one while the new photo
decodes. It also uses roughly 150–400 KB of network per change. The APK grows by about 1 KB when the
code is present. Home is unaffected, and with the switch off the bitmap is never allocated.

Enable it, rebuild, and it appears on the next screensaver:

```powershell
powershell -ExecutionPolicy Bypass -File tv-setup\rebuild-launcher.ps1
```

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
