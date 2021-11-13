FROM adoptopenjdk/maven-openjdk11@sha256:fd7ca81dc66b2d42907d2259ac179058bb26d8ce909116e1a8a3b539292cc988

COPY target/backend-spring.jar /usr/share/fsearch/backend/backend.jar
COPY docs/jdk-8-partial.zip /usr/share/fsearch/docs/jdk-8-partial.zip

CMD java \
    -jar /usr/share/fsearch/backend/backend.jar \
    --data-store.path=/usr/share/fsearch/store-cache/ \
    --jcl.javadoc.path=/usr/share/fsearch/docs/jdk-8-partial.zip
