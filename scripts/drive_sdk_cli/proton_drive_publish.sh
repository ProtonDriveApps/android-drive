#!/bin/bash
# Publishes signed APKs and mapping files (from rename <flavor> release jobs)
# to a known folder on Proton Drive, grouped by app version.
#
# Required env vars:
#   USERNAME_VAR / PASSWORD_VAR  consumed by utils.sh init_and_authenticate
#   PUBLISH_PARENT_PATH          known parent folder on Proton Drive

set -euo pipefail

source "$(dirname "$0")/utils.sh"

init_and_authenticate

if [ -z "${PUBLISH_PARENT_PATH:-}" ]; then
    echo "Error: PUBLISH_PARENT_PATH env var is not set"
    exit 1
fi

# Extract version from any APK filename:
#   ProtonDrive-2.37.0-alpha (4282).apk  -> 2.37.0
#   ProtonDrive-2.37.0 (4282).apk        -> 2.37.0
APP_VERSION=$(ls renamedArtifacts/*.apk 2>/dev/null | head -n1 \
    | sed -E 's|.*ProtonDrive-([0-9]+\.[0-9]+\.[0-9]+).*|\1|')

if [[ -z "$APP_VERSION" || "$APP_VERSION" == *"ProtonDrive"* ]]; then
    echo "Error: could not determine app version from renamedArtifacts/"
    ls -1 renamedArtifacts/ || true
    exit 1
fi
echo "App version: $APP_VERSION"

DEST_PATH="$PUBLISH_PARENT_PATH/$APP_VERSION"

# Check if destination folder already exists; create it only if missing.
# `fs info` exits 1 in both "found" and "missing" cases, so we parse the output
# (after stripping ANSI codes which are emitted even when stdout is redirected).
INFO_OUTPUT=$(mktemp)
INFO_EXIT=0
"$PROTON_DRIVE_CLI" fs info "$DEST_PATH" > "$INFO_OUTPUT" 2>&1 || INFO_EXIT=$?

INFO_CLEAN=$(mktemp)
strip_ansi_codes "$INFO_OUTPUT" "$INFO_CLEAN"

if grep -q "ok: true" "$INFO_CLEAN"; then
    echo "Folder $DEST_PATH already exists - skipping create-folder"
elif grep -q "Node not found" "$INFO_CLEAN"; then
    echo "Folder $DEST_PATH does not exist - creating"
    cp "$INFO_CLEAN" "$VERIFICATION_LOGS_DIR/fs-info-before-create.txt"
    if ! execute_command_with_output "$VERIFICATION_LOGS_DIR/create-folder.txt" \
            "$PROTON_DRIVE_CLI" fs create-folder "$PUBLISH_PARENT_PATH" "$APP_VERSION"; then
        echo "Error: create-folder failed after retries"
        [ -f "$VERIFICATION_LOGS_DIR/create-folder.txt" ] && cat "$VERIFICATION_LOGS_DIR/create-folder.txt"
        exit 1
    fi
else
    echo "Error: 'fs info $DEST_PATH' produced unrecognized output (exit $INFO_EXIT)"
    cat "$INFO_CLEAN"
    rm -f "$INFO_OUTPUT" "$INFO_CLEAN"
    exit 1
fi
rm -f "$INFO_OUTPUT" "$INFO_CLEAN"

# Collect files explicitly. The CLI expects: fs upload <file1> <file2> ... <fileN> <destination>
SOURCES=()
for file in renamedArtifacts/*; do
    [ -f "$file" ] || continue
    SOURCES+=("$file")
done

if [ "${#SOURCES[@]}" -eq 0 ]; then
    echo "Error: no files found in renamedArtifacts/"
    exit 1
fi

echo "Uploading ${#SOURCES[@]} file(s) to $DEST_PATH:"
printf '  %s\n' "${SOURCES[@]}"

TOTAL="${#SOURCES[@]}"
INDEX=0
for file in "${SOURCES[@]}"; do
    INDEX=$((INDEX + 1))
    name=$(basename "$file")
    log_name="upload-${name}.txt"
    echo ""
    echo "[$INDEX/$TOTAL] Uploading $name"
    if ! execute_command_with_output "$VERIFICATION_LOGS_DIR/$log_name" \
            "$PROTON_DRIVE_CLI" fs upload "$file" "$DEST_PATH" -f skip; then
        echo "Error: upload of $name failed after retries"
        [ -f "$VERIFICATION_LOGS_DIR/$log_name" ] && cat "$VERIFICATION_LOGS_DIR/$log_name"
        exit 1
    fi
done

echo "Publish complete."
