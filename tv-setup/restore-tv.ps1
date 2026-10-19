<#
    Put the TCL G13 Google TV back the way it was before the debloat.

    Re-enables every package in the debloat list, brings back and re-selects the stock
    Google TV launcher (com.google.android.apps.tv.launcherx), and undoes the handful of
    changes the list does not carry (animation scales, screensaver).

    Nothing is deleted and the custom launcher is left installed unless you pass
    -UninstallLauncher.

    Usage:
        powershell -ExecutionPolicy Bypass -File restore-tv.ps1
        powershell -ExecutionPolicy Bypass -File restore-tv.ps1 -UninstallLauncher
#>
param(
    [string]$TvHost = '192.168.1.8',
    [switch]$UninstallLauncher
)
$ErrorActionPreference = 'Stop'
# Pipe plain UTF-8 to native commands (an added BOM corrupts the first command).
$OutputEncoding = New-Object System.Text.UTF8Encoding $false

$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$repo = (Resolve-Path (Join-Path $here '..\tv-vibehack')).Path
$py   = Join-Path $repo 'tools\venv\Scripts\python.exe'
$tv   = Join-Path $repo 'tools\tv.py'
$list = Join-Path $repo 'examples\tcl-g13-google-tv\debloat.txt'

if (-not (Test-Path $py))   { throw "Python venv not found at $py" }
if (-not (Test-Path $list)) { throw "Debloat list not found at $list" }
$env:TV_HOST = $TvHost

Write-Host "Re-enabling the packages in $list and restoring the stock launcher ..."
& (Join-Path $repo 'tools\restore.ps1') -TvHost $TvHost -List $list -Stock com.google.android.apps.tv.launcherx
if ($LASTEXITCODE -ne 0) { throw 'restore.ps1 failed' }

# The extras that are not in the debloat list. Values are the TV's own from the backup
# taken before any change (tv-vibehack\backup\2026-10-19_124247).
Write-Host 'Restoring the animation scales and screensaver ...'
@(
    'settings put global window_animation_scale 0.5',       # the TV shipped at 0.5, not 1.0
    'settings put global transition_animation_scale 0.5',
    'settings delete global animator_duration_scale',       # was unset
    'settings delete secure screensaver_components',        # was unset; the TV falls back to Google's ambient mode
    'settings put secure screensaver_enabled 1'
) | & $py -I $tv --batch

# Changes made after the first pass: BrowseHere was removed for user 0 and TCL Channel
# was put in the restricted standby bucket. Put both back the way the TV shipped.
Write-Host 'Bringing back the TCL browser and TCL Channel''s normal background state ...'
@(
    'cmd package install-existing --user 0 com.tcl.browser',
    'am set-standby-bucket com.tcl.waterfall.overseas 10'
) | & $py -I $tv --batch

if ($UninstallLauncher) {
    Write-Host 'Uninstalling the custom launcher ...'
    & $py -I $tv 'pm uninstall com.gathin.tvhome'
}

Write-Host ''
Write-Host 'Still disabled (empty means everything came back):'
& $py -I $tv 'pm list packages -d'
Write-Host 'Home now resolves to:'
& $py -I $tv 'cmd package resolve-activity --brief -c android.intent.category.HOME -a android.intent.action.MAIN' | Select-Object -Last 1
Write-Host 'Done. Reboot the TV if a screen looks odd.'
