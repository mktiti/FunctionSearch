FROM nginx:1.17.1-alpine

COPY fsearch-frontend/dist /usr/share/fsearch/vue-frontend
RUN find /usr/share/fsearch/vue-frontend -type f -exec gzip -9 -k -f {} \;
COPY config/docker-nginx.conf /etc/nginx/nginx.conf
