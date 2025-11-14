# DuckBot Windows Installer (PowerShell)
# Run as Administrator

param(
    [string]$InstallPath = "$env:ProgramFiles\DuckBot"
)

# Function to check if running as admin
function Test-Admin {
    $currentUser = [Security.Principal.WindowsIdentity]::GetCurrent()
    $principal = New-Object Security.Principal.WindowsPrincipal($currentUser)
    return $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
}

if (-not (Test-Admin)) {
    Write-Host "This installer must run as Administrator." -ForegroundColor Red
    Write-Host "Please right-click PowerShell and select 'Run as administrator'" -ForegroundColor Yellow
    pause
    exit 1
}

Write-Host ""
Write-Host "========================================"
Write-Host "  DuckBot Installation" -ForegroundColor Cyan
Write-Host "========================================"
Write-Host ""

$scriptDir = Split-Path -Parent $MyInvocation.MyCommandPath

# Get custom install path if provided
if (-not $InstallPath.StartsWith($env:ProgramFiles)) {
    Write-Host "Default installation path: $InstallPath"
    $customPath = Read-Host "Enter custom path (or press Enter for default)"
    if ($customPath) {
        $InstallPath = $customPath
    }
}

Write-Host "Installing to: $InstallPath" -ForegroundColor Green

# Create installation directory
if (-not (Test-Path $InstallPath)) {
    New-Item -ItemType Directory -Path $InstallPath -Force | Out-Null
    if (-not $?) {
        Write-Host "Failed to create installation directory" -ForegroundColor Red
        pause
        exit 1
    }
}

# Copy JAR file
Write-Host "Copying application files..."
$jarPath = Join-Path $scriptDir "target\duckbot-java-0.1.0-SNAPSHOT.jar"
if (Test-Path $jarPath) {
    Copy-Item -Path $jarPath -Destination $InstallPath -Force
} else {
    Write-Host "Error: JAR file not found at $jarPath" -ForegroundColor Red
    Write-Host "Please run from the DuckBot project root directory" -ForegroundColor Yellow
    pause
    exit 1
}

# Copy data directory if it exists
$dataPath = Join-Path $scriptDir "data"
if (Test-Path $dataPath) {
    Copy-Item -Path $dataPath -Destination $InstallPath -Recurse -Force
}

# Copy pom.xml for mvn javafx:run
Write-Host "Copying build configuration..."
Copy-Item -Path (Join-Path $scriptDir "pom.xml") -Destination $InstallPath -Force
Copy-Item -Path (Join-Path $scriptDir "src") -Destination $InstallPath -Recurse -Force

# Create Start Menu folder
$startMenuPath = "$env:APPDATA\Microsoft\Windows\Start Menu\Programs\DuckBot"
if (-not (Test-Path $startMenuPath)) {
    New-Item -ItemType Directory -Path $startMenuPath -Force | Out-Null
}

# Create launcher batch file
Write-Host "Creating launcher script..."
$launcherContent = @"
@echo off
REM DuckBot Application Launcher
cd /d "$InstallPath"
cls
echo.
echo ========================================
echo  DuckBot Launcher
echo ========================================
echo.
echo Starting DuckBot...
echo.

REM Check if Maven is installed
where mvn >nul 2>&1
if %errorLevel% neq 0 (
    echo ERROR: Maven is not installed or not in PATH
    echo.
    echo DuckBot requires:
    echo   - Java 17 or later
    echo   - Apache Maven
    echo.
    echo Download:
    echo   Java 17: https://adoptium.net/
    echo   Maven: https://maven.apache.org/
    echo.
    pause
    exit /b 1
)

REM Launch DuckBot
mvn javafx:run
pause
"@

$launcherPath = Join-Path $InstallPath "DuckBotLauncher.bat"
Set-Content -Path $launcherPath -Value $launcherContent -Encoding ASCII

