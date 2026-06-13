FROM node:24-bookworm-slim AS deps
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci

FROM node:24-bookworm-slim AS builder
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .
RUN npm run build

FROM node:24-bookworm-slim AS runner
WORKDIR /app
ENV NODE_ENV=production
ENV PORT=3000
RUN useradd --create-home --uid 10001 nextjs \
    && mkdir -p /app/logs/web \
    && chown -R nextjs:nextjs /app/logs
COPY --from=builder /app/.next/standalone ./
COPY --from=builder /app/.next/static ./.next/static
COPY --from=builder /app/public ./public
COPY healthcheck.js ./healthcheck.js
USER 10001:10001
EXPOSE 3000
HEALTHCHECK --interval=15s --timeout=5s --retries=10 CMD node healthcheck.js
CMD ["node", "server.js"]
