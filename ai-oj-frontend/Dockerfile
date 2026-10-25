# ---------- 构建阶段 ----------
FROM node:20-alpine AS build
WORKDIR /app

# 使用国内 npm 镜像源（随上下文复制 .npmrc）
COPY .npmrc package.json ./
# 若存在 lock 文件则一并复制，npm 会自动识别
COPY package-lock.json* ./
RUN npm install --no-audit --no-fund

COPY . .
RUN npm run build

# ---------- 运行阶段 ----------
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
