#!/bin/bash
set -e

# =============================================================================
# GitLab Runner Setup Script
# Creates directory structure and copies templates
# =============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TEMPLATES_DIR="$SCRIPT_DIR/templates"
source "$SCRIPT_DIR/config.env"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# Cross-platform sed -i
sedi() {
    if [[ "$OSTYPE" == "darwin"* ]]; then
        sed -i '' "$@"
    else
        sed -i "$@"
    fi
}

# =============================================================================
# Directory structure setup
# =============================================================================
setup_directories() {
    log_info "Setting up directory structure at $RUNNER_BASE_DIR..."
    
    mkdir -p "$RUNNER_BASE_DIR/volumes/config"
    mkdir -p "$RUNNER_BASE_DIR/volumes/cache"

    log_info "Directory structure created."
}

# =============================================================================
# Copy and process templates
# =============================================================================

copy_config_template() {
    log_info "Processing config.toml..."
    
    cp "$TEMPLATES_DIR/config.template.toml" "$RUNNER_BASE_DIR/volumes/config/config.toml"
    
		sedi "s|{{CONCURRENT_JOBS}}|$CONCURRENT_JOBS|g" "$RUNNER_BASE_DIR/volumes/config/config.toml"
    sedi "s|{{RUNNER_NAME}}|$RUNNER_NAME|g" "$RUNNER_BASE_DIR/volumes/config/config.toml"
    sedi "s|{{GITLAB_URL}}|$GITLAB_URL|g" "$RUNNER_BASE_DIR/volumes/config/config.toml"
    sedi "s|{{RUNNER_IMAGE}}|$RUNNER_IMAGE|g" "$RUNNER_BASE_DIR/volumes/config/config.toml"
    sedi "s|{{RUNNER_BASE_DIR}}|$RUNNER_BASE_DIR|g" "$RUNNER_BASE_DIR/volumes/config/config.toml"
    
    log_info "Config created at $RUNNER_BASE_DIR/volumes/config/config.toml"
    log_warn "Edit the config.toml to add your runner token!"
}

# =============================================================================
# Main
# =============================================================================
echo "============================================================================="
echo "GitLab Runner Setup"
echo "============================================================================="
echo ""
echo "Configuration:"
echo "  Runner Name:     $RUNNER_NAME"
echo "  GitLab URL:      $GITLAB_URL"
echo "  Base Directory:  $RUNNER_BASE_DIR"
echo "  Runner Image:    $RUNNER_IMAGE"
echo "  Concurrent Jobs: $CONCURRENT_JOBS"
echo ""

setup_directories
copy_config_template

echo ""
log_info "Setup complete. Next steps:"
echo "  1. Edit the token:     vi $RUNNER_BASE_DIR/volumes/config/config.toml"
echo "  2. Build the image:    ./build.sh"
echo "  3. Start the runner:   ./start.sh"