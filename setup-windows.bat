@echo off
REM DuckBot Installer - Quick Setup
REM Right-click and select "Run as administrator"

setlocal enabledelayedexpansion

echo.
echo ========================================
echo   DuckBot Installer for Windows
echo ========================================
echo.
echo This installer will:
echo   1. Create Start Menu shortcuts
echo   2. Add to Control Panel uninstall list
echo   3. Create Desktop shortcut
echo.

REM Check admin
net session >nul 2>&1
if %errorLevel% neq 0 (
    echo ERROR: Must run as Administrator
    echo Please right-click this file and select "Run as administrator"
    pause
    exit /b 1
)

REM Get current directory
set "SOURCE_DIR=%~dp0"
set "INSTALL_DIR=%ProgramFiles%\DuckBot"

echo Installation directory: %INSTALL_DIR%
echo.

REM Create install directory
if not exist "%INSTALL_DIR%" mkdir "%INSTALL_DIR%"

REM Copy application files
echo [1/5] Copying application files...
if exist "%SOURCE_DIR%target\duckbot-java-0.1.0-SNAPSHOT.jar" (
    copy "%SOURCE_DIR%target\duckbot-java-0.1.0-SNAPSHOT.jar" "%INSTALL_DIR%\" >nul
    if not errorlevel 1 echo Done!
) else (
    echo WARNING: JAR file not found. Make sure to run from project root.
)

REM Copy project files for mvn javafx:run
echo [2/5] Copying project files...
xcopy "%SOURCE_DIR%pom.xml" "%INSTALL_DIR%\" /Y /Q >nul
xcopy "%SOURCE_DIR%src" "%INSTALL_DIR%\src" /E /I /Y /Q >nul
echo Done!

REM Create launcher script
echo [3/5] Creating launcher scripts...
(
    echo @echo off
    echo cd /d "%INSTALL_DIR%"
    echo cls
    echo echo.
    echo echo Starting DuckBot...
    echo echo.
    echo mvn javafx:run
) > "%INSTALL_DIR%\DuckBotLauncher.bat"
echo Done!

REM Create Start Menu shortcuts
echo [4/5] Creating Start Menu shortcuts...
set "STARTMENU=%APPDATA%\Microsoft\Windows\Start Menu\Programs"
if not exist "%STARTMENU%\DuckBot" mkdir "%STARTMENU%\DuckBot"

REM Create main shortcut (using PowerShell for better icon support)
powershell -Command ^
  "$ws = New-Object -ComObject WScript.Shell;" ^
  "$s = $ws.CreateShortcut('%STARTMENU%\DuckBot\DuckBot.lnk');" ^
  "$s.TargetPath = '%INSTALL_DIR%\DuckBotLauncher.bat';" ^
  "$s.WorkingDirectory = '%INSTALL_DIR%';" ^
  "$s.Description = 'DuckBot - LDPlayer Automation';" ^
  "$s.Save()"

REM Create uninstall script
echo @echo off > "%INSTALL_DIR%\Uninstall.bat"
echo if "%%1"=="" ( >> "%INSTALL_DIR%\Uninstall.bat"
echo     echo. >> "%INSTALL_DIR%\Uninstall.bat"
echo     echo Uninstalling DuckBot... >> "%INSTALL_DIR%\Uninstall.bat"
echo     timeout /t 2 /nobreak >> "%INSTALL_DIR%\Uninstall.bat"
echo ) >> "%INSTALL_DIR%\Uninstall.bat"
echo rmdir /s /q "%INSTALL_DIR%" 2>nul >> "%INSTALL_DIR%\Uninstall.bat"
echo rmdir /s /q "%STARTMENU%\DuckBot" 2>nul >> "%INSTALL_DIR%\Uninstall.bat"
echo del "%%USERPROFILE%%\Desktop\DuckBot.lnk" 2>nul >> "%INSTALL_DIR%\Uninstall.bat"
echo reg delete "HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\DuckBot" /f 2>nul >> "%INSTALL_DIR%\Uninstall.bat"

REM Create uninstall shortcut
powershell -Command ^
  "$ws = New-Object -ComObject WScript.Shell;" ^
  "$s = $ws.CreateShortcut('%STARTMENU%\DuckBot\Uninstall DuckBot.lnk');" ^
  "$s.TargetPath = '%INSTALL_DIR%\Uninstall.bat';" ^
  "$s.WorkingDirectory = '%INSTALL_DIR%';" ^
  "$s.Description = 'Uninstall DuckBot';" ^
  "$s.Save()"

REM Create Desktop shortcut
powershell -Command ^
  "$ws = New-Object -ComObject WScript.Shell;" ^
  "$s = $ws.CreateShortcut('%USERPROFILE%\Desktop\DuckBot.lnk');" ^
  "$s.TargetPath = '%INSTALL_DIR%\DuckBotLauncher.bat';" ^
  "$s.WorkingDirectory = '%INSTALL_DIR%';" ^
  "$s.Description = 'DuckBot - LDPlayer Automation';" ^
  "$s.Save()"

echo Done!

REM Add to registry
echo [5/5] Registering application...
reg add "HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\DuckBot" /f >nul
reg add "HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\DuckBot" /v "DisplayName" /t REG_SZ /d "DuckBot" /f >nul
reg add "HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\DuckBot" /v "DisplayVersion" /t REG_SZ /d "0.1.0" /f >nul
reg add "HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\DuckBot" /v "Publisher" /t REG_SZ /d "DuckBot" /f >nul
reg add "HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\DuckBot" /v "UninstallString" /t REG_SZ /d "%INSTALL_DIR%\Uninstall.bat" /f >nul
reg add "HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\DuckBot" /v "InstallLocation" /t REG_SZ /d "%INSTALL_DIR%" /f >nul
echo Done!

echo.
echo ========================================
echo   Installation Complete!
echo ========================================
echo.
echo DuckBot has been installed to:
echo   %INSTALL_DIR%
echo.
echo Shortcuts created:
echo   - Start Menu: Start menu ^> Programs ^> DuckBot
echo   - Desktop: DuckBot.lnk
echo.
echo Requirements:
echo   - Java 17+: https://adoptium.net/
echo   - Maven: https://maven.apache.org/
echo.
echo Login credentials:
echo   Username: Duck
echo   Password: Aedyn2013
echo.
echo ========================================
echo.
pause
