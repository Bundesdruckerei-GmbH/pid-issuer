FROM nginx:1.27.4-alpine3.21@sha256:b471bb609adc83f73c2d95148cf1bd683408739a3c09c0afc666ea2af0037aef
COPY docker/nginx.conf /etc/nginx/nginx.conf
COPY dist/revocation-service-frontend/browser /usr/share/nginx/html
