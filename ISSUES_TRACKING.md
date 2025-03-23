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