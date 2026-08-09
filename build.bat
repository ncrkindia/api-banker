@echo off
setlocal

cd /d "%~dp0"

echo ============================================================
echo  ApiBanker Build Script (Windows)
echo ============================================================
echo.

set SKIP_TESTS=-DskipTests
set GOAL=package

:parse_args
if "%~1"=="" goto run_build
if /I "%~1"=="--with-tests" (
    set SKIP_TESTS=
    shift
    goto parse_args
)
if /I "%~1"=="--install" (
    set GOAL=install
    shift
    goto parse_args
)
shift
goto parse_args

:run_build
echo [1/3] Cleaning previous build...
call mvn clean %SKIP_TESTS%
if %errorlevel% neq 0 ( echo [ERROR] Clean failed! & exit /b %errorlevel% )

echo.
echo [2/3] Compiling and packaging JAR + ZIP bundle...
call mvn %GOAL% %SKIP_TESTS%
if %errorlevel% neq 0 ( echo [ERROR] Maven build failed! & exit /b %errorlevel% )

echo.
echo [3/3] Building native Windows installer (.msi)...
call scripts\build-installers.bat
if %errorlevel% neq 0 ( echo [ERROR] Installer build failed! & exit /b %errorlevel% )

echo.
echo ============================================================
echo  Build SUCCESS! Artifacts are in: target\artifacts\
echo ============================================================
echo  Portable bundle:  target\artifacts\apibanker.zip
echo  Native installer: target\artifacts\ApiBanker-installer.msi
echo ============================================================
endlocal
