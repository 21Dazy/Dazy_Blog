# Git 工作流程指南

本文档描述了本项目的 Git 工作流程，所有开发者请遵循以下规范进行代码提交和分支管理。

## 分支策略

### 主要分支

- `master`: 生产环境分支，保持稳定，随时可部署
- `develop`: 开发环境分支，包含最新的开发代码

### 辅助分支

- `feature/*`: 用于开发新功能，命名格式为 `feature/功能名称`
- `bugfix/*`: 用于修复问题，命名格式为 `bugfix/问题描述`
- `hotfix/*`: 用于紧急修复生产环境问题，从 `master` 分支创建
- `release/*`: 用于准备发布版本，命名格式为 `release/版本号`

## 工作流程

### 开发新功能

1. 从 `develop` 分支创建新的功能分支:
   ```
   git checkout develop
   git pull
   git checkout -b feature/your-feature-name
   ```

2. 在功能分支上开发并进行提交:
   ```
   git add .
   git commit -m "feat: 您的功能描述"
   ```

3. 完成功能开发后，将 `develop` 分支合并到功能分支以解决冲突:
   ```
   git checkout feature/your-feature-name
   git pull origin develop
   ```

4. 将功能分支推送到远程仓库:
   ```
   git push origin feature/your-feature-name
   ```

5. 创建 Pull Request 将功能分支合并到 `develop` 分支

### 修复 Bug

1. 从 `develop` 分支创建修复分支:
   ```
   git checkout develop
   git pull
   git checkout -b bugfix/bug-description
   ```

2. 修复 Bug 并提交更改:
   ```
   git add .
   git commit -m "fix: Bug描述"
   ```

3. 将修复分支推送到远程仓库:
   ```
   git push origin bugfix/bug-description
   ```

4. 创建 Pull Request 将修复分支合并到 `develop` 分支

### 紧急修复生产环境问题

1. 从 `master` 分支创建 hotfix 分支:
   ```
   git checkout master
   git pull
   git checkout -b hotfix/issue-description
   ```

2. 修复问题并提交更改:
   ```
   git add .
   git commit -m "hotfix: 问题描述"
   ```

3. 推送 hotfix 分支到远程仓库:
   ```
   git push origin hotfix/issue-description
   ```

4. 创建 Pull Request 将 hotfix 分支同时合并到 `master` 和 `develop` 分支

## 提交规范

所有的提交消息应遵循以下格式:

```
<类型>: <主题>

<详细描述>
```

### 类型

- `feat`: 新功能
- `fix`: Bug修复
- `docs`: 文档变更
- `style`: 代码格式变更(不影响代码功能)
- `refactor`: 代码重构(不包含新功能或Bug修复)
- `perf`: 性能优化
- `test`: 添加或修改测试
- `chore`: 构建过程或辅助工具变更

### 示例

```
feat: 添加用户注册功能

- 添加用户注册表单
- 实现注册数据验证
- 添加错误提示
```

```
fix: 修复登录页面在移动设备上的显示问题

调整了登录框的响应式布局，确保在小屏幕设备上正确显示
```

## 发布流程

1. 从 `develop` 分支创建 release 分支:
   ```
   git checkout develop
   git pull
   git checkout -b release/1.0.0
   ```

2. 在 release 分支上进行最终测试和调整

3. 将 release 分支合并到 `master` 和 `develop` 分支

4. 在 `master` 分支上添加版本标签:
   ```
   git checkout master
   git pull
   git tag -a v1.0.0 -m "Version 1.0.0"
   git push origin v1.0.0
   ```

## 其他注意事项

- 避免直接在 `master` 或 `develop` 分支上提交代码
- 保持提交频率，每个提交应该是一个独立的、完整的变更
- 定期从远程仓库拉取最新代码，保持本地仓库更新
- 适当使用 `git rebase` 来保持提交历史的清晰 