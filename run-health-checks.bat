@echo off
setlocal ENABLEDELAYEDEXPANSION

REM Wrapper for scripts\test-health.ps1 on Windows
set "args="
if not "%BASE_URL%"=="" set "args=!args! -BaseUrl %BASE_URL%"
if not "%RETRIES%"=="" set "args=!args! -Retries %RETRIES%"
if not "%DELAY_SECONDS%"=="" set "args=!args! -DelaySeconds %DELAY_SECONDS%"
if not "%TIMEOUT_SECONDS%"=="" set "args=!args! -TimeoutSeconds %TIMEOUT_SECONDS%"

REM Handle INCLUDE_METRICS as 1/true/yes (case-insensitive)
set "_IM=%INCLUDE_METRICS%"
if /I "%_IM%"=="1" set "args=!args! -IncludeMetrics"
if /I "%_IM%"=="true" set "args=!args! -IncludeMetrics"
if /I "%_IM%"=="yes" set "args=!args! -IncludeMetrics"

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\test-health.ps1" !args!
set "exitcode=%ERRORLEVEL%"
if not "%exitcode%"=="0" (
  echo Health checks failed. Exit code: %exitcode%
  exit /b %exitcode%
) else (
  echo Health checks passed.
  exit /b 0
)
