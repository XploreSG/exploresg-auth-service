#!/bin/bash

# 🧪 Integration Test Runner Script for ExploreSG Auth Service
# This script provides easy commands to run integration tests locally

set -e  # Exit on any error

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to show usage
show_usage() {
    echo "🧪 ExploreSG Auth Service - Integration Test Runner"
    echo ""
    echo "Usage: $0 [COMMAND] [OPTIONS]"
    echo ""
    echo "Commands:"
    echo "  hello-world      Run hello world integration tests"
    echo "  database         Run database integration tests"
    echo "  all              Run all integration tests"
    echo "  unit             Run unit tests only"
    echo "  coverage         Run tests with coverage report"
    echo "  clean            Clean and rebuild project"
    echo "  help             Show this help message"
    echo ""
    echo "Options:"
    echo "  --debug          Run with debug logging"
    echo "  --profile=NAME   Use specific Maven profile"
    echo "  --port=PORT      Use specific server port"
    echo ""
    echo "Examples:"
    echo "  $0 hello-world"
    echo "  $0 all --debug"
    echo "  $0 database --profile=integration-tests"
    echo "  $0 coverage --port=8090"
}

# Function to check prerequisites
check_prerequisites() {
    print_status "Checking prerequisites..."
    
    # Check if Maven wrapper exists
    if [ ! -f "./mvnw" ]; then
        print_error "Maven wrapper (mvnw) not found!"
        exit 1
    fi
    
    # Check Java version
    if ! ./mvnw -version > /dev/null 2>&1; then
        print_error "Maven/Java not properly configured!"
        exit 1
    fi
    
    # Check if we're in the right directory
    if [ ! -f "pom.xml" ]; then
        print_error "pom.xml not found. Are you in the project root directory?"
        exit 1
    fi
    
    print_success "Prerequisites check passed!"
}

# Function to run hello world tests
run_hello_world_tests() {
    print_status "Running Hello World Integration Tests..."
    
    local mvn_cmd="./mvnw test -Dtest=HelloWorldIntegrationTest -Dspring.profiles.active=integration-test"
    
    if [ "$DEBUG" = true ]; then
        mvn_cmd="$mvn_cmd -X -Dlogging.level.com.exploresg=DEBUG"
    fi
    
    if [ ! -z "$PROFILE" ]; then
        mvn_cmd="$mvn_cmd -P$PROFILE"
    fi
    
    if [ ! -z "$PORT" ]; then
        mvn_cmd="$mvn_cmd -Dserver.port=$PORT"
    fi
    
    print_status "Executing: $mvn_cmd"
    eval $mvn_cmd
    
    print_success "Hello World integration tests completed!"
}

# Function to run database tests
run_database_tests() {
    print_status "Running Database Integration Tests..."
    
    local mvn_cmd="./mvnw test -Dtest=AuthServiceDatabaseIntegrationTest -Dspring.profiles.active=integration-test"
    
    if [ "$DEBUG" = true ]; then
        mvn_cmd="$mvn_cmd -X -Dlogging.level.com.exploresg=DEBUG"
    fi
    
    if [ ! -z "$PROFILE" ]; then
        mvn_cmd="$mvn_cmd -P$PROFILE"
    fi
    
    if [ ! -z "$PORT" ]; then
        mvn_cmd="$mvn_cmd -Dserver.port=$PORT"
    fi
    
    print_status "Executing: $mvn_cmd"
    eval $mvn_cmd
    
    print_success "Database integration tests completed!"
}

# Function to run all integration tests
run_all_integration_tests() {
    print_status "Running All Integration Tests..."
    
    local mvn_cmd="./mvnw test -Dtest='*Integration*Test' -Dspring.profiles.active=integration-test"
    
    if [ "$DEBUG" = true ]; then
        mvn_cmd="$mvn_cmd -X -Dlogging.level.com.exploresg=DEBUG"
    fi
    
    if [ ! -z "$PROFILE" ]; then
        mvn_cmd="$mvn_cmd -P$PROFILE"
    fi
    
    if [ ! -z "$PORT" ]; then
        mvn_cmd="$mvn_cmd -Dserver.port=$PORT"
    fi
    
    print_status "Executing: $mvn_cmd"
    eval $mvn_cmd
    
    print_success "All integration tests completed!"
}

# Function to run unit tests only
run_unit_tests() {
    print_status "Running Unit Tests..."
    
    local mvn_cmd="./mvnw test -Dtest='!*Integration*'"
    
    if [ "$DEBUG" = true ]; then
        mvn_cmd="$mvn_cmd -X"
    fi
    
    print_status "Executing: $mvn_cmd"
    eval $mvn_cmd
    
    print_success "Unit tests completed!"
}

# Function to run tests with coverage
run_with_coverage() {
    print_status "Running Tests with Coverage Report..."
    
    local mvn_cmd="./mvnw clean test jacoco:report -Dspring.profiles.active=integration-test"
    
    if [ "$DEBUG" = true ]; then
        mvn_cmd="$mvn_cmd -X"
    fi
    
    if [ ! -z "$PROFILE" ]; then
        mvn_cmd="$mvn_cmd -P$PROFILE"
    fi
    
    print_status "Executing: $mvn_cmd"
    eval $mvn_cmd
    
    print_success "Tests with coverage completed!"
    print_status "Coverage report available at: target/site/jacoco/index.html"
    
    # Try to open coverage report if on macOS or Linux with GUI
    if command -v open > /dev/null 2>&1; then
        print_status "Opening coverage report..."
        open target/site/jacoco/index.html
    elif command -v xdg-open > /dev/null 2>&1; then
        print_status "Opening coverage report..."
        xdg-open target/site/jacoco/index.html
    fi
}

# Function to clean and rebuild
clean_project() {
    print_status "Cleaning and rebuilding project..."
    
    ./mvnw clean compile -B
    
    print_success "Project cleaned and rebuilt!"
}

# Parse command line arguments
DEBUG=false
PROFILE=""
PORT=""

while [[ $# -gt 0 ]]; do
    case $1 in
        --debug)
            DEBUG=true
            shift
            ;;
        --profile=*)
            PROFILE="${1#*=}"
            shift
            ;;
        --port=*)
            PORT="${1#*=}"
            shift
            ;;
        *)
            COMMAND=$1
            shift
            ;;
    esac
done

# Main script logic
main() {
    echo "🚀 ExploreSG Auth Service - Integration Test Runner"
    echo "=================================================="
    
    check_prerequisites
    
    case ${COMMAND:-help} in
        "hello-world")
            run_hello_world_tests
            ;;
        "database")
            run_database_tests
            ;;
        "all")
            run_all_integration_tests
            ;;
        "unit")
            run_unit_tests
            ;;
        "coverage")
            run_with_coverage
            ;;
        "clean")
            clean_project
            ;;
        "help"|*)
            show_usage
            ;;
    esac
    
    print_success "Test execution completed! 🎉"
}

# Run the main function
main