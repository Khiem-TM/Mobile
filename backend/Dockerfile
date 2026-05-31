# ─────────────────────────── Builder ───────────────────────────
# Cần build-tools cho native deps (bcrypt) — base glibc (bookworm) để binary
# tương thích với stage runner.
FROM node:20-bookworm-slim AS builder
WORKDIR /app

RUN apt-get update \
  && apt-get install -y --no-install-recommends python3 make g++ \
  && rm -rf /var/lib/apt/lists/*

COPY package*.json ./
RUN npm ci

COPY tsconfig*.json nest-cli.json ./
COPY src ./src
RUN npm run build

# Loại dev-deps khỏi node_modules để copy sang runner (giữ native binary đã build).
RUN npm prune --omit=dev

# ─────────────────────────── Runner ────────────────────────────
FROM node:20-bookworm-slim AS runner
ENV NODE_ENV=production
WORKDIR /app

# tini: xử lý tín hiệu/Zombie cho process Node.
RUN apt-get update \
  && apt-get install -y --no-install-recommends tini \
  && rm -rf /var/lib/apt/lists/*

COPY package*.json ./
COPY --from=builder /app/node_modules ./node_modules
COPY --from=builder /app/dist ./dist
COPY public ./public

# Chạy non-root.
USER node

EXPOSE 3005
ENTRYPOINT ["/usr/bin/tini", "--"]
CMD ["node", "dist/main.js"]
