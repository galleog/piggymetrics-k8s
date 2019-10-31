FROM nginx:alpine

MAINTAINER Oleg Galkin <ogalleg@gmail.com>

COPY ./nginx.conf /etc/nginx/conf.d/default.conf.template
COPY ./static/ /var/www

ENV SERVER_PORT 80
ENV SERVER_NAME _

CMD ["sh", "-c", "envsubst '$SERVER_PORT $SERVER_NAME' < /etc/nginx/conf.d/default.conf.template > /etc/nginx/conf.d/default.conf && nginx -g 'daemon off;'"]