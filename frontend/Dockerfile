FROM nginx:1.27-alpine
RUN rm /etc/nginx/conf.d/default.conf
COPY nginx/app.conf.template /etc/nginx/templates/app.conf.template

EXPOSE 80 443
