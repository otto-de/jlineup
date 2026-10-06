#!/usr/bin/env bash
#
# Fetches the latest Chrome for Testing and Amazon Linux 2023 versions
# and prints them for easy updating of .chrome-version and .al2023-version
#
# Usage: check-latest-versions.sh [--update]
#   --update  write the latest versions to .chrome-version and .al2023-version
#

set -euo pipefail

UPDATE=false
for arg in "$@"; do
    case "$arg" in
        --update) UPDATE=true ;;
        *) echo "Unknown parameter: $arg" >&2; echo "Usage: $0 [--update]" >&2; exit 1 ;;
    esac
done

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

echo "Fetching latest versions..."
echo ""

# Chrome for Testing - get latest stable version
CHROME_VERSION=$(curl -s "https://googlechromelabs.github.io/chrome-for-testing/last-known-good-versions.json" \
    | grep -o '"Stable":{[^}]*}' \
    | grep -o '"version":"[^"]*"' \
    | cut -d'"' -f4)

echo "Chrome for Testing (Stable): $CHROME_VERSION"

# Amazon Linux 2023 - scrape latest version from release notes
AL2023_VERSION=$(curl -s "https://docs.aws.amazon.com/linux/al2023/release-notes/relnotes-2023.12.html" \
    | grep -o '2023\.[0-9]*\.[0-9]*' \
    | sort -V \
    | tail -1)

echo "Amazon Linux 2023:           $AL2023_VERSION"

echo ""
echo "Current versions in project:"
echo "  .chrome-version:  $(cat "$PROJECT_DIR/.chrome-version" 2>/dev/null || echo 'not found')"
echo "  .al2023-version:  $(cat "$PROJECT_DIR/.al2023-version" 2>/dev/null || echo 'not found')"

if [ "$UPDATE" = true ]; then
    if [ -z "$CHROME_VERSION" ] || [ -z "$AL2023_VERSION" ]; then
        echo "" >&2
        echo "Could not determine latest versions, not updating." >&2
        exit 1
    fi
    printf '%s' "$CHROME_VERSION" > "$PROJECT_DIR/.chrome-version"
    printf '%s' "$AL2023_VERSION" > "$PROJECT_DIR/.al2023-version"
    echo ""
    echo "Updated .chrome-version and .al2023-version to the latest versions."
fi
