#!/usr/bin/env bash

RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

scriptDir=$(dirname "$0")
projectRootDir=$(dirname "$scriptDir")

jclDocZip="$projectRootDir/data/docs/jdk-8-partial.zip"

printColor() {
  local color=${1}
  local message=${2}
  echo -e "$color${message}${NC}"
}

printError() {
  printColor "$RED" "$1"
}

printSuccess() {
  printColor "$GREEN" "$1"
}

printValidEnvs() {
  mapfile -t validEnvs < <(find "$projectRootDir/environments" -mindepth 1 -maxdepth 1 -type d | awk -F/ '{print $NF}' | grep -v '^base$')
  echo -n "Valid environments:"
  printf -v envString ' %s' "${validEnvs[@]}"
  echo -e "$BLUE$envString$NC"
}

if [[ -z "$1" ]]; then
  printError "Environment (first arg) missing!"
  printValidEnvs
  exit 1
fi

environment=${1}
envDir="$projectRootDir/environments/$environment"
if [[ ! -d "$envDir" ]]; then
  printError "Invalid environment passed ($environment)!"
  printValidEnvs
  exit 1
fi

imageDir="$projectRootDir/target/docker-images"

envPropsFile="$envDir/env.properties"
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

serverAddress="$HOST"
remoteConfigDir="$TARGET_DIR/config/"
remoteImageDir="$TARGET_DIR/images/"
remoteDocsDir="$TARGET_DIR/docs/"

checkFile() {
  local file="$1"
  local type="$2"

  if [[ -f "$file" ]]; then
    echo "Using $type file $file"
  else
    printError "$file ($type file) does not exist!"
    exit 1
  fi
}

backendImage="$imageDir/backend.tar.gz"
frontendImage="$imageDir/frontend.tar.gz"
facadeImage="$imageDir/facade.tar.gz"
checkFile "$backendImage" "docker image"
checkFile "$frontendImage" "docker image"
checkFile "$facadeImage" "docker image"

scriptName="load-images.sh"
scriptFile="$scriptDir/$scriptName"
checkFile "$scriptFile" "loader script"

envVarFile="$envDir/config/.env"
checkFile "$envVarFile" "environment-specific config"

composeFile="$projectRootDir/docker-compose.yml"
checkFile "$composeFile" "docker-compose config"

copyFileToServer() {
  local file=${1}
  local remoteDir=${2}

  if [[ "$DEPLOY" == "SSH" ]]; then
    local target="$serverAddress:$remoteDir"
    echo "Copying $file to $target"
    rsync --info=progress2 --info=name0 "$file" "$target" || {
      printError "Failed to copy file to remote directory"
      exit 1
    }
  elif [[ "$DEPLOY" == "LOCAL" ]]; then
    mkdir -p "$remoteDir"
    cp "$file" "$remoteDir/" || {
      printError "Failed to copy file to local deploy directory"
      exit 1
    }
  else
    printError "Unknown deploy mode ($DEPLOY)"
    exit 1
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
  ssh "$serverAddress" "$remoteConfigDir/$scriptName" "$TARGET_DIR" "$USE_FACADE" || {
    printError "Error while running remote deploy script"
    exit 1
  }
  printSuccess "Remote deploy script executed"
elif [[ "$DEPLOY" == "LOCAL" ]]; then
  "$remoteConfigDir/$scriptName" "$TARGET_DIR" "$USE_FACADE" || {
    printError "Error while running local deploy script"
    exit 1
  }
  printSuccess "Local deploy script executed"
else
  printError "Unknown deploy mode defined in $envPropsFile"
  printError "Known deploy modes: [SSH, LOCAL]"
  exit 1
fi
