FROM node:22-alpine AS build

WORKDIR /app

COPY package.json ./
RUN npm install

COPY . .
RUN npm run build
RUN npm prune --omit=dev

FROM node:22-alpine

WORKDIR /app

ENV NODE_ENV=production
ENV PORT=80
ENV API_BASE_URL=http://api-gateway:8080

COPY --from=build /app/package.json ./package.json
COPY --from=build /app/node_modules ./node_modules
COPY --from=build /app/dist ./dist
COPY docker-server.mjs ./docker-server.mjs

EXPOSE 80

CMD ["node", "docker-server.mjs"]
