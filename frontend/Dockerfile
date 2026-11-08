# --- Étape 1 : compilation du site ---
FROM node:22-slim AS build
WORKDIR /build
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

# --- Étape 2 : serveur web léger, sans droits particuliers ---
FROM nginxinc/nginx-unprivileged:stable
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /build/dist /usr/share/nginx/html
EXPOSE 8080