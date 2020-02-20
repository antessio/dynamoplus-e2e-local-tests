FROM maven:3.6.3-jdk-8

RUN apt-get update -y && apt-get upgrade -y


RUN mkdir -p /build/dynamoplus-e2e-tests
COPY . /build/dynamoplus-e2e-tests
RUN cd /build
RUN git clone https://github.com/antessio/dynamoplus-java-sdk.git
RUN git clone https://github.com/vishnubob/wait-for-it.git
RUN chmod +x wait-for-it/wait-for-it.sh


RUN export PATH=$PATH:~/wait-for-it/
RUN mvn clean install -f dynamoplus-java-sdk/pom.xml
RUN mvn clean compile -f /build/dynamoplus-e2e-tests/pom.xml
RUN chmod +x -R /build/dynamoplus-e2e-tests/scripts/
WORKDIR /build/dynamoplus-e2e-tests/scripts

CMD ["./entrypoint.sh","/build/dynamoplus-e2e-tests/pom.xml"]