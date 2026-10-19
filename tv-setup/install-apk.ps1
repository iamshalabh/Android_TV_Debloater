<#
    Install an APK on the TV, either from a file on this PC or straight from a URL
    (a GitHub release asset, F-Droid, an APKMirror direct link, ...).

    Usage:
        powershell -ExecutionPolicy Bypass -File install-apk.ps1 C:\Downloads\app.apk
        powershell -ExecutionPolicy Bypass -File install-apk.ps1 https://github.com/user/repo/releases/download/v1.2/app.apk
        powershell -ExecutionPolicy Bypass -File install-apk.ps1 <path-or-url> -TvHost 192.168.1.8

    Why a script: the TV has an APK installer but no file manager or browser download UI, so
    "open the file on the TV" isn't a thing here. This pushes the APK to /data/local/tmp and
    installs it with pm, which is the same mechanism the launcher build uses.

    Notes
    - Use the TV-oriented build of an app (leanback / Android TV), not the phone build.
    - App bundles that ship as .apks / a folder of splits don't work here; look for a universal APK.
    - Play Protect may scan the first install. If it blocks, allow it once on screen, or run
      tv-vibehack\tools\tv.py 'settings put global verifier_verify_adb_installs 0' (less safe).
#>
param(
    [Parameter(Mandatory = $true, Position = 0)][string]$Apk,
    [string]$TvHost = '192.168.1.8'
)
$ErrorActionPreference = 'Stop'
$OutputEncoding = New-Object System.Text.UTF8Encoding $false

$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$repo = (Resolve-Path (Join-Path $here '..\tv-vibehack')).Path
$py   = Join-Path $repo 'tools\venv\Scripts\python.exe'
$tv   = Join-Path $repo 'tools\tv.py'
$push = Join-Path $repo 'tools\b64push.py'
$env:TV_HOST = $TvHost

$local = $Apk
$downloaded = $false
if ($Apk -match '^https?://') {
    $local = Join-Path $env:TEMP ("tvinstall_" + [guid]::NewGuid().ToString('N') + ".apk")
    Write-Host "Downloading $Apk"
    Invoke-WebRequest -Uri $Apk -OutFile $local -UseBasicParsing
    $downloaded = $true
}
if (-not (Test-Path -LiteralPath $local)) { throw "APK not found: $local" }
$sizeMb = [math]::Round((Get-Item -LiteralPath $local).Length / 1MB, 2)
Write-Host "Installing $local ($sizeMb MB) on $TvHost ..."

& $py -I $push $local /data/local/tmp/tvinstall.apk
if ($LASTEXITCODE -ne 0) { throw 'upload failed' }

$out = & $py -I $tv 'pm install -r /data/local/tmp/tvinstall.apk'
Write-Host $out
& $py -I $tv 'rm -f /data/local/tmp/tvinstall.apk' | Out-Null
if ($downloaded) { Remove-Item -LiteralPath $local -Force -ErrorAction SilentlyContinue }

if ("$out" -notmatch 'Success') {
    throw "Install did not report Success. Common causes: the APK targets phones only, it is a split bundle, or the signature clashes with an installed copy (uninstall that first)."
}

Write-Host 'Installed. It appears in the launcher grid automatically.'
Write-Host 'Press Home on the remote if the launcher does not refresh.'
