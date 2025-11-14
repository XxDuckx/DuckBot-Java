@echo off
REM DuckBot Installer for Windows
REM Run as Administrator

setlocal enabledelayedexpansion

REM Check if running as admin
net session >nul 2>&1
if %errorLevel% neq 0 (
    echo This installer must be run as Administrator.
    echo Please right-click and select "Run as administrator"
    pause
    exit /b 1
)

echo.
echo ========================================
echo   DuckBot Installation
echo ========================================
echo.

REM Default installation path
set "INSTALL_PATH=%ProgramFiles%\DuckBot"

REM Allow custom installation path
echo Default installation path: %INSTALL_PATH%
set /p CUSTOM_PATH="Enter custom path (or press Enter to use default): "
if not "!CUSTOM_PATH!"=="" (
    set "INSTALL_PATH=!CUSTOM_PATH!"
)

echo Installing to: %INSTALL_PATH%

REM Create installation directory
if not exist "%INSTALL_PATH%" (
    mkdir "%INSTALL_PATH%"
    if errorlevel 1 (
        echo Failed to create installation directory
        pause
        exit /b 1
    )
)

REM Copy files
echo Copying application files...
REM Source files should be in the script directory
set "SOURCE_DIR=%~dp0"

REM Copy JAR and resources
if exist "%SOURCE_DIR%target" (
    echo Copying application...
    xcopy "%SOURCE_DIR%target\duckbot-java-0.1.0-SNAPSHOT.jar" "%INSTALL_PATH%" /Y /Q
    if errorlevel 1 goto copy_error
) else (
    echo Error: Application files not found. Please run from project root directory.
    pause
    exit /b 1
)

REM Copy data directory template (if exists)
if exist "%SOURCE_DIR%data" (
    echo Copying data directory...
    xcopy "%SOURCE_DIR%data" "%INSTALL_PATH%\data" /E /I /Y /Q
)

REM Create Start Menu shortcuts
echo Creating Start Menu shortcuts...
set "START_MENU=%APPDATA%\Microsoft\Windows\Start Menu\Programs"
if not exist "%START_MENU%\DuckBot" mkdir "%START_MENU%\DuckBot"

REM Create launcher shortcut
set "SHORTCUT_PATH=%START_MENU%\DuckBot\DuckBot.lnk"
call :create_shortcut "%SHORTCUT_PATH%" "%INSTALL_PATH%" "DuckBotLauncher.bat"

REM Create launcher batch file
echo Creating launcher script...
(
    echo @echo off
    echo REM DuckBot Application Launcher
    echo cd /d "%INSTALL_PATH%"
    echo if exist pom.xml (
    echo     mvn javafx:run
    echo ) else (
    echo     echo.
    echo     echo DuckBot requires Java 17 and Maven to be installed and in PATH
    echo     echo.
    echo     echo Download:
    echo     echo   Java 17: https://adoptium.net/
    echo     echo   Maven: https://maven.apache.org/
    echo     echo.
    echo     pause
    echo )
) > "%INSTALL_PATH%\DuckBotLauncher.bat"

REM Create uninstall shortcut
set "UNINSTALL_SHORTCUT=%START_MENU%\DuckBot\Uninstall DuckBot.lnk"
call :create_uninstall_shortcut "%UNINSTALL_SHORTCUT%"

REM Create uninstall script
echo Creating uninstall script...
(
    echo @echo off
    echo if "%%1"=="" (
    echo     powershell -Command "if ([System.Windows.Forms.MessageBox]::Show('Uninstall DuckBot?', 'DuckBot', 4) -eq 6) { exit 0 } else { exit 1 }"
    echo     if errorlevel 1 exit /b 1
    echo )
    echo rmdir /s /q "%INSTALL_PATH%"
    echo rmdir /s /q "%START_MENU%\DuckBot"
    echo echo DuckBot has been uninstalled.
    echo pause
) > "%INSTALL_PATH%\Uninstall.bat"

REM Registry entries
echo Adding registry entries...
reg add "HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\DuckBot" /f
reg add "HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\DuckBot" /v "DisplayName" /t REG_SZ /d "DuckBot" /f
reg add "HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\DuckBot" /v "DisplayVersion" /t REG_SZ /d "0.1.0" /f
reg add "HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\DuckBot" /v "Publisher" /t REG_SZ /d "DuckBot" /f
reg add "HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\DuckBot" /v "UninstallString" /t REG_SZ /d "%INSTALL_PATH%\Uninstall.bat" /f
reg add "HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\DuckBot" /v "InstallLocation" /t REG_SZ /d "%INSTALL_PATH%" /f

echo.
echo ========================================
echo   Installation Complete!
echo ========================================
echo.
echo DuckBot has been installed to: %INSTALL_PATH%
echo A shortcut has been created in your Start Menu
echo.
echo Login with:
echo   Username: Duck
echo   Password: Aedyn2013
echo.
pause
exit /b 0

:copy_error
echo Error copying files
pause
exit /b 1

:create_shortcut
REM Create a shortcut using VBScript
setlocal
set "LNK_PATH=%~1"
set "WORK_DIR=%~2"
set "BATCH_FILE=%~3"

REM Create VBS script to make shortcut
set "VBS_FILE=%TEMP%\create_shortcut.vbs"
(
    echo Set oWS = WScript.CreateObject("WScript.Shell"^)
    echo sLinkFile = "%LNK_PATH%"
    echo Set oLink = oWS.CreateShortcut(sLinkFile^)
    echo oLink.TargetPath = "%WORK_DIR%\%BATCH_FILE%"
    echo oLink.WorkingDirectory = "%WORK_DIR%"
    echo oLink.Description = "DuckBot - LDPlayer Automation"
    echo oLink.IconLocation = "%WORK_DIR%\%BATCH_FILE%"
    echo oLink.Save
) > "%VBS_FILE%"

cscript.exe "%VBS_FILE%"
del "%VBS_FILE%"
endlocal
exit /b 0

:create_uninstall_shortcut
REM Create uninstall shortcut
set "VBS_FILE=%TEMP%\create_uninstall.vbs"
(
    echo Set oWS = WScript.CreateObject("WScript.Shell"^)
    echo sLinkFile = "%~1"
    echo Set oLink = oWS.CreateShortcut(sLinkFile^)
    echo oLink.TargetPath = "%INSTALL_PATH%\Uninstall.bat"
    echo oLink.WorkingDirectory = "%INSTALL_PATH%"
    echo oLink.Description = "Uninstall DuckBot"
    echo oLink.Save
) > "%VBS_FILE%"

cscript.exe "%VBS_FILE%"
del "%VBS_FILE%"
exit /b 0

