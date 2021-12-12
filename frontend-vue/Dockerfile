FROM nginx:1.20-alpine@sha256:f6609f898bcdad15047629edc4033d17f9f90e2339fb5ccb97da267f16902251

RUN apk add --no-cache bash
RUN apk add --no-cache gawk

COPY fsearch-frontend/dist /usr/share/fsearch/vue-frontend

COPY config/docker-nginx.conf /etc/nginx/nginx.conf

COPY config/start.sh /usr/share/fsearch/start.sh
RUN chmod +x /usr/share/fsearch/start.sh

ENTRYPOINT /usr/share/fsearch/start.sh