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


mvn clean test -f $1