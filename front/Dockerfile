# --- Build : installation reproductible (npm ci), contrôle de types, bundle Vite ---
FROM node:24-alpine AS build
WORKDIR /build
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY . .
RUN npm run build

# --- Exécution : nginx non privilégié (port 8080), configuration rendue au démarrage ---
FROM nginxinc/nginx-unprivileged:alpine
COPY nginx/default.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /build/dist /usr/share/nginx/html
# URL du back, résolue par nginx au démarrage (service compose ou Service Kubernetes)
ENV BACK_URL=http://back:8080
EXPOSE 8080
