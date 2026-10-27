#!/usr/bin/env bash
#
# Fetches the latest Chrome for Testing and Amazon Linux 2023 versions
# and prints them for easy updating of .chrome-version and .al2023-version
#
# The lambda Dockerfiles can't read these files themselves (they are outside the docker build context),
# so their ARG defaults (CHROME_MAJOR_VERSION, AMI_LINUX_2023_VERSION) are kept in sync by this script.
# Builds that don't pass these build args (e.g. external pipelines) then still use the current versions.
#
# Usage: check-latest-versions.sh [--update | --sync | --check]
#   --update  write the latest versions to .chrome-version and .al2023-version and sync the Dockerfiles
#   --sync    only sync the Dockerfile defaults with .chrome-version and .al2023-version (no network access)
#   --check   only check if the Dockerfile defaults match .chrome-version and .al2023-version (exit code 1 if not)
#

set -euo pipefail

MODE=show
for arg in "$@"; do
    case "$arg" in
        --update) MODE=update ;;
        --sync) MODE=sync ;;
        --check) MODE=check ;;
        *) echo "Unknown parameter: $arg" >&2; echo "Usage: $0 [--update | --sync | --check]" >&2; exit 1 ;;
    esac
done

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

# Dockerfiles with a CHROME_MAJOR_VERSION and/or AMI_LINUX_2023_VERSION build arg
DOCKERFILES=(
    "$PROJECT_DIR/lambda/Dockerfile"
    "$PROJECT_DIR/lambda/Dockerfile.chrome"
    "$PROJECT_DIR/lambda/Dockerfile.firefox"
)

# Without parameter: prints every Dockerfile default that doesn't match the version files.
# With parameter "fix": updates them.
# Returns 1 if at least one default didn't match.
sync_dockerfiles() {
    local fix="${1:-}"
    local chrome_major al2023 outdated=0
    chrome_major=$(cut -d. -f1 "$PROJECT_DIR/.chrome-version")
    al2023=$(cat "$PROJECT_DIR/.al2023-version")

    for dockerfile in "${DOCKERFILES[@]}"; do
        for pair in "CHROME_MAJOR_VERSION=$chrome_major" "AMI_LINUX_2023_VERSION=$al2023"; do
            local name="${pair%%=*}" expected="${pair#*=}" current
            current=$(sed -n -E "s/^ARG ${name}=\"?([^\"]*)\"?\$/\1/p" "$dockerfile")
            if [ -z "$current" ] || [ "$current" = "$expected" ]; then
                continue
            fi
            outdated=1
            if [ "$fix" = "fix" ]; then
                sed -E -i.bak "s/^ARG ${name}=.*\$/ARG ${name}=\"${expected}\"/" "$dockerfile"
                rm -f "$dockerfile.bak"
                echo "  ${dockerfile#"$PROJECT_DIR"/}: ${name} ${current} -> ${expected}"
            else
                echo "  ${dockerfile#"$PROJECT_DIR"/}: ${name}=${current} (expected ${expected})"
            fi
        done
    done
    return $outdated
}

if [ "$MODE" = sync ]; then
    echo "Syncing Dockerfile defaults with .chrome-version and .al2023-version..."
    sync_dockerfiles fix || true
    exit 0
fi

if [ "$MODE" = check ]; then
    if ! sync_dockerfiles; then
        echo "Dockerfile defaults don't match .chrome-version/.al2023-version. Run $0 --sync" >&2
        exit 1
    fi
    echo "Dockerfile defaults match .chrome-version and .al2023-version."
    exit 0
fi

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

if [ "$MODE" = update ]; then
    if [ -z "$CHROME_VERSION" ] || [ -z "$AL2023_VERSION" ]; then
        echo "" >&2
        echo "Could not determine latest versions, not updating." >&2
        exit 1
    fi
    printf '%s' "$CHROME_VERSION" > "$PROJECT_DIR/.chrome-version"
    printf '%s' "$AL2023_VERSION" > "$PROJECT_DIR/.al2023-version"
    echo ""
    echo "Updated .chrome-version and .al2023-version to the latest versions."
    echo "Syncing Dockerfile defaults..."
    sync_dockerfiles fix || true
fi
