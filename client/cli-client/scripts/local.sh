#!/bin/bash

readonly scriptDir=$(dirname "$0")
"$scriptDir/client.sh" http://localhost:8080/api/v1/
