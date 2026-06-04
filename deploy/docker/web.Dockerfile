FROM node:22-bookworm-slim AS build
WORKDIR /app
COPY apps/web/package*.json ./
RUN npm ci
COPY apps/web ./
RUN npm run build

FROM nginx:1.29-alpine
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
HEALTHCHECK --interval=30s --timeout=5s --retries=5 CMD wget -q --spider http://localhost/ || exit 1
