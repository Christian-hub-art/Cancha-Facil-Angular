# Etapa 1: Compilar Angular
FROM node:22-alpine AS compilacion
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# Etapa 2: Servir Angular con Nginx
FROM nginx:alpine
COPY --from=compilacion /app/dist/frontend-angular/browser/ /usr/share/nginx/html/
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
