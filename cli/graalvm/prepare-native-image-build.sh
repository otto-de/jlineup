#!/usr/bin/env bash
#
# Updates the reachability metadata (reflection, resources etc. for native-image) by running jlineup on
# the JVM with the GraalVM tracing agent. New findings are merged into the existing file.
#
# Usage: prepare-native-image-build.sh [cli|cli-lambda]   (default: cli)
#
#   cli         Updates cli/graalvm/reachability-metadata.json.
#               Runs the CLI acceptance test configs and a few real pages.
#               Needs Chrome/Chromium, Firefox and internet access. Run it on Linux and macOS to cover both.
#   cli-lambda  Updates cli-lambda/graalvm/reachability-metadata.json (on top of the cli metadata).
#               All screenshots are taken by the Lambda function, so this needs a deployed JLineup Lambda
#               function in JLINEUP_LAMBDA_FUNCTION_NAME and AWS credentials. aws_lambda_acceptance_test.yml
#               runs this against the acceptance test stack.
#
# Only GraalVM Community Edition is used, see graalvm-community.sh for the version and the check.
#
set -euo pipefail

VARIANT="${1:-cli}"
CLI_DIR="$(cd "$(dirname "$0")/.." && pwd)"
ROOT_DIR="$(dirname "${CLI_DIR}")"

# shellcheck source=SCRIPTDIR/graalvm-community.sh
source "${CLI_DIR}/graalvm/graalvm-community.sh"

case "${VARIANT}" in
  cli)
    CONFIG_DIR="${CLI_DIR}/graalvm"
    EXCLUDED_JARS='^$'
    ;;
  cli-lambda)
    CONFIG_DIR="${ROOT_DIR}/cli-lambda/graalvm"
    # Keep in sync with build-native-image.sh
    EXCLUDED_JARS='/(netty-|log4j-|aws-lambda-java-log4j2-)'
    : "${JLINEUP_LAMBDA_FUNCTION_NAME:?Set JLINEUP_LAMBDA_FUNCTION_NAME to a deployed JLineup Lambda function}"
    ;;
  *)
    echo "Usage: $0 [cli|cli-lambda]" >&2
    exit 1
    ;;
esac

ensure_graalvm_community

echo "Building jlineup-${VARIANT} and collecting its runtime classpath"
"${ROOT_DIR}/gradlew" -p "${ROOT_DIR}" ":jlineup-${VARIANT}:installDist"
# shellcheck disable=SC2010 # jar file names are plain
CLASSPATH="$(ls "${ROOT_DIR}/${VARIANT}/build/install/jlineup-${VARIANT}/lib/"*.jar | grep -vE "${EXCLUDED_JARS}" | tr '\n' ':')"

mkdir -p "${CONFIG_DIR}"
WORK_DIR="$(mktemp -d)"
trap 'rm -rf "${WORK_DIR}"' EXIT
cd "${WORK_DIR}"

# Some runs are expected to fail (error handling is traced, too), so the exit code is only logged
jlineup() {
  echo "Tracing: jlineup $*"
  local exit_code=0
  "${GRAALVM_HOME}/bin/java" -agentlib:native-image-agent=config-merge-dir="${CONFIG_DIR}" \
    -cp "${CLASSPATH}" de.otto.jlineup.cli.Main "$@" || exit_code=$?
  echo "Exit code: ${exit_code}"
}

jlineup --version
jlineup --help
jlineup --print-config
jlineup --print-example

