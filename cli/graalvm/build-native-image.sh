#!/usr/bin/env bash

set -e

# Go into jlineup-cli root
cd ..

DDIIRR=$(pwd)

cd ../../graalvm/graalvm
JAVA_HOME=$(pwd)

cd "$DDIIRR"

cd ..
./gradlew compileJava shadowJar
cd cli
#../../graalvm/graalvm/bin/java -agentlib:native-image-agent -jar jlineup-cli-4.0.0-SNAPSHOT-all.jar --url https://www.otto.de --step after
../../graalvm/graalvm/bin/native-image --no-server -J-Djava.security.properties=graalvm/java.security.overrides -H:IncludeResources='.*properties$|.*html$' -H:+ReportExceptionStackTraces -H:+JNI --enable-https --enable-http --enable-url-protocols=http,https --enable-all-security-services --no-fallback --allow-incomplete-classpath -H:+AddAllCharsets -H:ReflectionConfigurationFiles=graalvm/reflect.json --initialize-at-build-time=com.fasterxml.jackson,javassist.ClassPool --static --verbose -jar build/libs/jlineup-cli-4.0.0-SNAPSHOT-all.jar
mv jlineup-cli-4.0.0-SNAPSHOT-all build/libs/jlineup-cli-4.0.0-SNAPSHOT-all
rm ~/.m2/repository/webdriver -rf
./build/libs/jlineup-cli-4.0.0-SNAPSHOT-all -Dwdm.architecture=X64 --config graalvm/lineup.json --step before
./build/libs/jlineup-cli-4.0.0-SNAPSHOT-all -Dwdm.architecture=X64 --config graalvm/lineup.json --step after
