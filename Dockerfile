FROM maven:3.6.3-jdk-11 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn clean dependency:go-offline
COPY . .
RUN mvn compile
CMD ["mvn", "exec:java"]