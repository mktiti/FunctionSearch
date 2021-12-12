#!/usr/bin/env bash

apiPathPlaceholder="<<<API_PATH_PLACEHOLDER_STRING>>>"
apiPathValue=${API_PATH}
path="/usr/share/fsearch/vue-frontend"

if [ -z "$apiPathValue" ]; then
  echo "Missing api path!"
  exit 1
else
  echo "Using API path $apiPathValue"
fi

# Replace API placeholder on start
find "$path" -type f -name "*.js" -exec gawk -i inplace -v PATH="$apiPathValue" "{gsub(/$apiPathPlaceholder/, PATH); print}" {} \;
echo "Replaced API path placeholder"

# Create permanent GZIP-d versions of files
find "$path" -type f -exec gzip -9 -k -f {} \;
echo "Created GZIP-d caches"

# Start Nginx
echo -n "Starting Nginx"
printf " %s" "$@"
echo
/docker-entrypoint.sh "nginx" "-g" "daemon off;"
