@echo off
setlocal

:: Change to project root directory
cd /d "%~dp0.."
echo ====================================================
echo ApiBanker - Native Installer Builder (Windows)
echo ====================================================

:: Extract version from pom.xml using findstr + for loop
for /f "tokens=3 delims=<>" %%v in ('findstr /r "<version>" pom.xml') do (
    set APP_VERSION=%%v
    goto :got_version
)
:got_version
:: jpackage MSI requires a pure numeric version (e.g. 1.2.0) — strip qualifiers
set IS_PRERELEASE=false
echo %APP_VERSION% | findstr /i "beta alpha rc snapshot" >nul
if not errorlevel 1 set IS_PRERELEASE=true

set APP_VERSION_MSI=%APP_VERSION:-beta=%
set APP_VERSION_MSI=%APP_VERSION_MSI:-alpha=%
set APP_VERSION_MSI=%APP_VERSION_MSI:-rc1=%
set APP_VERSION_MSI=%APP_VERSION_MSI:-rc2=%
set APP_VERSION_MSI=%APP_VERSION_MSI:-rc=%
set APP_VERSION_MSI=%APP_VERSION_MSI:-SNAPSHOT=%
set APP_VERSION_MSI=%APP_VERSION_MSI:-snapshot=%

:: For pre-release builds, append YYYYMM so they don't conflict with the final release MSI version
if "%IS_PRERELEASE%"=="true" (
    for /f %%d in ('powershell -NoProfile -Command "Get-Date -Format 'yyyyMM'"') do set YYYYMM=%%d
)
if "%IS_PRERELEASE%"=="true" set APP_VERSION_MSI=%APP_VERSION_MSI%.%YYYYMM%

echo [INFO] App version : %APP_VERSION%
echo [INFO] MSI version : %APP_VERSION_MSI% (pre-release: %IS_PRERELEASE%)


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

:: Copy the shaded fat JAR (not the original- stub produced by maven-shade-plugin)
for /f "delims=" %%f in ('dir /b /o-d "target\apibanker-*.jar" ^| findstr /v "^original-"') do (
    copy "target\%%f" target\jpackage-input\apibanker-app.jar >nul
    echo [INFO] Copied: %%f
    goto :copied
)
:copied

:: Verify the JAR was copied and has real content
for %%s in (target\jpackage-input\apibanker-app.jar) do (
    if %%~zs LSS 1000000 (
        echo [ERROR] JAR copy failed or copied the wrong file ^(size: %%~zs bytes^). Expected the shaded fat JAR ^(~30MB+^).
        exit /b 1
    )
)
echo [INFO] JAR verified: OK

echo.
echo [3/3] Running jpackage...
echo Note: Building .exe and .msi installers requires the WiX Toolset (v3.0+) installed and in your PATH.
echo If WiX is not installed, the installer steps will fail.
echo Building portable App Image (used as input for .msi)...
jpackage --type app-image ^
    --input target\jpackage-input ^
    --dest target\artifacts ^
    --name ApiBanker ^
    --main-jar apibanker-app.jar ^
    --main-class in.slpro.apibanker.App ^
    --icon src\main\resources\icon.ico ^
    --app-version %APP_VERSION_MSI%
if %errorlevel% neq 0 ( echo [ERROR] app-image creation failed. & exit /b 1 )

echo.
echo Building .msi Installer (with Desktop shortcut + launch-after-install prompt)...
jpackage --type msi ^
    --app-image target\artifacts\ApiBanker ^
    --dest target\artifacts ^
    --name ApiBanker ^
    --app-version %APP_VERSION_MSI% ^
    --description "ApiBanker - Offline-First API Toolkit" ^
    --vendor "NCRK" ^
    --win-shortcut ^
    --win-shortcut-prompt ^
    --win-menu ^
    --win-menu-group "NCRK" ^
    --win-dir-chooser ^
    --win-upgrade-uuid 7e6c3b2a-4f1d-4a8e-9c5b-2d1e3f4a5b6c
if %errorlevel% neq 0 ( echo [ERROR] .msi creation failed. Ensure WiX Toolset v3 is installed and in PATH. & exit /b 1 )

echo.
echo Building .zip Bundle from app-image...
powershell -NoProfile -Command "Compress-Archive -Path 'target\artifacts\ApiBanker' -DestinationPath 'target\artifacts\ApiBanker-%APP_VERSION%-jre21-winX64.zip' -Force"
if %errorlevel% neq 0 ( echo [ERROR] .zip creation failed. & exit /b 1 )

echo Cleaning up app-image directory...
rmdir /s /q "target\artifacts\ApiBanker"

echo.
echo ====================================================
echo Done! Check the 'target\artifacts' directory.
echo ====================================================
pause
