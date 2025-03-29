FROM maven:3.8.6-openjdk-17-slim AS build
WORKDIR /app
COPY pom.xml .
# 下载依赖
RUN mvn dependency:go-offline
COPY src ./src
# 构建应用
RUN mvn package -DskipTests

FROM openjdk:17-slim
WORKDIR /app
# 创建上传目录
RUN mkdir -p /app/uploads/blogs /app/uploads/avatar
# 复制构建的jar文件
COPY --from=build /app/target/*.jar app.jar
# 设置上传目录环境变量
ENV FILE_UPLOAD_DIR=/app/uploads
EXPOSE 8080
# 启动应用
ENTRYPOINT ["java", "-jar", "app.jar"]