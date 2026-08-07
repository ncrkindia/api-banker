@echo off
setlocal
echo ====================================================
echo ApiBanker - Native Installer Builder (Windows)
echo ====================================================

echo.
echo [1/3] Building Fat JAR with Maven...
call mvn clean package -DskipTests
if %errorlevel% neq 0 (
    echo Maven build failed!
    exit /b %errorlevel%
)

echo.
echo [2/3] Preparing jpackage input...
if exist target\jpackage-input rmdir /s /q target\jpackage-input
mkdir target\jpackage-input
copy target\apibanker-*.jar target\jpackage-input\apibanker-app.jar >nul

echo.
echo [3/3] Running jpackage...
echo Note: Building .exe and .msi installers requires the WiX Toolset (v3.0+) installed and in your PATH.
echo If WiX is not installed, the installer steps will fail.
echo Building portable App Image first...
jpackage --type app-image --input target\jpackage-input --dest target\artifacts --name ApiBanker --main-jar apibanker-app.jar --main-class in.slpro.apibanker.App 
if %errorlevel% neq 0 echo Warning: app-image creation failed.

echo.
echo Building .msi Installer...
jpackage --type msi --input target\jpackage-input --dest target\artifacts --name ApiBanker --main-jar apibanker-app.jar --main-class in.slpro.apibanker.App --win-shortcut --win-menu --win-dir-chooser
if %errorlevel% neq 0 echo Warning: .msi creation failed. Ensure WiX is installed.

echo.
echo ====================================================
echo Done! Check the 'target\artifacts' directory.
echo ====================================================
pause
