#!/usr/bin/env bash

if [ -z ${GRAAL_HOME+x} ]; then
  GRAAL_HOME="../../graalvm/graalvm-ce-java11-19.3.0"
  #GRAAL_HOME="../../graalvm/graalvm-ce-java8-19.3.0"
  #GRAAL_HOME="../../graalvm/graalvm-ce-19.2.1"
fi

echo "GRAAL_HOME is set to '$GRAAL_HOME'"

BASEDIR=$(dirname "$0")
cd "${BASEDIR}"/..

GRAAL_HOME=$(readlink -m ${GRAAL_HOME})
JAVA_HOME=${GRAAL_HOME}

set -e

cd ..
./gradlew compileJava shadowJar
cd cli
#../../graalvm/graalvm/bin/java -agentlib:native-image-agent -jar jlineup-cli-4.0.0-SNAPSHOT-all.jar --url https://www.otto.de --step after
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
-H:+TraceSecurityServices \
-H:+TraceClassInitialization \
-jar build/libs/jlineup-cli-4.0.0-SNAPSHOT-all.jar

echo ""
echo "DONE BUILDING NATIVE IMAGE"
echo ""

exit

mv jlineup-cli-4.0.0-SNAPSHOT-all build/libs/jlineup-cli-4.0.0-SNAPSHOT-all
rm ~/.m2/repository/webdriver -rf
./build/libs/jlineup-cli-4.0.0-SNAPSHOT-all -Dwdm.architecture=X64 --config graalvm/lineup.json --step before
./build/libs/jlineup-cli-4.0.0-SNAPSHOT-all -Dwdm.architecture=X64 --config graalvm/lineup.json --step after
