# How TV Home is put together

TV Home is a standalone Android TV launcher (`com.gathin.tvhome`), not a patch to Google TV. It is
plain Java on top of AndroidX RecyclerView + DynamicAnimation; there are no networking, JSON, image or
animation libraries. Reading this first makes every later change a small, local edit.

## The pieces

| File | What it does | Roughly |
|---|---|---|
| `MainActivity.java` | Home screen: loads config, builds the grid, focus/D-pad rules, hero title, ambient glow, glass highlight, pills, weather, clock ticks | 900 lines |
| `AppAdapter.java` | One view per app, the dock decoration, hidden apps and the requested order | 500 lines |
| `GlassView.java` | The single moving glass highlight (drawn, no blur) | 300 lines |
| `MascotView.java` | The 26 dp pixel companion: a `Canvas` sprite, no bitmap and no idle animation | 70 lines |
| `Config.java` | Reads `config.xml`, then overlays a pushed `config.json` theme | 200 lines |
| `ClockDream.java` | The screensaver: placement, palette, glows, weather, optional photos | 300 lines |
| `AnalogClockView.java` | The dial, drawn with Canvas, one tick per second | 180 lines |
| `Weather.java` | Open-Meteo temperature + condition text, plus the US AQI, cached for 10 minutes | 190 lines |
| `InputKeyService.java` | Opt-in: the remote's Input button opens the switcher | 100 lines |
| `HomeKeyService.java` | Opt-in: fixes Home on remotes that send `KEYCODE_ALL_APPS` | 80 lines |

Everything visual is a vector, a colour or a font. The APK is ~250 KB, mostly the Latin subset of
Schibsted Grotesk (104 KB).

## Home, end to end

1. `onCreate` loads `Config` (XML now, JSON overlay later), finds the views, adds the dock decoration
   and the adapter.
2. `AppAdapter.reload()` asks `PackageManager` for every app with a `LEANBACK_LAUNCHER` entry, removes
   `hidden_apps`, then sorts: `dock_apps` first (they occupy the dock slots), `app_order` next, and
   everything else alphabetically.
3. `MainActivity` owns every key: Up/Down move one row, Up from the dock reaches the pills, Left/Right
   stop at the ends. In reorder mode, OK picks an app up and drops it, the D-pad moves it, and Back
   finishes; the dock stays fixed. `GridLayoutManager` does not get to decide.
4. Focus moves the glass (`GlassView`) with a spring, hops the pixel mascot (`MascotView`) to the
   focused tile, fades the hero title in, and pulls the ambient glow's colour from the focused tile's
   icon.
5. `onResume` reloads the grid and returns focus to the last package (or slot) from the static fields.
6. The minute tick recomputes the greeting and the clock; the weather tick refreshes at most every
   10 minutes.

## The screensaver

`ClockDream` is a `DreamService`. The system starts it when the TV has been idle for
`screen_off_timeout`.

- `dream_clock.xml` is four layers: optional photo, scrim, two radial glows, then the centred content.
- `placeClock()` centres the block and sways it ±16 × ±10 dp on a 3×3 cycle, once a minute.
- `tick` (60 s) moves the block and cross-fades the glow colour.
- `AnalogClockView` runs its own 1 s handler while attached and stops on detach.
- `weather` refreshes on start and every 10 minutes; the same line (condition text and AQI) is shown
  on Home and on the screensaver.
- `photoTick` only runs when `ambient_photos` is `on`; it downloads one image, fades it in, and
  recycles the previous bitmap.

## Configuration

Every user-facing knob is in
`tv-vibehack/launcher/app/src/main/res/values/config.xml`:

| Key | Default | Meaning |
|---|---|---|
| `dock_apps` | YouTube, Apple TV, Hotstar, Prime | Dock order; missing packages are skipped |
| `app_order` | Xstream, TCL Channel, Netflix, ZEE5, Stremio, Spotify | Grid order after the dock; unlisted apps follow alphabetically |
| `hidden_apps` | tuner, TCL panel, Settings, YouTube Music, TV Bro | Never shown as tiles |
| `columns` | `4` | Tiles per row; the dock uses the same count |
| `rows_below_dock` | `0` | Grid rows left visible under the dock |
| `user_name` | `Vedarth` | Appended to the greeting; empty for none |
| `weather_city` | `Jaipur` | Label; empty hides weather and the request |
| `weather_lat` / `weather_lon` | Jaipur | Decimal coordinates |
| `weather_units` | `celsius` | or `fahrenheit` |
| `weather_aqi` | `true` | Adds the US AQI to the weather line (one extra tiny request) |
| `ambient_photos` | `off` | `on` adds the downloaded photo layer |
| `ambient_photo_url` | Picsum | `{w}`/`{h}` screen size, `{n}` cache-buster |
| `ambient_photo_minutes` | `10` | Photo change interval, 2–120 |
| `live_tv_package` | MediaTek tuner | Live TV row at the end of Inputs |
| `shamsi_date` | `false` | Solar Hijri date above the Gregorian one |
| `vpn_app` | empty | Package the Wi-Fi pill opens; empty → Wi-Fi settings |
| `show_home_settings_shortcut` | `false` | Adds a "Default home" tile where the page exists |

