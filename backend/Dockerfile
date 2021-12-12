FROM adoptopenjdk/maven-openjdk11@sha256:fd7ca81dc66b2d42907d2259ac179058bb26d8ce909116e1a8a3b539292cc988

COPY target/backend-spring.jar /usr/share/fsearch/backend/backend.jar

ENV BACKEND_MIN_HEAP 1024m
ENV BACKEND_MAX_HEAP 4096m

CMD java \
    -Xms${BACKEND_MIN_HEAP} \
    -Xmx${BACKEND_MAX_HEAP} \
    -jar /usr/share/fsearch/backend/backend.jar \
    --spring.profiles.active=${SPRING_PROFILE}
