FROM maven:3.6.3-jdk-8

RUN apt-get update -y && apt-get upgrade -y


RUN mkdir -p /build/dynamoplus-e2e-tests
COPY . /build/dynamoplus-e2e-tests
RUN chmod +x /build/dynamoplus-e2e-tests/scripts/*
RUN /build/dynamoplus-e2e-tests/scripts/init.sh
WORKDIR /build/dynamoplus-e2e-tests/scripts

CMD ["./entrypoint.sh","/build/dynamoplus-e2e-tests/pom.xml"]