`Config.java` reads these directly. The optional JSON theme (`tools/theme.ps1`) can override the
subset it knows about; XML is the source of truth.

## Adding a feature

### A new app tile

Usually nothing to do: install the app, press Home. To pin it to the dock, add its package to
`dock_apps`; to choose where it sits under the dock, add it to `app_order`; to keep it out of the
grid, add it to `hidden_apps`. Find the package with:

```powershell
tools\venv\Scripts\python.exe -I tools\tv.py 'pm list packages'
```

### A new line on Home

1. Add a `TextView` next to `@id/weather` in `activity_main.xml` (same font, 12–14 sp, 8C white).
2. Find it in `MainActivity.onCreate`.
3. Update it from an existing tick (`minuteTick`) or add a `Runnable` that posts itself, like
   `weatherTick`.
4. Remove the callback in `onPause` — otherwise it keeps running while an app is open.

### A new screensaver element

1. Add the view to `dream_clock.xml` **inside** `@id/dreamContent` so it drifts with the clock.
2. Bind it in `ClockDream.onAttachedToWindow`.
3. If it animates, use a `Runnable` on the dream's `handler` and remove it in both
   `onDreamingStopped` and `onDetachedFromWindow`.
4. If it holds memory (a bitmap, a listener), release it in `onDetachedFromWindow` — the dream is
   destroyed every time the TV wakes.

### A data source (the Weather pattern)

`Weather.java` is the template for anything fetched:

- one background thread, result posted to the main `Looper`;
- a `static` last-good value so a returning screen never flashes empty;
- a freshness guard (10 minutes) so re-entering Home does not re-request;
- failure returns the cached value and never throws;
- the AQI pattern: reuse the same thread and cache, add one small GET only when the feature is on,
  and append the result to the line as plain text. No image and no second cache.

Keep it to `HttpURLConnection` + `org.json`; adding Retrofit/OkHttp would cost more APK than the
feature.

### The pixel companion

`MascotView` is a plain `View` that draws a small character from rectangles on a `Canvas`. There is
no bitmap, no drawable and no idle animation: `MainActivity.positionMascot()` only runs when focus
moves (a 300 ms arc) and while the grid scrolls. To change the look, edit the `SPRITE` matrix and the
three colours in `MascotView`. To change where it sits, edit `positionMascot()`.

### Changing the clock style

`AnalogClockView` is one view in `dream_clock.xml`. To go back to digits, replace that element with
the two `TextClock`s from git history (`git log -- launcher/.../dream_clock.xml`), or wrap both in a
container and toggle visibility in `ClockDream`. Nothing else depends on the type of clock.

## Cost rules

Before adding anything, answer these:

| Resource | Budget | How to check |
|---|---|---|
| APK | stay under ~300 KB | `Get-Item ...\app-release.apk`; fonts and images dominate |
| Home RAM | no bitmap, no polling, no services, no listeners that outlive `onPause` | `dumpsys meminfo com.gathin.tvhome` |
| Dream RAM | no bitmap unless the feature is about images | one 1280×720 `RGB_565` bitmap = 1.8 MB; 1080p = 4.0 MB |
| CPU | nothing continuous except the dream's second hand | `top -n 1 | grep tvhome` |

A vector, a colour or a font glyph is free. A bitmap is not. A video is never worth it here.

The pixel mascot and the AQI are examples of features that stay on the right side of the line: the
mascot is Canvas drawing plus one short animation per keypress, and the AQI is a short string from a
tiny JSON response on the existing weather thread.

## Build and test loop

```powershell
# from the repo root
powershell -ExecutionPolicy Bypass -File tv-setup\rebuild-launcher.ps1
```

That script deletes `app\build` (OneDrive locks Gradle's incremental resource merge), builds the
release APK, pushes it, installs, grants location and restarts Home. The manual equivalent is in
`tv-setup\README.md`.

To test the screensaver without waiting ten minutes:

```powershell
tools\venv\Scripts\python.exe -I tools\tv.py 'settings put system screen_off_timeout 15000'
# wait 20–30 s, then capture
tools\venv\Scripts\python.exe -I tools\tv.py 'screencap -p /sdcard/dream.png'
```

Restore `600000` when done.

## Where not to touch

`tv-vibehack/` is a git subtree of upstream. Changes there are fine, but keep them in the launcher
folder and commit them separately from the TCL-specific docs, so a future `git subtree pull` stays
readable.
