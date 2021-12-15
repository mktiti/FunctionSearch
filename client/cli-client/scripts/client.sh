#!/bin/bash

readonly RED='\033[0;31m'
readonly NC='\033[0m'

printColor() {
  local color=${1}
  local message=${2}
  echo -e "$color${message}${NC}"
}

printError() {
  printColor "$RED" "$1"
}

if [[ -z "$1" ]]; then
  printError "Missing API base path (first param)!"
  exit 1
fi

readonly scriptDir=$(dirname "$0")
readonly clientDir=$(dirname "$scriptDir")
readonly jarFile="$clientDir/target/fsearch-client.jar"

java -jar "$jarFile" "$1"
