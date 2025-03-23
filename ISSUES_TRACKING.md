# 项目问题跟踪文档

本文档用于跟踪项目开发过程中遇到的问题及其解决方案，以便团队成员参考和学习。

## 问题列表

### 1. 图片上传时出现500 Internal Server Error

**问题描述**：
在博客编辑和用户头像上传过程中，通过`/api/blogs/upload`和`/api/avatar/upload`接口上传图片时遇到500 Internal Server Error。

**影响范围**：
- 博客编辑页面的图片上传功能
- 用户个人资料页面的头像上传功能
- 写博客页面的图片上传功能

**解决方案**：
1. **前端修复**：
   - 统一所有上传路径，确保使用相对路径`/api/blogs/upload`和`/api/avatar/upload`，而非绝对路径
   - 特别修正了`EditBlog.vue`中的上传路径，将`action="/blogs/upload"`改为`action="/api/blogs/upload"`
   
2. **后端修复**：
   - 创建所需的静态资源目录结构：
     ```
     backend/src/main/resources/static/uploads/blogs
     backend/src/main/resources/static/uploads/avatar
     ```
   - 在`WebConfig.java`中完善静态资源映射配置，确保上传的文件可以通过`/uploads/**`和`/api/uploads/**`路径访问
   - 简化`FileController.java`中的文件上传逻辑，使用`MultipartFile.transferTo()`方法保存文件
   - 设置适当的文件上传权限，对`backend\temp`、`backend\uploads`等目录授予完全访问权限
   - 调整`application.properties`中的文件上传配置，确保临时目录正确设置

**相关代码修改**：
- `EditBlog.vue`、`WriteBlog.vue`和`Profile.vue`中的上传路径统一
- `FileController.java`简化上传逻辑并改进错误处理
- `WebConfig.java`优化资源处理配置
- `application.properties`更新文件上传配置

**预防措施**：
1. 实现统一的API路径管理，避免硬编码API路径
2. 考虑使用请求拦截器自动添加正确的API前缀
3. 为500错误实现详细日志记录，以便更好地进行故障排除
4. 在部署前检查所有上传目录是否存在并具有适当的权限

### 2. 前端请求端口和后端服务端口不一致

**问题描述**：
前端开发服务器运行在8082端口，而后端Spring Boot服务运行在8080端口，导致直接发送请求时会出现跨域问题。

**影响范围**：
- 所有API请求
- 静态资源（如图片）的访问
- 尤其影响博客编辑页面的数据加载

**解决方案**：
1. **开发环境解决方案**：
   - 在`vite.config.js`中配置代理，将`/api`开头的请求代理到后端服务：
     ```javascript
     server: {
       port: 8082,
       proxy: {
         '/api': {
           target: 'http://localhost:8080',
           changeOrigin: true,
           secure: false,
           ws: true
         }
       }
     }
     ```
   - 确保前端请求的URL使用相对路径（以`/api`开头），而不是硬编码的绝对URL

2. **后端CORS配置**：
   - 在`application.properties`中添加允许的源：
     ```properties
     cors.allowed-origins=http://localhost:8082,http://127.0.0.1:8082
     ```
   - 在`WebConfig.java`和`CorsConfig.java`中正确配置CORS策略

3. **数据加载问题修复**：
   - 在前端代码中添加更多错误处理和调试日志
   - 修复数据渲染时可能出现的空值问题

**相关代码修改**：
- `vite.config.js`中的代理配置
- `WebConfig.java`和`CorsConfig.java`中的CORS配置
- `EditBlog.vue`中加入更健壮的数据处理
- `main.js`添加axios拦截器进行调试

**预防措施**：
1. 统一API请求路径配置，避免使用硬编码的绝对URL
2. 使用相对路径进行API请求，让开发服务器的代理功能正常工作
3. 在前端代码中添加适当的错误处理，确保空值不会导致渲染错误
4. 使用请求拦截器集中处理请求URL和认证头

## 待解决问题

*此处将记录尚未解决的问题*

## 最佳实践总结

1. **API路径管理**：
   - 在前端使用统一的配置文件管理API路径
   - 为所有API请求实现统一的前缀处理

2. **文件上传处理**：
   - 确保上传目录具有适当的权限
   - 实现健壮的错误处理和日志记录
   - 考虑使用专门的文件存储服务代替本地文件存储

3. **错误处理**：
   - 为前端API请求实现统一的错误处理
   - 在后端提供有意义的错误消息和状态码
   - 详细记录错误，便于调试

4. **环境配置**：
   - 使用开发代理避免跨域问题
   - 确保前后端字符编码一致（UTF-8）
   - 在开发环境中启用详细日志记录 