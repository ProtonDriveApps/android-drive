#!/bin/bash
# Minimal helpers for invoking the Proton Drive CLI from CI scripts.
# Subset of the shared utils.sh used by drive_sdk_cli load tests; vendored
# here so this repo can use the publish flow without an external dependency.

PROTON_DRIVE_INTERNAL_CLI="${PROTON_DRIVE_INTERNAL_CLI:-/proton-drive-internal}"
PROTON_DRIVE_CLI="${PROTON_DRIVE_CLI:-/proton-drive}"
VERIFICATION_LOGS_DIR="verification_logs"

DEFAULT_MAX_RETRIES=3
DEFAULT_RETRY_DELAY=5

init_and_authenticate() {
    validate_credential_vars
    get_credentials
    check_proton_drive_cli
    create_verification_logs_dir
    authenticate_proton_drive
}

validate_credential_vars() {
    if [ -z "${USERNAME_VAR:-}" ] || [ -z "${PASSWORD_VAR:-}" ]; then
        echo "Error: USERNAME_VAR and PASSWORD_VAR must be set as environment variables"
        exit 1
    fi
}

get_credentials() {
    if [ -n "${!USERNAME_VAR:-}" ]; then
        USERNAME="${!USERNAME_VAR}"
    else
        echo "Error: ${USERNAME_VAR} not found in environment variables"
        exit 1
    fi
    if [ -n "${!PASSWORD_VAR:-}" ]; then
        PASSWORD="${!PASSWORD_VAR}"
    else
        echo "Error: ${PASSWORD_VAR} not found in environment variables"
        exit 1
    fi
    echo "Using credentials from: ${USERNAME_VAR} / ${PASSWORD_VAR}"
}

check_proton_drive_cli() {
    if [ ! -f "$PROTON_DRIVE_INTERNAL_CLI" ]; then
        echo "Error: Proton Drive Internal CLI not found at $PROTON_DRIVE_INTERNAL_CLI"
        exit 1
    fi
    echo "Proton Drive Internal CLI: $PROTON_DRIVE_INTERNAL_CLI"
}

create_verification_logs_dir() {
    mkdir -p "$VERIFICATION_LOGS_DIR"
}

authenticate_proton_drive() {
    echo ""
    echo "Authenticating with Proton Drive..."
    local login_output
    login_output=$(mktemp)
    if ! execute_command_with_output "$login_output" "$PROTON_DRIVE_INTERNAL_CLI" auth login "$USERNAME" --password "$PASSWORD"; then
        echo "Error: Authentication failed after retries"
        cat "$login_output"
        rm -f "$login_output"
        exit 1
    fi
    cat "$login_output"
    rm -f "$login_output"
#    if [ ! -f "auth.txt" ]; then
#        echo "Error: Authentication verification failed - auth.txt not found"
#        exit 1
#    fi
    echo "Authentication successful"
}

strip_ansi_codes() {
    local input_file="$1"
    local output_file="$2"
    sed 's/\x1b\[[0-9;]*m//g' "$input_file" > "$output_file"
}

execute_command_with_output() {
    local output_file="$1"
    shift
    local cmd=("$@")
    local max_retries="${MAX_RETRIES:-$DEFAULT_MAX_RETRIES}"
    local delay="${RETRY_DELAY:-$DEFAULT_RETRY_DELAY}"
    local attempt=1
    local exit_code=0
    local temp_output

    while [ $attempt -le $max_retries ]; do
        echo "Attempt $attempt of $max_retries: ${cmd[*]}"
        temp_output=$(mktemp)
        "${cmd[@]}" > "$temp_output" 2>&1
        exit_code=$?
        if grep -q "TimeoutError\|Request timed out\|ETIMEDOUT\|ECONNRESET" "$temp_output" 2>/dev/null; then
            echo "Timeout detected in output"
            cat "$temp_output"
            rm -f "$temp_output"
            if [ $attempt -lt $max_retries ]; then
                echo "Retrying in ${delay}s..."
                sleep $delay
                delay=$((delay * 2))
            fi
            attempt=$((attempt + 1))
            continue
        fi
        if [ $exit_code -eq 0 ]; then
            echo "Command succeeded on attempt $attempt"
            mv "$temp_output" "$output_file"
            return 0
        fi
        echo "Command failed with exit code $exit_code"
        cat "$temp_output"
        rm -f "$temp_output"
        if [ $attempt -lt $max_retries ]; then
            echo "Retrying in ${delay}s..."
            sleep $delay
            delay=$((delay * 2))
        fi
        attempt=$((attempt + 1))
    done
    echo "Command failed after $max_retries attempts"
    return $exit_code
}
