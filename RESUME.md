# tv-vibehack — TCL G13 progress / RESUME

Last updated: 19 October 2026, after the power cut. **The job is done** — see "DONE (resumed)" below.
Day-to-day instructions now live in `tv-setup\README.md`; this file is the build/change log.

## Device
- TCL Google TV, `ro.product.device=G13`, MediaTek mt5867, Android 14 (SDK 34), ~950 MB RAM (`ro.config.low_ram=true`)
- IP **192.168.1.8:5555** (Wi-Fi SSID `204_5G`; MAC address removed — it identifies the hardware)
- Stock launcher: `com.google.android.apps.tv.launcherx` (Google TV)
- Live TV app: `com.mediatek.wwtv.tvcenter` | Inputs: HDMI 1, HDMI 2, AV, VGA, tuner
- ADB "Allow USB debugging?" was already accepted (key at `tv-vibehack/tools/adbkey`)

## Local paths
- Project root: `C:\Users\autom\OneDrive\Documents\Android_TV_Debloater`
- Repo (git clone): `Android_TV_Debloater\tv-vibehack`
- Local-only docs/scripts: `Android_TV_Debloater\tv-setup`
- Python ADB venv: `tv-vibehack\tools\venv\Scripts\python.exe`
- JDK 17: `C:\Users\autom\dev\jdk-17.0.20.1+1`
- Android SDK: `C:\Users\autom\dev\android-sdk` (platform 34, build-tools 34.0.0, platform-tools)
- Backup (untouched, pre-change): `tv-vibehack\backup\2026-10-19_124247\`
  - Originals to remember: animation scales 0.5 / 0.5 / null; `secure screensaver_components` was **null** (we set `com.android.dreams.basic/.Colors`)
- Screen captures so far: `..\home_before.png`, `shot_b1.png`, `shot_b2.png`, `shot_b3.png`, `launcher_1.png`

## DONE
1. Connected over ADB; full backup written (packages, getprop, all settings, HOME resolve, wm size/density).
2. **Debloat applied and verified in 3 batches** (23 packages disabled). Full list:
   `tv-vibehack\examples\tcl-g13-google-tv\debloat.txt`
   - Batch 1 (TCL): tcl.ttvs, tcl.logkit, tcl.partnercustomizer, tcl.messagebox, tcl.waterfall.overseas, tcl.guard, tcl.keyhelp, tpa.kb, mstar.android.tv.disclaimercustomization
   - Batch 2 (Google/ads/demo/wizards): android.autoinstalls.config.google.gtvpai, fusion.android.tv.demo, mediatek.MtkDemo, mediatek.wwtv.setupwizard, mediatek.wwtv.setup.customization, google.android.feedback, android.tv.feedbackconsent, google.android.apps.tv.dreamx
   - Batch 3 (unused apps + ad/telemetry frameworks): google.android.videos, google.android.play.games, google.android.marvin.talkback, android.adservices.api, android.ondevicepersonalization.services, android.federatedcompute.services
   - Kept on purpose: MMP media player, TCL Browser, Assistant (katniss), Chromecast (mediashell + tv.inputplayer), screen mirroring, Google TV phone remote.
3. **Launcher built**: `launcher\app\build\outputs\apk\release\app-release.apk` (241 KB), config edited
   (`launcher\app\src\main\res\values\config.xml`):
   - dock order: YouTube, Apple TV, Disney+ Hotstar, Prime Video
   - hidden tiles: `com.mediatek.wwtv.tvcenter`, `com.tcl.suspension`, `com.android.tv.settings`
4. **Launcher installed** (`pm install -r` → Success) as `com.gathin.tvhome`, launched OK (`am start` Status: ok),
   `set-home-activity` returned Success.

## DONE (resumed after the power cut)

5. **HOME switched.** Ran: start launcher → `set-home-activity` → disable `com.google.android.tungsten.setupwraith`
   → disable `com.google.android.apps.tv.launcherx` → `set-home-activity` again. Resolve now returns
   **`com.gathin.tvhome/.MainActivity`** (was `launcherx`). 24 packages now disabled (the 23 above + setupwraith),
   plus launcherx via the Home switch.
   Safety net (unused): `pm enable --user 0 com.google.android.apps.tv.launcherx` then re-set home.
6. **Verified by screenshot + injected D-pad keys** (`launcher_home.png`, `launcher_fresh.png`, `nav_down.png`,
   `nav_up1/2.png`, `nav_inputs.png`, `nav_wifi.png`, `tb_1..9.png`):
   - Home key lands on the launcher; cold start renders correctly (an early capture looked wrong only because it
     caught a stale scrolled state — restart the launcher and it is fine).
   - Down moves exactly one row and scrolls the grid up; Up returns to the dock; Up from the dock reaches the pills;
     both ends of the pill row hold (no fall-through); Down from the pills returns to the dock.
   - Focused tile scales up, shows its name, one glass highlight; hero name + ambient glow sampled from the app artwork.
   - Inputs dropdown: HDMI 1, HDMI 2, Composite (AV), Live TV. Settings pill works. Inputs pill hidden apps confirmed.
   - Wi-Fi pill shows `204_5G`. **Gotcha:** the grant must be `pm grant --user 0 ...`; the first attempt reported
     nothing but `dumpsys` showed `granted=false`. Fixed and re-verified.
   - Enter on a dock tile launches the app; Home returns to the launcher.
7. **InputKeyService: not enabled** — the user doesn't use the remote's Input button. Noted in `tv-setup\README.md`.
8. **Deliverables written** to `tv-setup\`:
   - `README.md` — what changed, everyday use, re-enable any single app, full restore, dock order, wallpaper/background,
     troubleshooting.
   - `restore-tv.ps1` — re-enables all 24 packages, restores launcherx + animation scales (0.5 / 0.5 / auto) and
     `settings delete secure screensaver_components`; `-UninstallLauncher` removes the custom launcher.
   - `rebuild-launcher.ps1` — sets JAVA_HOME/ANDROID_HOME, rebuilds + reinstalls, then re-grants the location
     permission properly and verifies it.
   - `set-clock-screensaver.ps1` — the one leftover step (below).
9. **Screensaver:** replaced Google TV's promo-capable `dreamx` ambient mode with the launcher's drifting **Clock**
   (user's choice) — still to be applied, see below.

## LEFT OVER (one command, optional)

The TV went off the network (idle Wi-Fi doze) before this ran, so the screensaver is still
`com.android.dreams.basic/.Colors`, not the launcher's Clock. When the TV is on:

```powershell
powershell -ExecutionPolicy Bypass -File tv-setup\set-clock-screensaver.ps1
```


## Reminder for the user
- USB debugging should be switched OFF on the TV once we are finished.
- Nothing is uninstalled; `tools\restore.ps1 examples\tcl-g13-google-tv\debloat.txt com.google.android.apps.tv.launcherx` undoes the debloat.
