FROM node:20-alpine3.19 AS builder
WORKDIR /app

COPY package*.json ./
RUN npm config set registry https://registry.npmmirror.com
RUN npm install

COPY . ./
RUN node node_modules/vite/bin/vite.js build

FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80
