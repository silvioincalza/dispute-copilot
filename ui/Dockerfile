FROM node:20-alpine AS build

WORKDIR /app

# Accept build-time API URL (defaults to empty, meaning nginx proxy handles /api/)
ARG REACT_APP_API_URL=""

COPY package.json .
RUN npm install --legacy-peer-deps

COPY public/ ./public/
COPY src/ ./src/

ENV REACT_APP_API_URL=$REACT_APP_API_URL

RUN npm run build

FROM nginx:alpine
COPY --from=build /app/build /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
