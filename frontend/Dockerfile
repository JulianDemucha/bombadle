FROM node:22-alpine AS build
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

FROM nginx:1.27-alpine
RUN rm /etc/nginx/conf.d/default.conf
COPY nginx/app.conf.template /etc/nginx/templates/app.conf.template
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80 443
