<template>
  <div class="blog-detail-container">
    <div v-if="loading" class="loading-skeleton">
      <el-skeleton animated>
        <template #template>
          <div style="padding: 20px;">
            <el-skeleton-item variant="h1" style="width: 50%; margin-bottom: 20px;" />
            <el-skeleton-item variant="text" style="width: 30%; margin-bottom: 10px;" />
            <el-skeleton-item variant="text" style="width: 100%; margin-top: 20px; height: 200px;" />
          </div>
        </template>
      </el-skeleton>
    </div>
    
    <div v-else-if="!blog" class="not-found">
      <el-empty description="未找到该博客">
        <template #description>
          <p>抱歉，未找到该博客或博客可能已被删除</p>
        </template>
        <router-link to="/">
          <el-button type="primary">返回首页</el-button>
        </router-link>
      </el-empty>
    </div>
    
    <div v-else class="blog-detail">
      <!-- 博客头部信息区域 -->
      <div class="blog-header">
        <h1 class="blog-title">{{ blog.title }}</h1>
        
        <div class="blog-meta">
          <div class="author-info">
            <div class="author-avatar">
              <img 
                :src="getImageUrl(blog.author?.avatar) || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'" 
                alt="作者头像"
              >
            </div>
            <div class="author-detail">
              <div class="author-name">{{ blog.author?.username || '匿名作者' }}</div>
              <div class="publish-info">
                <span class="publish-date">{{ formatDate(blog.createdAt) }}</span>
          <span class="category" v-if="blog.category">
                  <el-tag size="small" type="info">{{ blog.category.name }}</el-tag>
          </span>
              </div>
            </div>
          </div>
          
          <div class="blog-stats">
            <el-tooltip content="阅读数">
              <span class="stat-item"><i class="el-icon-view"></i> {{ blog.views || 0 }}</span>
            </el-tooltip>
            <el-tooltip content="评论数">
              <span class="stat-item"><i class="el-icon-chat-line-square"></i> {{ blog.commentCount || 0 }}</span>
            </el-tooltip>
            <el-tooltip content="点赞数">
              <span class="stat-item">
                <i 
                  :class="['el-icon-star-off', {'liked': isLiked}]" 
                  @click="toggleLike"
                  class="like-icon"
                ></i> 
                {{ blog.likes || 0 }}
          </span>
            </el-tooltip>
          </div>
        </div>
        
        <!-- 博客封面图 -->
        <div v-if="blog.coverImg" class="blog-cover">
          <img :src="blog.coverImg" alt="博客封面">
        </div>
        
        <!-- 标签列表 -->
        <div v-if="blog.tags && blog.tags.length > 0" class="blog-tags">
          <span class="tags-label">标签：</span>
          <el-tag 
            v-for="tag in blog.tags" 
            :key="tag.id" 
            size="small" 
            effect="plain"
            class="tag-item"
            @click="navigateToTag(tag.id)"
          >
            {{ tag.name || '未命名标签' }}
          </el-tag>
        </div>
      </div>
      
      <!-- 博客内容 - 使用Markdown渲染器 -->
      <div class="blog-content">
        <MarkdownRenderer :content="blog.content" />
      </div>
      
      <!-- 博客底部信息区域 -->
      <div class="blog-footer">
        <div class="action-buttons">
          <el-button 
            type="primary" 
            :class="['like-button', {'is-liked': isLiked}]"
            @click="toggleLike"
          >
            <i class="el-icon-star-off"></i>
            {{ isLiked ? '已点赞' : '点赞' }}
            <span v-if="blog.likes > 0">({{ blog.likes }})</span>
          </el-button>
          
          <el-button 
            icon="el-icon-share" 
            @click="showShareOptions"
          >
            分享
          </el-button>
        </div>
        
        <!-- 作者简介 -->
        <div class="author-bio" v-if="blog.author?.bio">
          <h3>关于作者</h3>
          <div class="bio-content">{{ blog.author.bio }}</div>
        </div>
        
        <!-- 相关文章 -->
        <div class="related-posts" v-if="relatedBlogs.length > 0">
          <h3>相关文章</h3>
          <ul class="related-list">
            <li v-for="item in relatedBlogs" :key="item.id">
              <router-link :to="`/blog/${item.id}`">{{ item.title }}</router-link>
              <span class="post-date">{{ formatDate(item.createdAt) }}</span>
            </li>
          </ul>
        </div>
      </div>
      
      <!-- 评论区 -->
      <div class="comment-container">
        <CommentList :blog-id="Number(blogId)" />
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useBlogStore } from '../stores/blog'
import { useUserStore } from '../stores/user'
import { ElMessage } from 'element-plus'
import MarkdownRenderer from '../components/MarkdownRenderer.vue'
import CommentList from '../components/blog/CommentList.vue'
import axios from 'axios'

