#!/usr/bin/env bash

if [[ -z "${DYNAMOPLUS_HOST}" ]]; then
  export DYNAMOPLUS_HOST="http://localhost:3000"
fi

if [[ -z "${DYNAMOPLUS_ROOT}" ]]; then
  export DYNAMOPLUS_ROOT="root"
fi

if [[ -z "${DYNAMOPLUS_PASSWORD}" ]]; then
  export DYNAMOPLUS_PASSWORD="root"
fi

echo "Calling system info $DYNAMOPLUS_HOST/system/info"
until $(curl --output /dev/null --silent --head --fail "$DYNAMOPLUS_HOST/system/info"); do
    printf '.'
    sleep 5
done

mvn clean test -q -f $1