if [[ "${VARIANT}" == "cli" ]]; then
  ACCEPTANCE="${CLI_DIR}/src/test/resources/acceptance"

  # Runs a config from the CLI acceptance tests, which use local test pages, in its own working directory
  acceptance() {
    local name="$1"
    shift
    jlineup --working-dir "${WORK_DIR}/${name}" --replace-in-url="###CWD###=${CLI_DIR}" "$@"
  }

  for config in acceptance_chrome-headless acceptance_chrome_devices acceptance_chrome_progressive_jpg \
    acceptance_chrome_svg acceptance_chrome-wait_for_selectors acceptance_long_url acceptance_reportv2; do
    acceptance "${config}" --config "${ACCEPTANCE}/${config}.lineup.json" --step before
    acceptance "${config}" --config "${ACCEPTANCE}/${config}.lineup.json" --step after
  done

  acceptance firefox --config "${ACCEPTANCE}/acceptance_chrome-headless.lineup.json" --override-browser firefox-headless --step before
  acceptance firefox --config "${ACCEPTANCE}/acceptance_chrome-headless.lineup.json" --override-browser firefox-headless --step after
  acceptance firefox-variants --config "${ACCEPTANCE}/acceptance_firefox_variant1.lineup.json" --step before
  acceptance firefox-variants --config "${ACCEPTANCE}/acceptance_firefox_variant2.lineup.json" --step after
  acceptance remove-selectors --config "${ACCEPTANCE}/acceptance_chrome-remove_selectors.lineup.json" --replace-in-url="###NUM###=1" --step before
  acceptance remove-selectors --config "${ACCEPTANCE}/acceptance_chrome-remove_selectors.lineup.json" --replace-in-url="###NUM###=2" --step after
  acceptance merge --config "${ACCEPTANCE}/acceptance.lineup.json" --merge-config "${ACCEPTANCE}/acceptance-merge.lineup.json" --step before
  acceptance merge --config "${ACCEPTANCE}/acceptance.lineup.json" --merge-config "${ACCEPTANCE}/acceptance-merge.lineup.json" --step after
  acceptance merge --config "${ACCEPTANCE}/acceptance.lineup.json" --step before --keep-existing
  acceptance compare --config "${ACCEPTANCE}/acceptance_chrome-headless.lineup.json" --step before
  acceptance compare --config "${ACCEPTANCE}/acceptance_chrome-headless.lineup.json" --step after
  acceptance compare --config "${ACCEPTANCE}/acceptance_chrome-headless.lineup.json" --step compare
  acceptance url --url "file://${ACCEPTANCE}/webpage/test.html"
  acceptance labels --config "${ACCEPTANCE}/acceptance_labels.lineup.yaml" --print-config --before-label "Before" --after-label "After"

  # Error handling
  acceptance errors --config "${ACCEPTANCE}/acceptance_chrome-wait_for_selectors_fails_but_no_error.lineup.json" --step before
  acceptance errors --config "${ACCEPTANCE}/acceptance_chrome-wait_for_selectors_fails_with_error.lineup.json" --step before
  acceptance errors --config "${ACCEPTANCE}/acceptance_wrong_js.lineup.json"
  acceptance errors --config "${ACCEPTANCE}/acceptance_wrong_url_chrome.lineup.json"
  acceptance errors --config "${ACCEPTANCE}/acceptance_wrong_url_firefox.lineup.json"
  acceptance errors --config "${ACCEPTANCE}/acceptance_no_urls.lineup.json"

  # Real pages
  LINEUP_CONFIGS="${CLI_DIR}/graalvm"
  acceptance real --config "${LINEUP_CONFIGS}/lineup_chrome_headless.json" --step before
  acceptance real --config "${LINEUP_CONFIGS}/lineup_chrome_headless.json" --step after
  acceptance real-firefox --config "${LINEUP_CONFIGS}/lineup_firefox_headless.json" --step before
  acceptance real-url --url www.otto.de --step before
fi

if [[ "${VARIANT}" == "cli-lambda" ]]; then
  LAMBDA_CONFIG="${ROOT_DIR}/cli-lambda/src/test/resources/acceptance/acceptance_cli_lambda.lineup.json"

  jlineup --working-dir "${WORK_DIR}/lambda" --config "${LAMBDA_CONFIG}" --step before
  jlineup --working-dir "${WORK_DIR}/lambda" --config "${LAMBDA_CONFIG}" --step after

  # The AWS SDK loads some credential providers by reflection, so trace the profile based ones, too.
  # The SSO profile can't log in, but resolving it loads the SSO provider classes.
  export AWS_CONFIG_FILE="${WORK_DIR}/aws-config"
  export AWS_SHARED_CREDENTIALS_FILE="${WORK_DIR}/aws-credentials"
  cat > "${AWS_CONFIG_FILE}" << EOF
[profile jlineup-sso-trace]
sso_session = jlineup-sso-trace
sso_account_id = 000000000000
sso_role_name = jlineup-trace
region = ${JLINEUP_LAMBDA_AWS_REGION:-eu-central-1}

[sso-session jlineup-sso-trace]
sso_start_url = https://jlineup-trace.awsapps.com/start
sso_region = ${JLINEUP_LAMBDA_AWS_REGION:-eu-central-1}
EOF
  if [[ -n "${AWS_ACCESS_KEY_ID:-}" && -n "${AWS_SECRET_ACCESS_KEY:-}" ]]; then
    cat > "${AWS_SHARED_CREDENTIALS_FILE}" << EOF
[jlineup-static-trace]
aws_access_key_id = ${AWS_ACCESS_KEY_ID}
aws_secret_access_key = ${AWS_SECRET_ACCESS_KEY}
EOF
    # shellcheck disable=SC2030 # the profile is only meant for this run
    (
      unset AWS_ACCESS_KEY_ID AWS_SECRET_ACCESS_KEY AWS_SESSION_TOKEN
      export AWS_PROFILE=jlineup-static-trace
      jlineup --working-dir "${WORK_DIR}/lambda-profile" --config "${LAMBDA_CONFIG}" --step before
    )
  fi
  # shellcheck disable=SC2030,SC2031 # the profile is only meant for this run
  (
    unset AWS_ACCESS_KEY_ID AWS_SECRET_ACCESS_KEY AWS_SESSION_TOKEN
    export AWS_PROFILE=jlineup-sso-trace
    jlineup --working-dir "${WORK_DIR}/lambda-sso" --config "${LAMBDA_CONFIG}" --step before
  )
fi

echo "Done: ${CONFIG_DIR}/reachability-metadata.json"
