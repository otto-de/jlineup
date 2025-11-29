#!/usr/bin/env bash

## Uncomment to downloaded latest nightly
#set -x
#wget -O ../../graalvm/graalvm-dev.tar.gz https://github.com/graalvm/graalvm-ce-dev-builds/releases/latest/download/graalvm-ce-java11-linux-amd64-dev.tar.gz
#mkdir ../../graalvm/graal-dev && tar -xzf ../../graalvm/graalvm-dev.tar.gz -C ../../graalvm/graal-dev --strip-components 1
#set +x

if [ -z ${GRAAL_HOME+x} ]; then

  if [[ $JAVA_HOME == *"grl"* ]]; then
    GRAAL_HOME=$JAVA_HOME
  else
    GRAAL_HOME="../../graalvm/graalvm-ce-java11-21.1.0/"
    #GRAAL_HOME="../../graalvm/graalvm-ce-java11-19.3.1/"
  fi
fi

"${GRAAL_HOME}"/bin/gu install native-image
echo "GRAAL_HOME is set to '$GRAAL_HOME'"

sudo apt-get install libfreetype6-dev

#"${GRAAL_HOME}"/bin/native-image --expert-options-all

BASEDIR=$(dirname "$0")
cd "${BASEDIR}"/..

GRAAL_HOME=$(readlink -m ${GRAAL_HOME})
JAVA_HOME=${GRAAL_HOME}

set -e

echo ""
echo "BUILDING JAR"
echo ""

cd ..
#./gradlew compileJava shadowJar

echo ""
echo "DOING STUFF"
echo ""

echo "$JAVA_HOME"

cd cli
"${GRAAL_HOME}"/bin/java -agentlib:native-image-agent=config-output-dir=graalvm -jar build/libs/jlineup-cli-4.14.2-all.jar --config graalvm/lineup_chrome_headless.json --step before || true
"${GRAAL_HOME}"/bin/java -agentlib:native-image-agent=config-merge-dir=graalvm -jar build/libs/jlineup-cli-4.14.2-all.jar --config graalvm/lineup_chrome_headless.json --step after || true
"${GRAAL_HOME}"/bin/java -agentlib:native-image-agent=config-merge-dir=graalvm -jar build/libs/jlineup-cli-4.14.2-all.jar --config graalvm/lineup_firefox_headless.json --step before || true
"${GRAAL_HOME}"/bin/java -agentlib:native-image-agent=config-merge-dir=graalvm -jar build/libs/jlineup-cli-4.14.2-all.jar --config graalvm/lineup_chrome.json --step before || true
"${GRAAL_HOME}"/bin/java -agentlib:native-image-agent=config-merge-dir=graalvm -jar build/libs/jlineup-cli-4.14.2-all.jar --url www.otto.de --step before || true

#-J-Djava.security.properties=graalvm/java.security.overrides \
