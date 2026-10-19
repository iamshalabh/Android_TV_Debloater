<#
    Install TV Home on any Android TV, make it the home screen and set its screensaver.
    This is the portable script: it uses stock `adb` on PATH, not this repo's Python client.

    Usage:
        powershell -ExecutionPolicy Bypass -File install-any-tv.ps1 -TvHost 192.168.1.50
        powershell -ExecutionPolicy Bypass -File install-any-tv.ps1 -TvHost 192.168.1.50 -Apk C:\build\app-release.apk
        powershell -ExecutionPolicy Bypass -File install-any-tv.ps1 -TvHost 192.168.1.50 -SkipDream
        powershell -ExecutionPolicy Bypass -File install-any-tv.ps1 -TvHost 192.168.1.50 -Restore -StockHome com.google.android.apps.tv.launcherx/.LauncherActivity

    Notes:
    - Enable Developer options -> USB debugging / Wireless debugging on the TV first.
    - Accept the RSA prompt on the TV when it appears.
    - TV Home is `com.gathin.tvhome`. Nothing here uninstalls a system app.
    - On Android 8-9 `cmd package set-home-activity` may not exist; press Home and pick TV Home.
#>
param(
    [Parameter(Mandatory = $true)][string]$TvHost,
    [string]$Apk = '',
    [switch]$SkipHome,
    [switch]$SkipDream,
    [switch]$Restore,
    [string]$StockHome = ''
)
$ErrorActionPreference = 'Stop'

if (-not (Get-Command adb -ErrorAction SilentlyContinue)) {
    throw 'adb not found. Install Android Platform Tools and add its folder to PATH.'
}

function Invoke-Adb {
    param([string[]]$Command)
    $output = & adb -s $TvHost @Command 2>&1
    $code = $LASTEXITCODE
    foreach ($line in $output) { Write-Host "  $line" }
    return $code
}

Write-Host "Connecting to $TvHost ..."
if ($TvHost -match ':\d+$') {
    & adb connect $TvHost | ForEach-Object { Write-Host "  $_" }
}
$devices = & adb devices
if (-not ($devices -match [regex]::Escape($TvHost))) {
    throw "adb does not list $TvHost. Check the IP, that debugging is on, and accept the prompt on the TV."
}

if ($Restore) {
    Write-Host 'Removing TV Home ...'
    Invoke-Adb @('uninstall', 'com.gathin.tvhome') | Out-Null
    if ($StockHome) {
        Write-Host "Pointing Home back at $StockHome ..."
        Invoke-Adb @('shell', 'cmd', 'package', 'set-home-activity', '--user', '0', $StockHome) | Out-Null
    } else {
        Write-Host 'No -StockHome given. Press Home and choose the TV''s own launcher, or re-run with'
        Write-Host '  -StockHome <package/activity>'
        Write-Host '  Find it with: adb shell cmd package resolve-activity -c android.intent.category.HOME'
    }
    Write-Host 'Done. TV Home is removed; system apps were never touched.'
    exit 0
}

$here = Split-Path -Parent $MyInvocation.MyCommand.Path
if (-not $Apk) {
    $Apk = Join-Path $here '..\tv-vibehack\launcher\app\build\outputs\apk\release\app-release.apk'
}
if (-not (Test-Path -LiteralPath $Apk)) {
    throw "APK not found: $Apk`nBuild it first: tv-vibehack\launcher\gradlew.bat -p tv-vibehack\launcher assembleRelease"
}
$Apk = (Resolve-Path -LiteralPath $Apk).Path
$sizeMb = [math]::Round((Get-Item -LiteralPath $Apk).Length / 1MB, 2)
Write-Host "Installing $Apk ($sizeMb MB) ..."
if ((Invoke-Adb @('install', '-r', $Apk)) -ne 0) { throw 'Install failed.' }

# The Wi-Fi pill can only read the SSID with location granted. Harmless if the TV refuses.
Write-Host 'Granting location (for the Wi-Fi name) ...'
Invoke-Adb @('shell', 'pm', 'grant', 'com.gathin.tvhome', 'android.permission.ACCESS_FINE_LOCATION') | Out-Null

if (-not $SkipHome) {
    Write-Host 'Making TV Home the home screen ...'
    if ((Invoke-Adb @('shell', 'cmd', 'package', 'set-home-activity', '--user', '0',
                'com.gathin.tvhome/.MainActivity')) -ne 0) {
        Write-Host 'Could not set Home over adb. Press Home on the remote and choose TV Home -> Always.'
    }
}

if (-not $SkipDream) {
    Write-Host 'Selecting the TV Home screensaver ...'
    Invoke-Adb @('shell', 'settings', 'put', 'secure', 'screensaver_components',
            'com.gathin.tvhome/.ClockDream') | Out-Null
    Invoke-Adb @('shell', 'settings', 'put', 'secure', 'screensaver_enabled', '1') | Out-Null
}

Write-Host 'Opening Home ...'
Invoke-Adb @('shell', 'input', 'keyevent', '3') | Out-Null

Write-Host ''
Write-Host 'Done. Next:'
Write-Host '  1. Press Home a few times, open an app, open Inputs and Settings.'
Write-Host '  2. Only after that works, disable the stock launcher (never uninstall it):'
Write-Host '       adb -s <host> shell pm disable-user --user 0 <stock-launcher-package>'
Write-Host '  3. Different dock apps or weather city? Edit config.xml and rebuild. See docs/PORTING.md.'
