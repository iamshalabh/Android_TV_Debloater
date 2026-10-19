<#
    Rebuild the custom launcher (com.gathin.tvhome) and install it on the TV.

    Edit tv-vibehack\launcher\app\src\main\res\values\config.xml first if you want to
    change the dock apps, their order, the hidden apps or the grid columns, then run
    this. It always re-uses whatever is in config.xml at build time.

    Usage:
        powershell -ExecutionPolicy Bypass -File rebuild-launcher.ps1
        powershell -ExecutionPolicy Bypass -File rebuild-launcher.ps1 -TvHost 192.168.1.8

    Needs JDK 17 and the Android SDK. If they live somewhere unusual, set JAVA_HOME and
    ANDROID_HOME before running, or edit the two paths below.
#>
param(
    [string]$TvHost = '192.168.1.8'
)
$ErrorActionPreference = 'Stop'
# Pipe plain UTF-8 to native commands (an added BOM corrupts the first command).
$OutputEncoding = New-Object System.Text.UTF8Encoding $false

$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$repo = (Resolve-Path (Join-Path $here '..\tv-vibehack')).Path
$py   = Join-Path $repo 'tools\venv\Scripts\python.exe'
$tv   = Join-Path $repo 'tools\tv.py'

# Toolchain: this machine keeps them under C:\Users\autom\dev.
if (-not $env:JAVA_HOME)    { $env:JAVA_HOME    = 'C:\Users\autom\dev\jdk-17.0.20.1+1' }
if (-not $env:ANDROID_HOME) { $env:ANDROID_HOME = 'C:\Users\autom\dev\android-sdk' }
$env:TV_HOST = $TvHost

Write-Host "Building and installing the launcher on $TvHost ..."
# build-and-install.ps1 asks the TV who owns Home and only disables a stock launcher it
# recognises. With our launcher already the default it reports "none" and changes nothing,
# so this is safe to re-run.
& (Join-Path $repo 'launcher\build-and-install.ps1') -TvHost $TvHost
if ($LASTEXITCODE -ne 0) { throw 'build-and-install.ps1 failed' }

# The TV's Wi-Fi pill shows the network name only if this runtime permission is granted.
# build-and-install.ps1 chains the grant with ';' (which can silently stop after the first
# command) and omits --user 0, so grant it properly here and prove it took.
Write-Host 'Granting the location permission (needed for the Wi-Fi network name) ...'
@(
    'pm grant --user 0 com.gathin.tvhome android.permission.ACCESS_FINE_LOCATION',
    'pm grant --user 0 com.gathin.tvhome android.permission.ACCESS_COARSE_LOCATION'
) | & $py -I $tv --batch
$granted = & $py -I $tv 'dumpsys package com.gathin.tvhome' | Select-String 'ACCESS_FINE_LOCATION: granted=true'
if (-not $granted) { Write-Warning 'ACCESS_FINE_LOCATION still not granted; the Wi-Fi pill will just say "Wi-Fi".' }

# Restart so the launcher picks everything up.
Write-Host 'Restarting the launcher ...'
@(
    'am force-stop com.gathin.tvhome',
    'am start -n com.gathin.tvhome/.MainActivity'
) | & $py -I $tv --batch

Write-Host 'Home resolves to:'
& $py -I $tv 'cmd package resolve-activity --brief -c android.intent.category.HOME -a android.intent.action.MAIN' | Select-Object -Last 1
Write-Host 'Done. Press Home on the remote to see it.'
