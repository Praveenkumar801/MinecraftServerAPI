#!/bin/bash

# Script to manually trigger the release workflow for all existing releases
# This is needed because releases created via gh CLI don't automatically trigger workflows

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging functions
log_info() {
    printf "${BLUE}[INFO]${NC} %s\n" "$1"
}

log_success() {
    printf "${GREEN}[SUCCESS]${NC} %s\n" "$1"
}

log_warning() {
    printf "${YELLOW}[WARNING]${NC} %s\n" "$1"
}

log_error() {
    printf "${RED}[ERROR]${NC} %s\n" "$1"
}

# Check if gh is installed
if ! command -v gh &> /dev/null; then
    log_error "GitHub CLI (gh) is not installed. Please install it with: brew install gh"
    exit 1
fi

# Check if we're authenticated
if ! gh auth status &> /dev/null; then
    log_error "Not authenticated with GitHub. Please run 'gh auth login'."
    exit 1
fi

log_info "Fetching all releases..."

# Get all releases
RELEASES=$(gh release list --limit 100 --json tagName,name -q '.[] | .tagName')

if [ -z "$RELEASES" ]; then
    log_warning "No releases found!"
    exit 0
fi

log_info "Found releases:"
echo "$RELEASES" | while read -r tag; do
    printf "  - %s\n" "$tag"
done

printf "\n"
log_info "Triggering release workflow for each release..."
printf "\n"

SUCCESS_COUNT=0
FAIL_COUNT=0

for TAG in $RELEASES; do
    log_info "Processing release: $TAG"
    
    # Trigger workflow dispatch for this release
    if gh workflow run release.yml \
        -f release_tag="$TAG" \
        --ref main > /dev/null 2>&1; then
        log_success "Workflow triggered for $TAG"
        ((SUCCESS_COUNT++))
    else
        log_error "Failed to trigger workflow for $TAG"
        ((FAIL_COUNT++))
    fi
done

printf "\n"
printf "========================================\n"
printf "                SUMMARY                 \n"
printf "========================================\n"
printf "\n"

log_info "Successfully triggered: $SUCCESS_COUNT"
log_info "Failed: $FAIL_COUNT"

if [ $FAIL_COUNT -eq 0 ]; then
    log_success "All workflows triggered successfully! 🎉"
else
    log_warning "Some workflows failed to trigger. Check the errors above."
fi