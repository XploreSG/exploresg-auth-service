@echo off
setlocal enabledelayedexpansion

REM 🧪 Integration Test Runner Script for ExploreSG Auth Service (Windows)
REM This script provides easy commands to run integration tests locally on Windows

title ExploreSG Auth Service - Integration Test Runner

REM Initialize variables
set DEBUG=false
set PROFILE=
set PORT=
set COMMAND=

REM Function to print colored output (simulated with echo)
:print_status
echo [INFO] %~1
exit /b

:print_success
echo [SUCCESS] %~1
exit /b

:print_error
echo [ERROR] %~1
exit /b

:show_usage
echo 🧪 ExploreSG Auth Service - Integration Test Runner
echo.
echo Usage: %0 [COMMAND] [OPTIONS]
echo.
echo Commands:
echo   hello-world      Run hello world integration tests
echo   database         Run database integration tests
echo   all              Run all integration tests
echo   unit             Run unit tests only
echo   coverage         Run tests with coverage report
echo   clean            Clean and rebuild project
echo   help             Show this help message
echo.
echo Options:
echo   --debug          Run with debug logging
echo   --profile=NAME   Use specific Maven profile
echo   --port=PORT      Use specific server port
echo.
echo Examples:
echo   %0 hello-world
echo   %0 all --debug
echo   %0 database --profile=integration-tests
echo   %0 coverage --port=8090
exit /b

:check_prerequisites
call :print_status "Checking prerequisites..."

REM Check if Maven wrapper exists
if not exist "mvnw.cmd" (
    call :print_error "Maven wrapper (mvnw.cmd) not found!"
    exit /b 1
)

REM Check if we're in the right directory
if not exist "pom.xml" (
    call :print_error "pom.xml not found. Are you in the project root directory?"
    exit /b 1
)

call :print_success "Prerequisites check passed!"
exit /b 0

:run_hello_world_tests
call :print_status "Running Hello World Integration Tests..."

set "mvn_cmd=mvnw.cmd test -Dtest=HelloWorldIntegrationTest -Dspring.profiles.active=integration-test"

if "%DEBUG%"=="true" (
    set "mvn_cmd=!mvn_cmd! -X -Dlogging.level.com.exploresg=DEBUG"
)

if not "%PROFILE%"=="" (
    set "mvn_cmd=!mvn_cmd! -P%PROFILE%"
)

if not "%PORT%"=="" (
    set "mvn_cmd=!mvn_cmd! -Dserver.port=%PORT%"
)

call :print_status "Executing: !mvn_cmd!"
call !mvn_cmd!

if errorlevel 1 (
    call :print_error "Hello World integration tests failed!"
    exit /b 1
)

call :print_success "Hello World integration tests completed!"
exit /b 0

:run_database_tests
call :print_status "Running Database Integration Tests..."

set "mvn_cmd=mvnw.cmd test -Dtest=AuthServiceDatabaseIntegrationTest -Dspring.profiles.active=integration-test"

if "%DEBUG%"=="true" (
    set "mvn_cmd=!mvn_cmd! -X -Dlogging.level.com.exploresg=DEBUG"
)

if not "%PROFILE%"=="" (
    set "mvn_cmd=!mvn_cmd! -P%PROFILE%"
)

if not "%PORT%"=="" (
    set "mvn_cmd=!mvn_cmd! -Dserver.port=%PORT%"
)

call :print_status "Executing: !mvn_cmd!"
call !mvn_cmd!

if errorlevel 1 (
    call :print_error "Database integration tests failed!"
    exit /b 1
)

call :print_success "Database integration tests completed!"
exit /b 0

:run_all_integration_tests
call :print_status "Running All Integration Tests..."

set "mvn_cmd=mvnw.cmd test -Dtest=*Integration*Test -Dspring.profiles.active=integration-test"

