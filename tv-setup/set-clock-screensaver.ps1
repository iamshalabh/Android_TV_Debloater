<#
    The one step that was left over: point the TV's idle screensaver at the launcher's
    own drifting Clock instead of Google TV's ambient mode (which was disabled because it
    can show promos).

    Run this whenever the TV is back on the network:
        powershell -ExecutionPolicy Bypass -File tv-setup\set-clock-screensaver.ps1

    Nothing else depends on it - the launcher and the debloat are already finished.
#>
param(
    [string]$TvHost = '192.168.1.8'
)
$ErrorActionPreference = 'Stop'
$OutputEncoding = New-Object System.Text.UTF8Encoding $false

$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$repo = (Resolve-Path (Join-Path $here '..\tv-vibehack')).Path
$py   = Join-Path $repo 'tools\venv\Scripts\python.exe'
$tv   = Join-Path $repo 'tools\tv.py'
$env:TV_HOST = $TvHost

Write-Host "Setting the Clock screensaver on $TvHost ..."
@(
    'settings put secure screensaver_components com.gathin.tvhome/.ClockDream',
    'settings put secure screensaver_enabled 1'
) | & $py -I $tv --batch

Write-Host 'Screensaver is now:'
& $py -I $tv 'settings get secure screensaver_components'
Write-Host 'It starts after the TV has been idle for screen_off_timeout milliseconds:'
& $py -I $tv 'settings get system screen_off_timeout'
Write-Host 'Done.'