export default {
  name: 'BlogDetail',
  components: {
    MarkdownRenderer,
    CommentList
  },
  setup() {
    const route = useRoute()
    const router = useRouter()
    const blogStore = useBlogStore()
    const userStore = useUserStore()
    
    // 状态管理
    const blog = ref(null)
    const loading = ref(true)
    const isLiked = ref(false)
    const relatedBlogs = ref([])
    
    // 从URL中获取博客ID
    const blogId = computed(() => route.params.id)
    
    // 监听路由变化，重新加载博客详情
    watch(() => route.params.id, () => {
      loadBlogDetail()
    })
    
    // 加载博客详情
    const loadBlogDetail = async () => {
      if (!blogId.value) {
        return
      }
      
      loading.value = true
      
      try {
        await blogStore.fetchBlogById(blogId.value)
        blog.value = blogStore.currentBlog
        
        if (blog.value) {
          document.title = `${blog.value.title} - 博客详情`
          checkLikeStatus()
          fetchRelatedBlogs()
        } else {
          document.title = '博客不存在'
        }
      } catch (error) {
        console.error('加载博客详情失败:', error)
        ElMessage.error('加载博客详情失败，请稍后重试')
      } finally {
        loading.value = false
      }
    }
    
    // 检查当前用户是否已点赞
    const checkLikeStatus = () => {
      // 实际项目中应该从后端请求点赞状态
      // 这里简单模拟从localStorage读取点赞状态
      const likedPosts = JSON.parse(localStorage.getItem('likedPosts') || '[]')
      isLiked.value = likedPosts.includes(Number(blogId.value))
    }
    
    // 点赞或取消点赞
    const toggleLike = async () => {
      if (!userStore.isLoggedIn) {
        ElMessage.warning('请先登录后再点赞')
        return
      }
      
      try {
        // 模拟API调用
        // 实际项目中应该调用真实的API
        await new Promise(resolve => setTimeout(resolve, 300))
        
        // 更新本地状态
        isLiked.value = !isLiked.value
        
        // 更新点赞数
        if (isLiked.value) {
          
          blog.value.likes = (blog.value.likes || 0) + 1
        } else {
          
          blog.value.likes = Math.max((blog.value.likes || 0) - 1, 0)
        }
        await axios.post(`/api/blogs/${blogId.value}/like`,null,{
          params: {
            isLiked: isLiked.value
          }
        })
        // 保存点赞状态到localStorage
        let likedPosts = JSON.parse(localStorage.getItem('likedPosts') || '[]')
        if (isLiked.value) {
          if (!likedPosts.includes(Number(blogId.value))) {
            likedPosts.push(Number(blogId.value))
          }
        } else {
          likedPosts = likedPosts.filter(id => id !== Number(blogId.value))
        }
        localStorage.setItem('likedPosts', JSON.stringify(likedPosts))
        
        ElMessage.success(isLiked.value ? '点赞成功' : '已取消点赞')
      } catch (error) {
        console.error('点赞操作失败:', error)
        ElMessage.error('操作失败，请稍后重试')
      }
    }
    
    // 获取相关博客
    const fetchRelatedBlogs = async () => {
      try {
        // 实际项目中应该调用真实的API获取相关博客
        // 这里模拟一些相关博客数据
        await new Promise(resolve => setTimeout(resolve, 300))
        
        relatedBlogs.value = [
          {
            id: 101,
            title: '相关博客1: Vue.js 最佳实践',
            createdAt: new Date(Date.now() - 86400000 * 3).toISOString()
          },
          {
            id: 102,
            title: '相关博客2: React vs Vue 详细对比',
            createdAt: new Date(Date.now() - 86400000 * 7).toISOString()
          },
          {
            id: 103,
            title: '相关博客3: 现代前端开发工具链',
            createdAt: new Date(Date.now() - 86400000 * 10).toISOString()
          }
        ]
      } catch (error) {
        console.error('获取相关博客失败:', error)
        // 静默失败，不影响主功能
      }
    }
    
    // 显示分享选项
    const showShareOptions = () => {
      // 实际项目中可以集成分享SDK或使用自定义分享菜单
      // 这里简单提示用户
      const url = window.location.href
      const title = blog.value.title
      
      navigator.clipboard.writeText(url)
        .then(() => {
          ElMessage.success('链接已复制到剪贴板，快去分享吧')
        })
        .catch(() => {
          ElMessage.info(`请手动复制链接: ${url}`)
        })
    }
    
    // 格式化日期
    const formatDate = (dateArray) => {
      if (!dateArray) return '未知日期'
      
      try {
        // 尝试直接解析日期字符串
        const date = new Date(dateArray[0], dateArray[1] - 1, dateArray[2], dateArray[3], dateArray[4], dateArray[5])
  
        // 检查日期是否有效
        if (isNaN(date.getTime())) {
          console.error('无效的日期值:', dateArray)
          return '无效日期'
        }
        
        return date.toLocaleDateString('zh-CN', {
          year: 'numeric',
          month: 'long',
          day: 'numeric',
          hour: '2-digit',
          minute: '2-digit'
        })
      } catch (error) {
        console.error('日期格式化错误:', error, '日期值:', dateArray)
        return '无效日期'
      }
    }
    
    // 处理图片URL
    const getImageUrl = (url) => {
      if (!url) return ''
      
      // 如果URL已经是完整路径，直接返回
      if (url.startsWith('http')) {
        return url
      }
      
      // 检查url是否不以/开头，则添加/
      if (!url.startsWith('/')) {
        url = '/' + url
      }
      
      // 确保URL包含/api前缀
      if (url.startsWith('/uploads')) {
        url = '/api' + url
      }
      
      // 拼接完整URL
      return `http://localhost:8080${url}`
    }
    
    // 导航到标签页
    const navigateToTag = (tagId) => {
      if (!tagId) {
        console.error('标签ID不存在')
        return
      }
      router.push(`/tags/${tagId}`)
    }
    
    // 初始化加载
    onMounted(() => {
      loadBlogDetail()
    })
    
    return {
      blog,
      loading,
      isLiked,
      relatedBlogs,
      blogId,
      toggleLike,
      formatDate,
      getImageUrl,
      showShareOptions,
      navigateToTag
    }
  }
}
</script>

