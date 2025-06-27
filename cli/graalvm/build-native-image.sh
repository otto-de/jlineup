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
./gradlew compileJava shadowJar

echo ""
echo "START BUILDING NATIVE IMAGE"
echo ""

cd cli
#../../graalvm/graalvm/bin/java -agentlib:native-image-agent -jar jlineup-cli-4.1.1-SNAPSHOT-all.jar --url https://www.otto.de --step after
#-J-Djava.security.properties=graalvm/java.security.overrides \

"${GRAAL_HOME}"/bin/native-image \
--no-server \
-H:IncludeResources='.*properties$|.*html$|.*xml$' \
-H:+ReportExceptionStackTraces \
-H:+JNI \
--enable-https \
--enable-http \
--enable-url-protocols=http,https \
--enable-all-security-services \
--no-fallback \
--allow-incomplete-classpath \
-H:+AddAllCharsets \
`#-H:ReflectionConfigurationFiles=graalvm/reflect.json` \
-H:ConfigurationFileDirectories=graalvm/ \
`#--initialize-at-build-time=com.fasterxml.jackson,javassist.ClassPool` \
--verbose \
--report-unsupported-elements-at-runtime \
`#--static` \
`#-H:+TraceSecurityServices` \
`#-H:+TraceClassInitialization` \
-jar build/libs/jlineup-cli-4.13.8-all.jar

echo ""
echo "DONE BUILDING NATIVE IMAGE"
echo ""

#exit

echo ""
echo "STARTING TEST RUN"
echo ""

mv jlineup-cli-4.13.8-all build/libs/jlineup-cli-4.13.8-all
rm ~/.m2/repository/webdriver -rf
./build/libs/jlineup-cli-4.13.8-all -Dwdm.architecture=X64 --config graalvm/lineup_chrome_headless.json --step before

set +e

./build/libs/jlineup-cli-4.13.8-all -Dwdm.architecture=X64 --config graalvm/lineup_chrome_headless.json --step after

set -e

mv ./build/libs/jlineup-cli-*-all ./build/libs/jlineup
cd ./build/libs

tar -czf jlineup-cli-linux-amd64.tar.gz jlineup

echo ""
echo "FINISHED"
echo ""
