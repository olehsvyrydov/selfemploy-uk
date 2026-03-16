# =============================================================================
# UK Self-Employment Manager - Installation Script (Windows)
# =============================================================================
#
# Usage:
#   .\install.ps1              Build and run the application
#   .\install.ps1 -Build       Build only (no run)
#   .\install.ps1 -Package     Build and create native installer (.msi/.exe)
#   .\install.ps1 -Install     Download and install latest release (no Java/Maven needed)
#   .\install.ps1 -Check       Check prerequisites only
#   .\install.ps1 -Help        Show help message
#
# Note: You may need to allow script execution first:
#   Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
#
# =============================================================================

param(
    [switch]$Build,
    [switch]$Package,
    [switch]$Install,
    [switch]$Check,
    [switch]$Help
)

# --- Configuration -----------------------------------------------------------

$RequiredJavaVersion = 21
$RequiredMavenMajor = 3
$RequiredMavenMinor = 6
$AppName = "UK Self-Employment Manager"
$GitHubRepo = "olehsvyrydov/selfemploy-uk"
$GitHubApi = "https://api.github.com/repos/$GitHubRepo/releases/latest"

# --- Helper Functions ---------------------------------------------------------

function Write-Info    { param($msg) Write-Host "[INFO]  $msg" -ForegroundColor Cyan }
function Write-Success { param($msg) Write-Host "[OK]    $msg" -ForegroundColor Green }
function Write-Warn    { param($msg) Write-Host "[WARN]  $msg" -ForegroundColor Yellow }
function Write-Err     { param($msg) Write-Host "[ERROR] $msg" -ForegroundColor Red }

function Show-Help {
    @"
$AppName - Installation Script (Windows)

Usage:
  .\install.ps1              Build and run the application
  .\install.ps1 -Build       Build only (no run)
  .\install.ps1 -Package     Build and create native installer (.msi/.exe)
  .\install.ps1 -Install     Download and install latest release (no Java/Maven needed)
  .\install.ps1 -Check       Check prerequisites only
  .\install.ps1 -Help        Show this help message

Developer mode (default):
  Requires Java $RequiredJavaVersion+ and Maven $RequiredMavenMajor.$RequiredMavenMinor+. Builds from source.

Install mode (-Install):
  Downloads the pre-built .msi installer from GitHub Releases.
  No Java or Maven required.

Note: If you get a script execution error, run:
  Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
"@
}

# --- Prerequisite Checks -----------------------------------------------------

function Test-Java {
    Write-Info "Checking Java installation..."

    try {
        $javaOutput = & java -version 2>&1 | Select-Object -First 1
    }
    catch {
        Write-Err "Java is not installed or not in PATH."
        Show-JavaInstall
        return $false
    }

    if (-not $javaOutput) {
        Write-Err "Java is not installed or not in PATH."
        Show-JavaInstall
        return $false
    }

    if ($javaOutput -match '"(\d+)') {
        $javaVersion = [int]$Matches[1]
    }
    else {
        Write-Err "Could not determine Java version."
        Show-JavaInstall
        return $false
    }

    if ($javaVersion -ge $RequiredJavaVersion) {
        Write-Success "Java $javaVersion found (required: $RequiredJavaVersion+)"
    }
    else {
        Write-Err "Java $javaVersion found, but Java $RequiredJavaVersion+ is required."
        Show-JavaInstall
        return $false
    }

    # Check for JDK (javac)
    try {
        $null = & javac -version 2>&1
        Write-Success "JDK confirmed (javac available)"
    }
    catch {
        Write-Err "JRE detected but JDK is required (javac not found)."
        Show-JavaInstall
        return $false
    }

    return $true
}

function Test-Maven {
    Write-Info "Checking Maven installation..."

    try {
        $mvnOutput = & mvn --version 2>&1 | Select-Object -First 1
    }
    catch {
        Write-Err "Maven is not installed or not in PATH."
        Show-MavenInstall
        return $false
    }

    if (-not $mvnOutput) {
        Write-Err "Maven is not installed or not in PATH."
        Show-MavenInstall
        return $false
    }

    if ($mvnOutput -match '(\d+)\.(\d+)\.(\d+)') {
        $mvnMajor = [int]$Matches[1]
        $mvnMinor = [int]$Matches[2]
        $mvnVersion = "$($Matches[1]).$($Matches[2]).$($Matches[3])"
    }
    else {
        Write-Err "Could not determine Maven version."
        Show-MavenInstall
        return $false
    }

    if (($mvnMajor -gt $RequiredMavenMajor) -or
        (($mvnMajor -eq $RequiredMavenMajor) -and ($mvnMinor -ge $RequiredMavenMinor))) {
        Write-Success "Maven $mvnVersion found (required: $RequiredMavenMajor.$RequiredMavenMinor+)"
    }
    else {
        Write-Err "Maven $mvnVersion found, but $RequiredMavenMajor.$RequiredMavenMinor+ is required."
        Show-MavenInstall
        return $false
    }

    return $true
}

function Show-JavaInstall {
    Write-Host ""
    Write-Warn "Install Java $RequiredJavaVersion+ (Temurin recommended):"
    Write-Host "  winget:     winget install EclipseAdoptium.Temurin.21.JDK"
    Write-Host "  Chocolatey: choco install temurin21"
    Write-Host "  Scoop:      scoop install temurin21-jdk"
    Write-Host "  Manual:     https://adoptium.net/temurin/releases/"
    Write-Host ""
}

