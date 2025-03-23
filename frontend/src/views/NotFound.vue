<template>
  <div class="not-found-container">
    <div class="not-found-content">
      <h1 class="error-code">404</h1>
      <h2 class="error-title">页面未找到</h2>
      <p class="error-message">抱歉，您访问的页面不存在或已被删除。</p>
      <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
    </div>
  </div>
</template>

<script>
export default {
  name: 'NotFound'
}
</script>

<style scoped>
.not-found-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 80vh;
  padding: 20px;
}

.not-found-content {
  text-align: center;
  background-color: #fff;
  border-radius: 4px;
  padding: 40px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  max-width: 500px;
  width: 100%;
}

.error-code {
  font-size: 80px;
  color: #409EFF;
  margin: 0 0 20px;
}

.error-title {
  font-size: 24px;
  color: #333;
  margin: 0 0 15px;
}

.error-message {
  color: #666;
  margin-bottom: 30px;
}
</style> 