#!/usr/bin/env bash
#
# Builds a native jlineup binary with GraalVM Community Edition.
#
# Usage: build-native-image.sh [cli|cli-lambda]   (default: cli)
#
#   cli         jlineup-cli, takes screenshots with locally installed browsers
#   cli-lambda  jlineup-cli-lambda, takes screenshots with JLineup's AWS Lambda function
#
# Only GraalVM Community Edition is used, see graalvm-community.sh for the version and the check.
# native-image supports AWT (needed for screenshots and reports) only on Linux and Windows, so this
# script builds for Linux only. The binary needs the shared libraries native-image puts next to it,
# which is why they are packaged together.
#
# More memory for native-image can be set via NATIVE_IMAGE_OPTIONS, e.g. NATIVE_IMAGE_OPTIONS=-J-Xmx4g
#
set -euo pipefail

VARIANT="${1:-cli}"
CLI_DIR="$(cd "$(dirname "$0")/.." && pwd)"
ROOT_DIR="$(dirname "${CLI_DIR}")"
OUT_DIR="${ROOT_DIR}/${VARIANT}/build/native"

# shellcheck source=SCRIPTDIR/graalvm-community.sh
source "${CLI_DIR}/graalvm/graalvm-community.sh"

case "${VARIANT}" in
  cli)
    CONFIG_DIRS="${CLI_DIR}/graalvm"
    EXCLUDED_JARS='^$'
    ;;
  cli-lambda)
    CONFIG_DIRS="${CLI_DIR}/graalvm,${ROOT_DIR}/cli-lambda/graalvm"
    # The AWS clients used here are Apache (sync) and CRT (async), so the SDK's Netty client is not needed.
    # log4j is only used inside the Lambda function itself.
    EXCLUDED_JARS='/(netty-|log4j-|aws-lambda-java-log4j2-)'
    ;;
  *)
    echo "Usage: $0 [cli|cli-lambda]" >&2
    exit 1
    ;;
esac

ensure_graalvm_community

if [[ "$(uname -s)" != "Linux" ]]; then
  echo "A native jlineup binary can only take screenshots on Linux, aborting." >&2
  exit 1
fi

case "$(uname -m)" in
  x86_64) ARCH="amd64" ;;
  aarch64 | arm64) ARCH="arm64" ;;
  *) ARCH="$(uname -m)" ;;
esac
DIST_NAME="jlineup-${VARIANT}-linux-${ARCH}"

echo "Building jlineup-${VARIANT} and collecting its runtime classpath"
"${ROOT_DIR}/gradlew" -p "${ROOT_DIR}" ":jlineup-${VARIANT}:installDist"
# shellcheck disable=SC2010 # jar file names are plain
CLASSPATH="$(ls "${ROOT_DIR}/${VARIANT}/build/install/jlineup-${VARIANT}/lib/"*.jar | grep -vE "${EXCLUDED_JARS}" | tr '\n' ':')"

echo "Building native image with GraalVM Community Edition from ${GRAALVM_HOME}"
rm -rf "${OUT_DIR:?}/${DIST_NAME}" "${OUT_DIR}/${DIST_NAME}.tar.gz"
mkdir -p "${OUT_DIR}/${DIST_NAME}"

"${GRAALVM_HOME}/bin/native-image" \
  -cp "${CLASSPATH}" \
  -H:+UnlockExperimentalVMOptions \
  -H:ConfigurationFileDirectories="${CONFIG_DIRS}" \
  -H:IncludeResources='.*properties$|.*html$|.*xml$' \
  -H:+ReportExceptionStackTraces \
  -H:+AddAllCharsets \
  --enable-url-protocols=http,https \
  --no-fallback \
  -o "${OUT_DIR}/${DIST_NAME}/jlineup" \
  de.otto.jlineup.cli.Main

echo "Smoke test"
"${OUT_DIR}/${DIST_NAME}/jlineup" --version

tar -czf "${OUT_DIR}/${DIST_NAME}.tar.gz" -C "${OUT_DIR}" "${DIST_NAME}"
echo "Done: ${OUT_DIR}/${DIST_NAME}.tar.gz"
