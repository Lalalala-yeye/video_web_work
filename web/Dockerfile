#构建阶段：安装依赖，打包前端资源
FROM node:22-alpine AS builder

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .

RUN npm run build

#运行阶段：nginx静态服务
FROM nginx:alpine

WORKDIR /usr/share/nginx/html

#从构建产物复制dist打包后的静态文件
COPY --from=builder /app/dist ./

EXPOSE 80

HEALTHCHECK --interval=10s --timeout=3s --retries=3 \
  CMD wget -q --spider http://127.0.0.1/ || exit 1

#启动nginx
CMD ["nginx", "-g", "daemon off;"]
