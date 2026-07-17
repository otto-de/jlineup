#!/usr/bin/env bash
#
# Fetches the latest Chrome for Testing and Amazon Linux 2023 versions
# and prints them for easy updating of .chrome-version and .al2023-version
#

set -euo pipefail

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
echo "  .chrome-version:  $(cat "$(dirname "$0")/../.chrome-version" 2>/dev/null || echo 'not found')"
echo "  .al2023-version:  $(cat "$(dirname "$0")/../.al2023-version" 2>/dev/null || echo 'not found')"
