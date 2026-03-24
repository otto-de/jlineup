#!/usr/bin/env bash

set -euo pipefail

# Only login if credentials are expired or missing
if ! aws sts get-caller-identity --profile jlineup &>/dev/null; then
  echo "AWS credentials expired or missing – logging in..."
  aws login --profile jlineup
else
  echo "✔ AWS credentials still valid – skipping login."
fi

export JLINEUP_LAMBDA_ACCEPTANCE_TEST_ENABLED=true
export JLINEUP_LAMBDA_ACCEPTANCE_FORCE_DEPLOY=true
export AWS_ACCOUNT_ID=$(aws sts get-caller-identity --profile jlineup --query "Account" --output text)

eval $(aws configure export-credentials --profile jlineup --format env)
unset AWS_PROFILE

./gradlew publishToMavenLocal
./gradlew build assemble -x test

# Example run with real configuration (make sure to have the correct function name and config file path):
#./gradlew :jlineup-cli-lambda:run --args='-F jlineup-lambda-acc-test-fn -s before -c ../lineup.json'

./gradlew :jlineup-lambda:test --tests '*LambdaAcceptanceTest.shouldInvoke*'