if "%DEBUG%"=="true" (
    set "mvn_cmd=!mvn_cmd! -X -Dlogging.level.com.exploresg=DEBUG"
)

if not "%PROFILE%"=="" (
    set "mvn_cmd=!mvn_cmd! -P%PROFILE%"
)

if not "%PORT%"=="" (
    set "mvn_cmd=!mvn_cmd! -Dserver.port=%PORT%"
)

call :print_status "Executing: !mvn_cmd!"
call !mvn_cmd!

if errorlevel 1 (
    call :print_error "Integration tests failed!"
    exit /b 1
)

call :print_success "All integration tests completed!"
exit /b 0

:run_unit_tests
call :print_status "Running Unit Tests..."

set "mvn_cmd=mvnw.cmd test -Dtest=!*Integration*"

if "%DEBUG%"=="true" (
    set "mvn_cmd=!mvn_cmd! -X"
)

call :print_status "Executing: !mvn_cmd!"
call !mvn_cmd!

if errorlevel 1 (
    call :print_error "Unit tests failed!"
    exit /b 1
)

call :print_success "Unit tests completed!"
exit /b 0

:run_with_coverage
call :print_status "Running Tests with Coverage Report..."

set "mvn_cmd=mvnw.cmd clean test jacoco:report -Dspring.profiles.active=integration-test"

if "%DEBUG%"=="true" (
    set "mvn_cmd=!mvn_cmd! -X"
)

if not "%PROFILE%"=="" (
    set "mvn_cmd=!mvn_cmd! -P%PROFILE%"
)

call :print_status "Executing: !mvn_cmd!"
call !mvn_cmd!

if errorlevel 1 (
    call :print_error "Tests with coverage failed!"
    exit /b 1
)

call :print_success "Tests with coverage completed!"
call :print_status "Coverage report available at: target\site\jacoco\index.html"

REM Try to open coverage report
if exist "target\site\jacoco\index.html" (
    call :print_status "Opening coverage report..."
    start "" "target\site\jacoco\index.html"
)
exit /b 0

:clean_project
call :print_status "Cleaning and rebuilding project..."

call mvnw.cmd clean compile -B

if errorlevel 1 (
    call :print_error "Project clean and rebuild failed!"
    exit /b 1
)

call :print_success "Project cleaned and rebuilt!"
exit /b 0

REM Parse command line arguments
:parse_args
if "%~1"=="" goto :args_done
if "%~1"=="--debug" (
    set DEBUG=true
    shift
    goto :parse_args
)
if "%~1"=="--profile" (
    set PROFILE=%~2
    shift
    shift
    goto :parse_args
)
if "%~1"=="--port" (
    set PORT=%~2
    shift
    shift
    goto :parse_args
)
REM Check for --profile=value format
for /f "tokens=1,2 delims==" %%a in ("%~1") do (
    if "%%a"=="--profile" (
        set PROFILE=%%b
        shift
        goto :parse_args
    )
    if "%%a"=="--port" (
        set PORT=%%b
        shift
        goto :parse_args
    )
)
set COMMAND=%~1
shift
goto :parse_args

:args_done

REM Main script logic
echo 🚀 ExploreSG Auth Service - Integration Test Runner
echo ==================================================

call :check_prerequisites
if errorlevel 1 exit /b 1

if "%COMMAND%"=="hello-world" (
    call :run_hello_world_tests
) else if "%COMMAND%"=="database" (
    call :run_database_tests
) else if "%COMMAND%"=="all" (
    call :run_all_integration_tests
) else if "%COMMAND%"=="unit" (
    call :run_unit_tests
) else if "%COMMAND%"=="coverage" (
    call :run_with_coverage
) else if "%COMMAND%"=="clean" (
    call :clean_project
) else (
    call :show_usage
)

if errorlevel 1 (
    echo.
    call :print_error "Test execution failed! ❌"
    exit /b 1
) else (
    echo.
    call :print_success "Test execution completed! 🎉"
)

endlocal