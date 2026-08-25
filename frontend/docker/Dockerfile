# Build all apps, then serve with nginx in a single image.
FROM node:20-alpine AS builder
WORKDIR /app

RUN apk add --no-cache bash

COPY package.json package-lock.json* turbo.json ./
COPY packages ./packages
COPY apps ./apps

RUN npm ci
RUN npm run build

# Production image
FROM nginx:alpine
RUN rm /etc/nginx/conf.d/default.conf
COPY docker/nginx.conf /etc/nginx/conf.d/default.conf

COPY --from=builder /app/apps/landing/dist /usr/share/nginx/html/landing
COPY --from=builder /app/apps/customer/dist /usr/share/nginx/html/customer
COPY --from=builder /app/apps/merchant/dist /usr/share/nginx/html/merchant
COPY --from=builder /app/apps/admin/dist /usr/share/nginx/html/admin
COPY --from=builder /app/apps/agent/dist /usr/share/nginx/html/agent

EXPOSE 80
HEALTHCHECK --interval=30s --timeout=3s CMD wget -qO- http://localhost/ >/dev/null 2>&1 || exit 1
CMD ["nginx", "-g", "daemon off;"]
