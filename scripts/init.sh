#!/usr/bin/env bash

cd /build
git clone https://github.com/antessio/dynamoplus-java-sdk.git
#git clone https://github.com/vishnubob/wait-for-it.git
#chmod +x wait-for-it/wait-for-it.sh
ls -lart
#PATH=${PATH}:/build/wait-for-it/
mvn clean install -q -f dynamoplus-java-sdk/pom.xml
mvn clean compile -q -f /build/dynamoplus-e2e-tests/pom.xml
chmod +x -R /build/dynamoplus-e2e-tests/scripts/