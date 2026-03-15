#!/usr/bin/env bash
# =============================================================================
# UK Self-Employment Manager - Installation Script (Linux / macOS)
# =============================================================================
#
# Usage:
#   ./install.sh              Build and run the application
#   ./install.sh --build      Build only (no run)
#   ./install.sh --package    Build and create native installer
#   ./install.sh --check      Check prerequisites only
#   ./install.sh --help       Show this help message
#
# =============================================================================

set -euo pipefail

# --- Configuration -----------------------------------------------------------

REQUIRED_JAVA_VERSION=21
REQUIRED_MAVEN_MAJOR=3
REQUIRED_MAVEN_MINOR=6
APP_NAME="UK Self-Employment Manager"

# --- Colors -------------------------------------------------------------------

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# --- Helper Functions ---------------------------------------------------------

info()    { echo -e "${BLUE}[INFO]${NC}  $*"; }
success() { echo -e "${GREEN}[OK]${NC}    $*"; }
warn()    { echo -e "${YELLOW}[WARN]${NC}  $*"; }
error()   { echo -e "${RED}[ERROR]${NC} $*"; }

show_help() {
    cat << 'EOF'
UK Self-Employment Manager - Installation Script

Usage:
  ./install.sh              Build and run the application
  ./install.sh --build      Build only (no run)
  ./install.sh --package    Build and create native installer
  ./install.sh --check      Check prerequisites only
  ./install.sh --help       Show this help message

Prerequisites:
  - Java 21 or later (JDK, not JRE)
  - Maven 3.6 or later

The script will check for prerequisites and guide you through
installation if anything is missing.
EOF
}

detect_os() {
    local os
    os="$(uname -s)"
    case "$os" in
        Linux*)  echo "linux" ;;
        Darwin*) echo "macos" ;;
        CYGWIN*|MINGW*|MSYS*) echo "windows" ;;
        *)       echo "unknown" ;;
    esac
}

# --- Prerequisite Checks -----------------------------------------------------

check_java() {
    info "Checking Java installation..."

    if ! command -v java &> /dev/null; then
        error "Java is not installed or not in PATH."
        suggest_java_install
        return 1
    fi

    local java_version
    java_version=$(java -version 2>&1 | head -1 | sed -E 's/.*"([0-9]+).*/\1/')

    if [[ "$java_version" -ge "$REQUIRED_JAVA_VERSION" ]]; then
        success "Java $java_version found (required: $REQUIRED_JAVA_VERSION+)"
    else
        error "Java $java_version found, but Java $REQUIRED_JAVA_VERSION+ is required."
        suggest_java_install
        return 1
    fi

    # Check it's a JDK (javac must exist)
    if ! command -v javac &> /dev/null; then
        error "JRE detected but JDK is required (javac not found)."
        suggest_java_install
        return 1
    fi

    success "JDK confirmed (javac available)"
    return 0
}

check_maven() {
    info "Checking Maven installation..."

    if ! command -v mvn &> /dev/null; then
        error "Maven is not installed or not in PATH."
        suggest_maven_install
        return 1
    fi

    local mvn_version
    mvn_version=$(mvn --version 2>&1 | head -1 | sed -E 's/.*([0-9]+\.[0-9]+\.[0-9]+).*/\1/')

    local mvn_major mvn_minor
    mvn_major=$(echo "$mvn_version" | cut -d. -f1)
    mvn_minor=$(echo "$mvn_version" | cut -d. -f2)

    if [[ "$mvn_major" -gt "$REQUIRED_MAVEN_MAJOR" ]] || \
       { [[ "$mvn_major" -eq "$REQUIRED_MAVEN_MAJOR" ]] && [[ "$mvn_minor" -ge "$REQUIRED_MAVEN_MINOR" ]]; }; then
        success "Maven $mvn_version found (required: ${REQUIRED_MAVEN_MAJOR}.${REQUIRED_MAVEN_MINOR}+)"
    else
        error "Maven $mvn_version found, but ${REQUIRED_MAVEN_MAJOR}.${REQUIRED_MAVEN_MINOR}+ is required."
        suggest_maven_install
        return 1
    fi

    return 0
}

