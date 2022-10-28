FROM maven:3.6-openjdk-14 as builder

ARG MAVEN_OPTS

WORKDIR /app

COPY . .

RUN mvn package -B -DskipTests=true $MAVEN_OPTS

FROM openjdk:14-alpine

WORKDIR /app

# copy built application
COPY --from=builder /app/target/webx-app/webx-demo.jar /app

CMD java -jar /app/webx-demo.jar

EXPOSE 8080
