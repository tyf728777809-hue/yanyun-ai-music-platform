FROM node:22-bookworm-slim
WORKDIR /app
COPY apps/render-worker/package*.json ./
RUN npm ci
COPY apps/render-worker ./
RUN npm run build
EXPOSE 3001
CMD ["npm", "run", "preview", "--", "--host", "0.0.0.0", "--port", "3001"]
