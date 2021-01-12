#!/usr/bin/env bash

if [ -z ${GRAAL_HOME+x} ]; then
  GRAAL_HOME="../../graalvm/graalvm-ce-java8-19.3.1"
fi

echo "GRAAL_HOME is set to '$GRAAL_HOME'"

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
-H:ReflectionConfigurationFiles=graalvm/reflect.json \
--initialize-at-build-time=com.fasterxml.jackson,javassist.ClassPool \
--verbose \
`#--static` \
`#-H:+TraceSecurityServices` \
`#-H:+TraceClassInitialization` \
-jar build/libs/jlineup-cli-4.3.0-all.jar

echo ""
echo "DONE BUILDING NATIVE IMAGE"
echo ""

#exit

echo ""
echo "STARTING TEST RUN"
echo ""

mv jlineup-cli-4.3.0-all build/libs/jlineup-cli-4.3.0-all
rm ~/.m2/repository/webdriver -rf
./build/libs/jlineup-cli-4.3.0-all -Dwdm.architecture=X64 --config graalvm/lineup.json --step before

set +e

./build/libs/jlineup-cli-4.3.0-all -Dwdm.architecture=X64 --config graalvm/lineup.json --step after

set -e

mv ./build/libs/jlineup-cli-*-all ./build/libs/jlineup
cd ./build/libs

tar -czf jlineup-cli-linux-amd64.tar.gz jlineup

echo ""
echo "FINISHED"
echo ""
