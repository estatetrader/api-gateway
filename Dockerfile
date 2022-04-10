FROM openjdk:8u212-jdk-stretch
MAINTAINER nick

ENV JAVA_OPTS ""
ENV SPRING_ARGS ""

RUN cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && echo "Asia/Shanghai" > /etc/timezone

RUN mkdir -p /service/api-gateway
WORKDIR /service/api-gateway
RUN mkdir api-jars
COPY api-gateway/target/api-gateway.jar api-gateway.jar

CMD exec java -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/dump -XX:ErrorFile=/dump/crash.log ${JAVA_OPTS} -jar api-gateway.jar ${SPRING_ARGS}