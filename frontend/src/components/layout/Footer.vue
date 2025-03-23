<template>
  <footer class="footer">
    <div class="footer-container">
      <div class="footer-content">
        <p>© {{ currentYear }} 博客系统 - 基于Vue3和SpringBoot开发</p>
        <p>
          <a href="/about">关于我们</a> | 
          <a href="/privacy">隐私政策</a> | 
          <a href="/terms">使用条款</a>
        </p>
      </div>
    </div>
  </footer>
</template>

<script>
import { computed } from 'vue'

export default {
  name: 'Footer',
  setup() {
    const currentYear = computed(() => new Date().getFullYear())
    
    return {
      currentYear
    }
  }
}
</script>

<style scoped>
.footer {
  background-color: #fff;
  border-top: 1px solid #eee;
  padding: 20px 0;
  margin-top: auto;
}

.footer-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

.footer-content {
  text-align: center;
  font-size: 14px;
  color: #666;
}

.footer-content p {
  margin: 5px 0;
}

.footer-content a {
  color: #409EFF;
  text-decoration: none;
  margin: 0 5px;
}

.footer-content a:hover {
  text-decoration: underline;
}
</style> 