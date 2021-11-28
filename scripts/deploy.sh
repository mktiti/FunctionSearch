#!/usr/bin/env bash

scriptDir=$(dirname "$0")
projectRootDir=$(dirname "$scriptDir")

serverUrl=${1:-"fsearch-eu"}
configDir=${2:-"$projectRootDir/config/prod/"}
imageDir="$projectRootDir/target/docker-images"

remoteDirBase="/home/titi/fsearch-docker/"
remoteConfigDir="$remoteDirBase/config/"
remoteImageDir="$remoteDirBase/images/"

checkFile() {
  local file="$1"
  local type="$2"

  if [[ -f "$file" ]]; then
    echo "Using $type file [$file]"
  else
    echo "[$file] ($type file) does not exist!"
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

envVarFile="$configDir/.env"
checkFile "$envVarFile" "environment-specific config"

composeFile="$projectRootDir/docker-compose.yml"
checkFile "$composeFile" "docker-compose config"

copyFileToServer() {
  local file=${1}
  local remoteDir=${2}

  echo "Copying $file to server"
  echo rsync "$file" "$serverUrl:$remoteDir"
  rsync --info=progress2 --info=name0 "$file" "$serverUrl:$remoteDir" || {
    echo "Failed to copy file"
    exit 1
  }
}

copyFileToServer "$backendImage" "$remoteImageDir"
copyFileToServer "$frontendImage" "$remoteImageDir"
copyFileToServer "$facadeImage" "$remoteImageDir"

copyFileToServer "$envVarFile" "$remoteConfigDir"
copyFileToServer "$composeFile" "$remoteConfigDir"
copyFileToServer "$scriptFile" "$remoteConfigDir"

ssh "$serverUrl" "$remoteConfigDir/$scriptName" $remoteDirBase