# Create shortcut in Start Menu
Write-Host "Creating Start Menu shortcuts..."
$WshShell = New-Object -ComObject WScript.Shell
$shortcutPath = Join-Path $startMenuPath "DuckBot.lnk"
$shortcut = $WshShell.CreateShortcut($shortcutPath)
$shortcut.TargetPath = $launcherPath
$shortcut.WorkingDirectory = $InstallPath
$shortcut.Description = "DuckBot - LDPlayer Automation"
$shortcut.Save()

# Create Desktop shortcut
$desktopShortcut = Join-Path ([Environment]::GetFolderPath("Desktop")) "DuckBot.lnk"
$shortcut = $WshShell.CreateShortcut($desktopShortcut)
$shortcut.TargetPath = $launcherPath
$shortcut.WorkingDirectory = $InstallPath
$shortcut.Description = "DuckBot - LDPlayer Automation"
$shortcut.Save()

# Create uninstall script
$uninstallContent = @"
@echo off
REM DuckBot Uninstaller
echo.
echo ========================================
echo  DuckBot Uninstaller
echo ========================================
echo.

REM Confirm uninstall
set /p confirm="Are you sure you want to uninstall DuckBot? (Y/N): "
if /i not "!confirm!"=="Y" exit /b 0

REM Remove installation directory
echo Removing application files...
rmdir /s /q "$InstallPath" 2>nul

REM Remove Start Menu shortcuts
echo Removing shortcuts...
rmdir /s /q "$startMenuPath" 2>nul

REM Remove Desktop shortcut
del "%USERPROFILE%\Desktop\DuckBot.lnk" 2>nul

REM Remove registry entry
reg delete "HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\DuckBot" /f 2>nul

echo.
echo DuckBot has been uninstalled.
echo.
pause
"@

$uninstallPath = Join-Path $InstallPath "Uninstall.bat"
Set-Content -Path $uninstallPath -Value $uninstallContent -Encoding ASCII

# Create uninstall shortcut
$uninstallShortcut = Join-Path $startMenuPath "Uninstall DuckBot.lnk"
$shortcut = $WshShell.CreateShortcut($uninstallShortcut)
$shortcut.TargetPath = $uninstallPath
$shortcut.WorkingDirectory = $InstallPath
$shortcut.Description = "Uninstall DuckBot"
$shortcut.Save()

# Add to Windows Registry for uninstall
Write-Host "Registering application..."
$regPath = "HKLM:\SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\DuckBot"
if (-not (Test-Path $regPath)) {
    New-Item -Path $regPath -Force | Out-Null
}
New-ItemProperty -Path $regPath -Name "DisplayName" -Value "DuckBot" -PropertyType String -Force | Out-Null
New-ItemProperty -Path $regPath -Name "DisplayVersion" -Value "0.1.0" -PropertyType String -Force | Out-Null
New-ItemProperty -Path $regPath -Name "Publisher" -Value "DuckBot" -PropertyType String -Force | Out-Null
New-ItemProperty -Path $regPath -Name "UninstallString" -Value "$uninstallPath" -PropertyType String -Force | Out-Null
New-ItemProperty -Path $regPath -Name "InstallLocation" -Value $InstallPath -PropertyType String -Force | Out-Null
New-ItemProperty -Path $regPath -Name "NoModify" -Value 1 -PropertyType DWORD -Force | Out-Null
New-ItemProperty -Path $regPath -Name "NoRepair" -Value 1 -PropertyType DWORD -Force | Out-Null

Write-Host ""
Write-Host "========================================"
Write-Host "  Installation Complete!" -ForegroundColor Green
Write-Host "========================================"
Write-Host ""
Write-Host "DuckBot has been installed to: $InstallPath" -ForegroundColor Green
Write-Host ""
Write-Host "Shortcuts created at:" -ForegroundColor Cyan
Write-Host "  - Start Menu: Programs\DuckBot\DuckBot.lnk"
Write-Host "  - Desktop: DuckBot.lnk"
Write-Host ""
Write-Host "Login with:" -ForegroundColor Yellow
Write-Host "  Username: Duck"
Write-Host "  Password: Aedyn2013"
Write-Host ""
Write-Host "To run: Double-click the DuckBot shortcut or search for 'DuckBot' in Start Menu" -ForegroundColor Cyan
Write-Host ""
pause
