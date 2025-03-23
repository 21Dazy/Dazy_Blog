# 博客系统项目

基于Vue 3和Spring Boot的现代博客系统，支持文章发布、用户评论和图片上传等功能。

## 项目结构

```
BLOG/
├── backend/ - Spring Boot后端
│   ├── src/ - 源代码
│   ├── uploads/ - 上传的图片存储
│   └── pom.xml - Maven依赖管理
├── frontend/ - Vue前端
│   ├── src/ - 源代码
│   ├── public/ - 静态资源
│   └── package.json - npm依赖管理
└── .gitignore - Git忽略配置
```

## 技术栈

### 前端
- Vue 3
- Vue Router
- Pinia
- Element Plus
- Axios

### 后端
- Spring Boot
- Spring Security
- Spring Data JPA
- MySQL
- JWT认证

## 功能特性

- 用户认证与授权系统
- 文章的创建、编辑、删除
- 图片上传功能
- 文章分类与标签
- 评论系统
- 用户个人资料管理

## 部署说明

### 后端
1. 安装JDK 17或更高版本
2. 配置数据库连接（application.properties）
3. 构建项目：`mvn clean package`
4. 运行：`java -jar target/blog-0.0.1-SNAPSHOT.jar`

### 前端
1. 安装依赖：`npm install`
2. 开发模式：`npm run dev`
3. 构建生产版本：`npm run build`

## 开发指南

### 代码风格
- 后端：遵循Google Java代码风格
- 前端：遵循Vue风格指南

### 分支策略
- `main`: 稳定版本
- `develop`: 开发中的版本
- `feature/*`: 新功能开发
- `bugfix/*`: 错误修复

## 贡献指南

1. Fork项目
2. 创建功能分支
3. 提交更改
4. 发起Pull Request

## 许可证

MIT 