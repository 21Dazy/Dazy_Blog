# 构建阶段
FROM node:18-alpine as build-stage
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
# 修改API地址为后端容器名
RUN sed -i 's|http://localhost:8080/api|/api|g' src/main.js
RUN npm run build

# 生产阶段
FROM nginx:stable-alpine as production-stage
# 复制构建结果到Nginx目录
COPY --from=build-stage /app/dist /usr/share/nginx/html
# 复制Nginx配置
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"] 