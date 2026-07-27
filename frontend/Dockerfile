# Etap 1: build
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

# Etap 2: nginx
FROM nginx:1.27-alpine
# Domyślny backend — envsubst podstawia je do szablonu przy starcie kontenera.
# Można nadpisać w docker compose (environment: BACKEND_HOST / BACKEND_PORT).
ENV BACKEND_HOST=backend \
    BACKEND_PORT=8080
COPY nginx.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
