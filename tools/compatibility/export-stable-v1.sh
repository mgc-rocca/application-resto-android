#!/usr/bin/env bash
set -euo pipefail

resto_current_directory="$PWD"
resto_stable_directory="$(dirname "$resto_current_directory")/resto-stable-v1"
mkdir -p build/compatibility
git -C "$resto_stable_directory" rev-parse HEAD > build/compatibility/v1-source-commit.txt

cd "$resto_stable_directory"
if ! ./gradlew connectedDebugAndroidTest --stacktrace --no-daemon --max-workers=2 \
    -Pandroid.testInstrumentationRunnerArguments.class=fr.martinrocca.resto.compatibility.StableV1ExportTest \
    > "$resto_current_directory/build/compatibility/stable-v1-export.log" 2>&1; then
    tail -n 100 "$resto_current_directory/build/compatibility/stable-v1-export.log"
    exit 1
fi
cd "$resto_current_directory"
adb exec-out run-as fr.martinrocca.resto cat cache/resto-stable-v1.zip > build/compatibility/resto-stable-v1.zip
adb exec-out run-as fr.martinrocca.resto cat cache/resto-stable-v1.db > build/compatibility/resto-stable-v1.db
test -s build/compatibility/resto-stable-v1.zip
test -s build/compatibility/resto-stable-v1.db
