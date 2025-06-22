# 多阶段构建 Dockerfile
# 阶段1: 构建前端
FROM node:18-alpine AS frontend-builder

# 设置工作目录
WORKDIR /app/frontend

# 复制前端依赖文件
COPY frontend/package*.json ./

# 安装依赖
RUN npm ci --only=production

# 复制前端源码
COPY frontend/ ./

# 构建前端静态文件
RUN npm run generate

# 阶段2: 构建后端
FROM maven:3.9-openjdk-17-slim AS backend-builder

# 设置工作目录
WORKDIR /app/backend

# 复制Maven配置文件
COPY backend/pom.xml ./
COPY backend/mvnw ./
COPY backend/mvnw.cmd ./
COPY backend/.mvn ./.mvn

# 下载依赖（利用Docker缓存）
RUN mvn dependency:go-offline -B

# 复制后端源码
COPY backend/src ./src

# 从前端构建阶段复制静态文件到后端资源目录
COPY --from=frontend-builder /app/frontend/.output/public ./src/main/resources/static

# 构建后端应用
RUN mvn clean package -DskipTests -B

# 阶段3: 运行时镜像
FROM openjdk:17-jre-slim

# 设置时区
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 创建应用用户
RUN groupadd -r appuser && useradd -r -g appuser appuser

# 设置工作目录
WORKDIR /app

# 创建必要的目录
RUN mkdir -p /app/uploads /app/logs && \
    chown -R appuser:appuser /app

# 从构建阶段复制JAR文件
COPY --from=backend-builder /app/backend/target/*.jar app.jar

# 修改文件权限
RUN chown appuser:appuser app.jar

# 切换到非root用户
USER appuser

# 暴露端口
EXPOSE 8080

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/api/auth/status || exit 1

# 启动应用
ENTRYPOINT ["java", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dspring.profiles.active=docker", \
    "-Xmx512m", \
    "-Xms256m", \
    "-jar", \
    "app.jar"]
