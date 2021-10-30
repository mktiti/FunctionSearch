FROM adoptopenjdk/maven-openjdk11

COPY target/backend-spring.jar /usr/share/fsearch/backend/backend.jar
COPY docs/jdk-8-partial.zip /usr/share/fsearch/docs/jdk-8-partial.zip

ENV BACKEND_MIN_HEAP 1024m

ENTRYPOINT java \
    -Xms${BACKEND_MIN_HEAP} \
    -jar /usr/share/fsearch/backend/backend.jar \
    --data-store.path=/usr/share/fsearch/store-cache/ \
    --jcl.javadoc.path=/usr/share/fsearch/docs/jdk-8-partial.zip
