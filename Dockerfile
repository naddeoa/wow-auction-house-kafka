FROM amd64/amazoncorretto:15-alpine

COPY ./app/build/libs/app-standalone.jar /opt/whylogs/data-poller.jar

WORKDIR /opt/whylogs

CMD ["java", "-jar", "/opt/whylogs/data-poller.jar"]

