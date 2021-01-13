FROM nginx:1.17.1-alpine

RUN apk add -U unzip && rm -rf /var/cache/apk/*

COPY target/frontend.zip /usr/share/fsearch/frontend.zip
RUN unzip /usr/share/fsearch/frontend.zip -d /usr/share/fsearch/frontend

COPY docker/nginx.conf /etc/nginx/nginx.conf
