FROM hub.c.163.com/library/java:8-alpine
WORKDIR .
ADD ./*.jar app.jar
EXPOSE 9208
ENTRYPOINT ["java","-jar","/app.jar"]