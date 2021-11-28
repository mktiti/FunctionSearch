FROM nginx:1.20-alpine@sha256:f6609f898bcdad15047629edc4033d17f9f90e2339fb5ccb97da267f16902251

COPY fsearch-frontend/dist /usr/share/fsearch/vue-frontend
RUN find /usr/share/fsearch/vue-frontend -type f -exec gzip -9 -k -f {} \;
COPY config/docker-nginx.conf /etc/nginx/nginx.conf
