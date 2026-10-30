#!/usr/bin/env bash
#
# Shared by build-native-image.sh and prepare-native-image-build.sh (sourced, not executed).
#
# Only GraalVM Community Edition may be used, never the commercially licensed Oracle GraalVM.
# GraalVM is taken from mise. Without mise, java has to be on the PATH and is checked the same way.
# ensure_graalvm_community sets GRAALVM_HOME to the checked installation; always run java and
# native-image from there.
#

GRAALVM="graalvm-community-25.0.2"

ensure_graalvm_community() {
  local java_home
  if command -v mise > /dev/null; then
    java_home="$(mise exec "java@${GRAALVM}" -- java -XshowSettings:properties -version 2>&1 | sed -n 's/^ *java\.home = //p')"
  else
    java_home="$(java -XshowSettings:properties -version 2>&1 | sed -n 's/^ *java\.home = //p')"
  fi

  if [[ -z "${java_home}" ]] \
    || ! grep -qx 'IMPLEMENTOR="GraalVM Community"' "${java_home}/release" 2> /dev/null \
    || ! "${java_home}/bin/native-image" --version 2> /dev/null | grep -q "GraalVM CE" \
    || "${java_home}/bin/native-image" --version 2> /dev/null | grep -qi "Oracle GraalVM"; then
    echo "Only GraalVM Community Edition is allowed, aborting. Found:" >&2
    echo "  java.home: ${java_home:-<none>}" >&2
    grep '^IMPLEMENTOR=' "${java_home}/release" 2> /dev/null | sed 's/^/  /' >&2
    "${java_home}/bin/native-image" --version 2>&1 | sed 's/^/  /' >&2
    exit 1
  fi

  GRAALVM_HOME="${java_home}"
  echo "Using GraalVM Community Edition from ${GRAALVM_HOME}"
}
