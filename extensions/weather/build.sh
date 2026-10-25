#!/usr/bin/env sh
# Rebuilds the embedded extension dex and prints it as base64 for OpenMeteoDex.kt.
# Needs a JDK (javac) and d8 (from Android build-tools, or com.android.tools.r8.D8 in any
# com.android.tools.build:builder jar). Usage: D8="d8" ./build.sh > classes.b64
set -e
cd "$(dirname "$0")"
out=$(mktemp -d)
javac --release 8 -Xlint:-options -d "$out/classes" $(find stubs src -name '*.java')
# Only the extension package goes into the dex; the stubs resolve to the app's own classes.
${D8:-d8} --release --min-api 26 --output "$out" $(find "$out/classes/app" -name '*.class')
base64 -w0 "$out/classes.dex"
