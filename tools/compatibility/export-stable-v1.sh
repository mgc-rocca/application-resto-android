#!/usr/bin/env bash
set -euo pipefail

resto_current_directory="$PWD"
resto_stable_directory="$(dirname "$resto_current_directory")/resto-stable-v1"
mkdir -p build/compatibility
git -C "$resto_stable_directory" rev-parse HEAD > build/compatibility/v1-source-commit.txt

# Keep diagnostics inside the artifact root: upload-artifact rejects ../ patterns.
collect_stable_reports() {
    for resto_report_folder in outputs/androidTest-results reports/androidTests; do
        resto_report_source="$resto_stable_directory/app/build/$resto_report_folder"
        if [ -d "$resto_report_source" ]; then
            mkdir -p "$resto_current_directory/build/compatibility/reports/$resto_report_folder"
            cp -R "$resto_report_source/." "$resto_current_directory/build/compatibility/reports/$resto_report_folder/"
        fi
    done
}
trap collect_stable_reports EXIT

cd "$resto_stable_directory"
if ! ./gradlew connectedDebugAndroidTest --stacktrace --no-daemon --max-workers=2 \
    -Pandroid.testInstrumentationRunnerArguments.class=fr.martinrocca.resto.compatibility.StableV1ExportTest \
    > "$resto_current_directory/build/compatibility/stable-v1-export.log" 2>&1; then
    tail -n 100 "$resto_current_directory/build/compatibility/stable-v1-export.log"
    exit 1
fi
tail -n 15 "$resto_current_directory/build/compatibility/stable-v1-export.log"
cd "$resto_current_directory"
adb exec-out cat /data/local/tmp/resto-stable-v1.zip > build/compatibility/resto-stable-v1.zip
adb exec-out cat /data/local/tmp/resto-stable-v1.db > build/compatibility/resto-stable-v1.db
python3 - <<'PY'
import json
import sqlite3
from pathlib import Path
from zipfile import ZipFile

folder = Path('build/compatibility')
with ZipFile(folder / 'resto-stable-v1.zip') as archive:
    assert archive.testzip() is None
    manifest = json.loads(archive.read('backup.json'))
    assert manifest['version'] == 1
    assert len(manifest['restaurants']) == 2
    assert len(manifest['photos']) == 1
database = folder / 'resto-stable-v1.db'
assert database.read_bytes().startswith(b'SQLite format 3\x00')
with sqlite3.connect(f'file:{database.resolve()}?mode=ro', uri=True) as connection:
    assert connection.execute('PRAGMA user_version').fetchone()[0] == 1
    assert connection.execute('PRAGMA quick_check').fetchone()[0] == 'ok'
    assert connection.execute('PRAGMA foreign_key_check').fetchall() == []
print('Verified V1 ZIP, SQLite version, integrity and foreign keys.')
PY
adb shell rm -f /data/local/tmp/resto-stable-v1.zip /data/local/tmp/resto-stable-v1.db
