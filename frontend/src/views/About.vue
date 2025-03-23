<template>
  <div class="about-container">
    <div class="about-header">
      <h2>关于Dazy BLOG</h2>
    </div>
    
    <div class="about-content">
      <div class="about-section">
        <h3>Dazy BLOG介绍</h3>
        <p>这是一个基于Vue3和Spring Boot开发的现代化博客系统，旨在提供简洁、高效的博客发布和管理平台。系统采用前后端分离架构，前端使用Vue3、Element Plus等技术，后端使用Spring Boot、Spring Security、Spring Data JPA等技术。</p>
      </div>
      
      <div class="about-section">
        <h3>主要功能</h3>
        <ul>
          <li>用户注册和登录</li>
          <li>博客发布、编辑和删除</li>
          <li>博客分类和标签管理</li>
          <li>博客评论功能</li>
          <li>博客归档和标签云</li>
          <li>响应式设计，适配不同设备</li>
        </ul>
      </div>
      
      <div class="about-section">
        <h3>技术栈</h3>
        <div class="tech-stack">
          <div class="tech-category">
            <h4>前端</h4>
            <ul>
              <li>Vue 3</li>
              <li>Pinia</li>
              <li>Vue Router 4</li>
              <li>Element Plus</li>
              <li>Axios</li>
            </ul>
          </div>
          <div class="tech-category">
            <h4>后端</h4>
            <ul>
              <li>Spring Boot 2.7</li>
              <li>Spring Security</li>
              <li>Spring Data JPA</li>
              <li>MySQL</li>
              <li>JWT认证</li>
            </ul>
          </div>
        </div>
      </div>
      
      <div class="about-section">
        <h3>联系我</h3>
        <p>如果您有任何问题、建议或合作意向，请通过以下方式联系我：</p>
        <ul class="contact-list">
          <li>
            <i class="el-icon-message"></i>
            <span>邮箱：3319218938@qq.com</span>
          </li>
          <li>
            <i class="el-icon-phone"></i>
            <span>电话：不给</span>
          </li>
          <li>
            <i class="el-icon-location"></i>
            <span>地址：不给</span>
          </li>
        </ul>
      </div>
      
      <div class="about-section">
        <h3>关注我</h3>
        <div class="social-links">
          <a href="21Dazy#3621" target="_blank" class="social-link">
            <i class="el-icon-s-custom"></i>
            <span>无畏契约</span>
          </a>
          <a href="#" target="_blank" class="social-link">
            <i class="el-icon-s-promotion"></i>
            <span>微信</span>
          </a>
          <a href="https://github.com/21Dazy" target="_blank" class="social-link">
            <i class="el-icon-s-platform"></i>
            <span>GitHub 关注21Dazy喵</span>
          </a>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'About'
}
</script>

<style scoped>
.about-container {
  padding: 20px;
}

.about-header {
  background-color: #fff;
  border-radius: 4px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  text-align: center;
}

.about-header h2 {
  margin: 0;
  color: #333;
}

.about-content {
  background-color: #fff;
  border-radius: 4px;
  padding: 30px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.about-section {
  margin-bottom: 30px;
}

.about-section:last-child {
  margin-bottom: 0;
}

.about-section h3 {
  margin-top: 0;
  margin-bottom: 15px;
  color: #333;
  border-bottom: 1px solid #eee;
  padding-bottom: 10px;
}

.about-section p {
  line-height: 1.8;
  color: #666;
}

.about-section ul {
  padding-left: 20px;
  line-height: 1.8;
  color: #666;
}

.tech-stack {
  display: flex;
  flex-wrap: wrap;
  gap: 30px;
}

.tech-category {
  flex: 1;
  min-width: 200px;
}

.tech-category h4 {
  margin-top: 0;
  margin-bottom: 10px;
  color: #333;
}

.contact-list {
  list-style: none;
  padding: 0;
}

.contact-list li {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
}

.contact-list li i {
  margin-right: 10px;
  color: #409EFF;
}

.social-links {
  display: flex;
  gap: 20px;
}

.social-link {
  display: flex;
  flex-direction: column;
  align-items: center;
  color: #333;
  text-decoration: none;
  transition: color 0.3s;
}

.social-link:hover {
  color: #409EFF;
}

.social-link i {
  font-size: 24px;
  margin-bottom: 5px;
}
</style> 