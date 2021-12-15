#!/usr/bin/env bash

readonly RED='\033[0;31m'
readonly GREEN='\033[0;32m'
readonly BLUE='\033[0;34m'
readonly NC='\033[0m'

readonly scriptDir=$(dirname "$0")
readonly projectRootDir=$(dirname "$scriptDir")

readonly jclDocZip="$projectRootDir/data/docs/jdk-8-partial.zip"

printColor() {
  local -r color=${1}
  local -r message=${2}
  echo -e "$color${message}${NC}"
}

printError() {
  printColor "$RED" "$1"
}

failError() {
  printError "$1"
  exit 1
}

printSuccess() {
  printColor "$GREEN" "$1"
}

printHighlight() {
  printColor "$BLUE" "$1"
}

printValidEnvs() {
  mapfile -t validEnvs < <(find "$projectRootDir/environments" -mindepth 1 -maxdepth 1 -type d | awk -F/ '{print $NF}' | grep -v '^base$')
  echo -n "Valid environments:"
  printf -v envString ' %s' "${validEnvs[@]}"
  printHighlight "$envString"
}

if [[ -z "$1" ]]; then
  printError "Environment (first arg) missing!"
  printValidEnvs
  exit 1
fi

readonly environment=${1}
readonly envDir="$projectRootDir/environments/$environment"
if [[ ! -d "$envDir" ]]; then
  printError "Invalid environment passed ($environment)!"
  printValidEnvs
  exit 1
fi

readonly imageDir="$projectRootDir/target/docker-images"

readonly envPropsFile="$envDir/env.properties"
# shellcheck disable=SC2046
export $(grep -v '^#' "$envPropsFile" | xargs) >/dev/null

if [[ -z "$TARGET_DIR" ]]; then
  printError "Environment specific properties ($envPropsFile) does not set 'TARGET_DIR'"
  exit 1
elif [[ -z "$HOST" ]]; then
  printError "Environment specific properties ($envPropsFile) does not set 'HOST'"
  exit 1
elif [[ -z "$DEPLOY" ]]; then
  printError "Environment specific properties ($envPropsFile) does not set 'DEPLOY'"
  exit 1
elif [[ -z "$USE_FACADE" ]]; then
  printError "Environment specific properties ($envPropsFile) does not set 'USE_FACADE'"
  exit 1
fi

readonly serverAddress="$HOST"
readonly remoteConfigDir="$TARGET_DIR/config/"
readonly remoteImageDir="$TARGET_DIR/images/"
readonly remoteDocsDir="$TARGET_DIR/docs/"

checkFile() {
  local -r file="$1"
  local -r type="$2"

  if [[ -f "$file" ]]; then
    echo "Using $type file $file"
  else
    failError "$file ($type file) does not exist!"
  fi
}

readonly backendImage="$imageDir/backend.tar.gz"
readonly frontendImage="$imageDir/frontend.tar.gz"
readonly facadeImage="$imageDir/facade.tar.gz"
checkFile "$backendImage" "docker image"
checkFile "$frontendImage" "docker image"
checkFile "$facadeImage" "docker image"

readonly scriptName="load-images.sh"
readonly scriptFile="$scriptDir/$scriptName"
checkFile "$scriptFile" "loader script"

readonly envVarFile="$envDir/config/.env"
checkFile "$envVarFile" "environment-specific config"

readonly composeFile="$projectRootDir/docker-compose.yml"
checkFile "$composeFile" "docker-compose config"

copyFileToServer() {
  local -r file=${1}
  local -r remoteDir=${2}

  if [[ "$DEPLOY" == "SSH" ]]; then
    local -r target="$serverAddress:$remoteDir"
    echo "Copying $file to $target"
    rsync --info=progress2 --info=name0 "$file" "$target" || failError "Failed to copy file to remote directory"
  elif [[ "$DEPLOY" == "LOCAL" ]]; then
    mkdir -p "$remoteDir"
    cp "$file" "$remoteDir/" || failError "Failed to copy file to local deploy directory"
  else
    failError "Unknown deploy mode ($DEPLOY)"
  fi
}

copyFileToServer "$backendImage" "$remoteImageDir"
copyFileToServer "$frontendImage" "$remoteImageDir"
copyFileToServer "$facadeImage" "$remoteImageDir"

copyFileToServer "$envVarFile" "$remoteConfigDir"
copyFileToServer "$composeFile" "$remoteConfigDir"
copyFileToServer "$scriptFile" "$remoteConfigDir"

copyFileToServer "$jclDocZip" "$remoteDocsDir"

printSuccess "All files copied to server [$environment], starting deploy script"

if [[ "$DEPLOY" == "SSH" ]]; then
  # shellcheck disable=SC2029
  ssh "$serverAddress" "$remoteConfigDir/$scriptName" "$TARGET_DIR" "$USE_FACADE" || failError "Error while running remote deploy script"
  printSuccess "Remote deploy script executed"
elif [[ "$DEPLOY" == "LOCAL" ]]; then
  "$remoteConfigDir/$scriptName" "$TARGET_DIR" "$USE_FACADE" || failError "Error while running local deploy script"
  printSuccess "Local deploy script executed"
else
  printError "Unknown deploy mode defined in $envPropsFile"
  printError "Known deploy modes: [SSH, LOCAL]"
  exit 1
fi