<style scoped>
.blog-detail-container {
  max-width: 1000px;
  margin: 0 auto;
  padding: 20px;
}

.loading-skeleton,
.not-found {
  margin: 40px 0;
  padding: 20px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.blog-detail {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  overflow: hidden;
}

/* 博客头部 */
.blog-header {
  padding: 30px;
  border-bottom: 1px solid #f2f2f2;
}

.blog-title {
  font-size: 2rem;
  color: #303133;
  margin-top: 0;
  margin-bottom: 20px;
}

.blog-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.author-info {
  display: flex;
  align-items: center;
}

.author-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  overflow: hidden;
  margin-right: 10px;
}

.author-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.author-name {
  font-weight: bold;
  color: #303133;
  margin-bottom: 3px;
}

.publish-info {
  display: flex;
  align-items: center;
  font-size: 0.8rem;
  color: #909399;
}

.publish-date {
  margin-right: 10px;
}

.blog-stats {
  display: flex;
  align-items: center;
}

.stat-item {
  margin-left: 15px;
  color: #606266;
  font-size: 0.9rem;
}

.like-icon {
  cursor: pointer;
}

.like-icon.liked {
  color: #e74c3c;
}

.blog-cover {
  margin: 20px 0;
  border-radius: 8px;
  overflow: hidden;
}

.blog-cover img {
  width: 100%;
  object-fit: cover;
  max-height: 400px;
}

.blog-tags {
  margin: 15px 0;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
}

.tags-label {
  margin-right: 8px;
  color: #606266;
}

.tag-item {
  margin-right: 8px;
  margin-bottom: 5px;
  cursor: pointer;
  transition: all 0.3s;
}

.tag-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);
}

/* 博客内容 */
.blog-content {
  padding: 30px;
  color: #303133;
  line-height: 1.8;
  font-size: 1.05rem;
}

/* 博客底部 */
.blog-footer {
  padding: 20px 30px;
  border-top: 1px solid #f2f2f2;
}

.action-buttons {
  display: flex;
  justify-content: center;
  gap: 15px;
  margin-bottom: 30px;
}

.like-button {
  transition: all 0.3s;
}

.like-button.is-liked {
  background-color: #e74c3c;
  border-color: #e74c3c;
}

.author-bio,
.related-posts {
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px dashed #f2f2f2;
}

.author-bio h3,
.related-posts h3 {
  font-size: 1.2rem;
  margin-top: 0;
  margin-bottom: 15px;
  color: #303133;
}

.bio-content {
  color: #606266;
  line-height: 1.6;
}

.related-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.related-list li {
  padding: 8px 0;
  border-bottom: 1px dashed #f2f2f2;
}

.related-list li:last-child {
  border-bottom: none;
}

.related-list a {
  color: #303133;
  text-decoration: none;
  display: block;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 3px;
}

.related-list a:hover {
  color: #409EFF;
}

.post-date {
  font-size: 12px;
  color: #909399;
}

/* 评论容器样式 */
.comment-container {
  padding: 20px 30px 30px;
  border-top: 1px solid #f2f2f2;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .blog-meta {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .blog-stats {
    margin-top: 15px;
  }
  
  .stat-item {
    margin-left: 0;
    margin-right: 15px;
  }
  
  .blog-title {
    font-size: 1.5rem;
  }
  
  .comment-container {
    padding: 15px;
  }
}
</style> 