suggest_java_install() {
    local os
    os=$(detect_os)
    echo ""
    warn "Install Java $REQUIRED_JAVA_VERSION+ (Temurin recommended):"
    case "$os" in
        linux)
            echo "  Ubuntu/Debian:  sudo apt install temurin-21-jdk"
            echo "  Fedora/RHEL:    sudo dnf install java-21-openjdk-devel"
            echo "  Arch:           sudo pacman -S jdk21-openjdk"
            echo "  SDKMAN:         sdk install java 21-tem"
            ;;
        macos)
            echo "  Homebrew:       brew install --cask temurin@21"
            echo "  SDKMAN:         sdk install java 21-tem"
            ;;
        *)
            echo "  Download from:  https://adoptium.net/temurin/releases/"
            ;;
    esac
    echo ""
}

suggest_maven_install() {
    local os
    os=$(detect_os)
    echo ""
    warn "Install Maven ${REQUIRED_MAVEN_MAJOR}.${REQUIRED_MAVEN_MINOR}+:"
    case "$os" in
        linux)
            echo "  Ubuntu/Debian:  sudo apt install maven"
            echo "  Fedora/RHEL:    sudo dnf install maven"
            echo "  SDKMAN:         sdk install maven"
            ;;
        macos)
            echo "  Homebrew:       brew install maven"
            echo "  SDKMAN:         sdk install maven"
            ;;
        *)
            echo "  Download from:  https://maven.apache.org/download.cgi"
            ;;
    esac
    echo ""
}

check_prerequisites() {
    local failed=0

    echo ""
    echo "============================================="
    echo "  $APP_NAME"
    echo "  Prerequisite Check"
    echo "============================================="
    echo ""

    check_java  || failed=1
    check_maven || failed=1

    echo ""

    if [[ "$failed" -eq 0 ]]; then
        success "All prerequisites met!"
    else
        error "Some prerequisites are missing. Please install them and re-run this script."
    fi

    return $failed
}

# --- Build & Run --------------------------------------------------------------

build_project() {
    info "Building $APP_NAME..."
    echo ""

    mvn clean install -DskipTests --no-transfer-progress

    echo ""
    success "Build completed successfully!"
}

run_app() {
    info "Starting $APP_NAME..."
    echo ""

    mvn -pl app javafx:run
}

create_installer() {
    local os
    os=$(detect_os)

    info "Creating native installer for $os..."
    echo ""

    mvn -pl app -Ppackage jpackage:jpackage

    echo ""
    success "Native installer created!"
    info "Check: app/target/installer/"

    case "$os" in
        linux)
            echo ""
            info "Install the .deb package:"
            echo "  sudo dpkg -i app/target/installer/*.deb"
            echo ""
            info "Or the .rpm package:"
            echo "  sudo rpm -i app/target/installer/*.rpm"
            ;;
        macos)
            echo ""
            info "Install the .dmg package:"
            echo "  Open app/target/installer/*.dmg and drag to Applications"
            ;;
    esac
}

setup_env() {
    if [[ ! -f .env ]]; then
        if [[ -f .env.example ]]; then
            warn "No .env file found. Creating from .env.example..."
            cp .env.example .env
            echo ""
            warn "Please edit .env with your HMRC credentials before using HMRC features."
            warn "See: https://developer.service.hmrc.gov.uk/developer/registration"
            echo ""
        fi
    else
        success ".env file already exists"
    fi
}

# --- Main ---------------------------------------------------------------------

main() {
    local mode="${1:-run}"

    case "$mode" in
        --help|-h)
            show_help
            exit 0
            ;;
        --check)
            check_prerequisites
            exit $?
            ;;
        --build)
            check_prerequisites || exit 1
            setup_env
            build_project
            ;;
        --package)
            check_prerequisites || exit 1
            setup_env
            build_project
            create_installer
            ;;
        run|"")
            check_prerequisites || exit 1
            setup_env
            build_project
            run_app
            ;;
        *)
            error "Unknown option: $mode"
            echo ""
            show_help
            exit 1
            ;;
    esac
}

main "$@"