function Show-MavenInstall {
    Write-Host ""
    Write-Warn "Install Maven $RequiredMavenMajor.$RequiredMavenMinor+:"
    Write-Host "  winget:     winget install Apache.Maven"
    Write-Host "  Chocolatey: choco install maven"
    Write-Host "  Scoop:      scoop install maven"
    Write-Host "  Manual:     https://maven.apache.org/download.cgi"
    Write-Host ""
}

function Test-Prerequisites {
    Write-Host ""
    Write-Host "============================================="
    Write-Host "  $AppName"
    Write-Host "  Prerequisite Check"
    Write-Host "============================================="
    Write-Host ""

    $javaOk = Test-Java
    $mavenOk = Test-Maven

    Write-Host ""

    if ($javaOk -and $mavenOk) {
        Write-Success "All prerequisites met!"
        return $true
    }
    else {
        Write-Err "Some prerequisites are missing. Please install them and re-run this script."
        return $false
    }
}

# --- Build & Run --------------------------------------------------------------

function Start-Build {
    Write-Info "Building $AppName..."
    Write-Host ""

    & mvn clean install -DskipTests --no-transfer-progress
    if ($LASTEXITCODE -ne 0) {
        Write-Err "Build failed!"
        exit 1
    }

    Write-Host ""
    Write-Success "Build completed successfully!"
}

function Start-App {
    Write-Info "Starting $AppName..."
    Write-Host ""

    & mvn -pl app javafx:run
}

function New-Installer {
    Write-Info "Creating native installer for Windows..."
    Write-Host ""

    & mvn -pl app -Ppackage jpackage:jpackage
    if ($LASTEXITCODE -ne 0) {
        Write-Err "Installer creation failed!"
        Write-Warn "Make sure WiX Toolset is installed for .msi generation:"
        Write-Host "  winget:     winget install FireGiant.WiX"
        Write-Host "  Chocolatey: choco install wixtoolset"
        Write-Host "  Manual:     https://wixtoolset.org/docs/wix3/"
        exit 1
    }

    Write-Host ""
    Write-Success "Native installer created!"
    Write-Info "Check: app\target\installer\"
    Write-Host ""
    Write-Info "Install the .msi or .exe package from that directory."
}

function Initialize-Env {
    if (-not (Test-Path ".env")) {
        if (Test-Path ".env.example") {
            Write-Warn "No .env file found. Creating from .env.example..."
            Copy-Item ".env.example" ".env"
            Write-Host ""
            Write-Warn "Please edit .env with your HMRC credentials before using HMRC features."
            Write-Warn "See: https://developer.service.hmrc.gov.uk/developer/registration"
            Write-Host ""
        }
    }
    else {
        Write-Success ".env file already exists"
    }
}

# --- Install from Release -----------------------------------------------------

function Install-Release {
    Write-Info "Downloading latest release from GitHub..."

    try {
        $release = Invoke-RestMethod -Uri $GitHubApi -ErrorAction Stop
    }
    catch {
        Write-Err "Could not fetch latest release. Check your internet connection."
        Write-Err "URL: $GitHubApi"
        exit 1
    }

    $version = $release.tag_name -replace '^v', ''
    if (-not $version) {
        Write-Err "Could not determine latest version."
        exit 1
    }

    Write-Info "Latest version: $version"

    # Find the .msi asset
    $msiAsset = $release.assets | Where-Object { $_.name -like '*.msi' } | Select-Object -First 1

    if (-not $msiAsset) {
        Write-Err "No .msi installer found in the latest release."
        Write-Err "Please download manually from: https://github.com/$GitHubRepo/releases/latest"
        exit 1
    }

    $downloadUrl = $msiAsset.browser_download_url
    $fileName = "SelfEmploy-$version.msi"
    $tempDir = Join-Path $env:TEMP "selfemploy-install"
    $filePath = Join-Path $tempDir $fileName

    # Create temp directory
    if (-not (Test-Path $tempDir)) {
        New-Item -ItemType Directory -Path $tempDir -Force | Out-Null
    }

    Write-Info "Downloading $fileName..."

    try {
        Invoke-WebRequest -Uri $downloadUrl -OutFile $filePath
    }
    catch {
        Write-Err "Download failed: $_"
        exit 1
    }

    if (-not (Test-Path $filePath)) {
        Write-Err "Download failed - file not found."
        exit 1
    }

    Write-Success "Downloaded: $fileName"

    Write-Info "Installing..."

    try {
        $process = Start-Process msiexec.exe -ArgumentList "/i", "`"$filePath`"", "/passive", "/norestart" -Wait -PassThru
        if ($process.ExitCode -ne 0) {
            Write-Err "Installer exited with code $($process.ExitCode)"
            Write-Err "Try running the .msi manually: $filePath"
            exit 1
        }
    }
    catch {
        Write-Err "Installation failed: $_"
        Write-Err "Try running the .msi manually: $filePath"
        exit 1
    }

    # Cleanup
    Remove-Item -Path $tempDir -Recurse -Force -ErrorAction SilentlyContinue

    Write-Host ""
    Write-Success "$AppName $version installed successfully!"
    Write-Info "Launch SelfEmploy from the Start Menu or desktop shortcut."
}

# --- Main ---------------------------------------------------------------------

if ($Help) {
    Show-Help
    exit 0
}

if ($Check) {
    $result = Test-Prerequisites
    if ($result) { exit 0 } else { exit 1 }
}

if ($Install) {
    Install-Release
    exit 0
}

# Check prerequisites first
$prereqOk = Test-Prerequisites
if (-not $prereqOk) { exit 1 }

Initialize-Env

if ($Package) {
    Start-Build
    New-Installer
}
elseif ($Build) {
    Start-Build
}
else {
    # Default: build and run
    Start-Build
    Start-App
}
