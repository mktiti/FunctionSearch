FROM adoptopenjdk/maven-openjdk11

COPY target/backend-spring.jar /usr/share/fsearch/backend/backend.jar
COPY docs/jdk-8-partial.zip /usr/share/fsearch/docs/jdk-8-partial.zip

ENTRYPOINT ["java", "-jar", "/usr/share/fsearch/backend/backend.jar", "/usr/share/fsearch/docs/jdk-8-partial.zip"]
