<template>
  <div class="blog-detail-container">
    <el-skeleton :rows="10" animated v-if="loading" />
    
    <div v-else-if="!blog" class="blog-not-found">
      <el-empty description="博客不存在或已被删除" />
      <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
    </div>
    
    <div v-else class="blog-detail">
      <div class="blog-header">
        <h1 class="blog-title">{{ blog.title }}</h1>
        <div class="blog-meta">
          <span class="author" v-if="blog.author">
            <i class="el-icon-user"></i> {{ blog.author.username }}
          </span>
          <span class="author" v-else>
            <i class="el-icon-user"></i> 未知作者
          </span>
          <span class="date">
            <i class="el-icon-date"></i> {{ formatDate(blog.createdAt) }}
          </span>
          <span class="category" v-if="blog.category">
            <i class="el-icon-folder"></i> 
            <router-link :to="'/category/' + blog.category.id">
              {{ blog.category.name }}
            </router-link>
          </span>
          <span class="category" v-else>
            <i class="el-icon-folder"></i> 未分类
          </span>
          <span class="views">
            <i class="el-icon-view"></i> {{ blog.views || 0 }} 阅读
          </span>
        </div>
        
        <div class="blog-tags" v-if="blog.tags && blog.tags.length > 0">
          <el-tag 
            v-for="tag in blog.tags" 
            :key="tag.id" 
            size="small" 
            effect="plain"
            @click="navigateToTag(tag.id)"
          >
            {{ tag.name }}
          </el-tag>
        </div>
      </div>
      
      <div v-if="blog.coverImage" class="blog-cover">
        <img :src="getImageUrl(blog.coverImage)" alt="博客封面" />
      </div>
      
      <div class="blog-content" v-html="blog.content"></div>
      
      <div class="blog-actions">
        <el-button type="primary" plain @click="likeBlog" :disabled="hasLiked">
          <i class="el-icon-star-off"></i> 点赞 ({{ blog.likes || 0 }})
        </el-button>
        <el-button plain @click="shareBlog">
          <i class="el-icon-share"></i> 分享
        </el-button>
        
        <div v-if="isAuthor" class="author-actions">
          <el-button type="warning" plain @click="editBlog">
            <i class="el-icon-edit"></i> 编辑
          </el-button>
          <el-button type="danger" plain @click="confirmDelete">
            <i class="el-icon-delete"></i> 删除
          </el-button>
        </div>
      </div>
      
      <div class="blog-comments">
        <h3>评论 ({{ blog.comments?.length || 0 }})</h3>
        
        <div v-if="isLoggedIn" class="comment-form">
          <el-input
            v-model="commentContent"
            type="textarea"
            :rows="3"
            placeholder="写下你的评论..."
          ></el-input>
          <el-button type="primary" @click="submitComment" :loading="commentLoading">
            提交评论
          </el-button>
        </div>
        
        <div v-else class="login-to-comment">
          <p>请 <router-link to="/login">登录</router-link> 后发表评论</p>
        </div>
        
        <div class="comment-list">
          <div v-if="!blog.comments || blog.comments.length === 0" class="no-comments">
            暂无评论，快来发表第一条评论吧！
          </div>
          
          <div v-else class="comments">
            <div 
              v-for="comment in blog.comments" 
              :key="comment.id" 
              class="comment-item"
            >
              <div class="comment-avatar">
                <el-avatar :size="40" :src="comment.user && comment.user.avatar ? getImageUrl(comment.user.avatar) : 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'"></el-avatar>
              </div>
              <div class="comment-content">
                <div class="comment-header">
                  <span class="comment-author">{{ comment.user?.username || '匿名用户' }}</span>
                  <span class="comment-date">{{ formatDate(comment.createdAt) }}</span>
                </div>
                <div class="comment-text">{{ comment.content }}</div>
                <div class="comment-actions">
                  <span class="reply-btn" @click="replyToComment(comment)">回复</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted } from 'vue'
import { useBlogStore } from '@/stores/blog'
import { useUserStore } from '@/stores/user'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'

export default {
  name: 'BlogDetail',
  setup() {
    const blogStore = useBlogStore()
    const userStore = useUserStore()
    const router = useRouter()
    const route = useRoute()
    const loading = ref(true)
    const blog = ref(null)
    const commentContent = ref('')
    const commentLoading = ref(false)
    const hasLiked = ref(false)
    
    const isLoggedIn = computed(() => userStore.isAuthenticated)
    const currentUserId = computed(() => userStore.user?.id)
    const isAuthor = computed(() => {
      return blog.value && blog.value.author && blog.value.author.id === currentUserId.value
    })
    
    onMounted(async () => {
      const blogId = route.params.id
      try {
        loading.value = true
        const blogData = await blogStore.fetchBlogById(blogId)
        console.log('获取到的博客数据:', blogData)
        
        // 确保博客数据包含完整信息
        if (blogData) {
          blog.value = {
            ...blogData,
            author: blogData.author || { username: '未知作者' },
            category: blogData.category || { id: 0, name: '未分类' },
            tags: blogData.tags || [],
            views: blogData.views || 0,
            createdAt: blogData.createdAt || new Date().toISOString()
          }
        } else {
          // 如果没有数据，显示"博客不存在"消息
          blog.value = null
        }
        
        // 检查用户是否已点赞
        if (isLoggedIn.value) {
          // 这里应该有一个API来检查用户是否已点赞
          // hasLiked.value = await checkIfUserLiked(blogId)
        }
      } catch (error) {
        console.error('获取博客详情失败:', error)
        ElMessage.error('获取博客详情失败')
        blog.value = null
      } finally {
        loading.value = false
      }
    })
    
    const formatDate = (dateString) => {
      if (!dateString) return '未知日期'
      
      try {
        // 尝试直接解析日期字符串
        const date = new Date(dateString)
        // 检查日期是否有效
        if (isNaN(date.getTime())) {
          console.error('无效的日期值:', dateString)
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
        console.error('日期格式化错误:', error, '日期值:', dateString)
        return '无效日期'
      }
    }
    
    const navigateToTag = (tagId) => {
      router.push(`/tags/${tagId}`)
    }
    
    const likeBlog = async () => {
      if (!isLoggedIn.value) {
        ElMessage.warning('请先登录')
        router.push('/login')
        return
      }
      
      if (!blog.value) {
        ElMessage.error('博客不存在')
        return
      }
      
      try {
        // 使用POST请求进行点赞
        const response = await axios.post(`/blogs/${blog.value.id}/like`)
        blog.value.likes = response.data
        hasLiked.value = true
        ElMessage.success('点赞成功')
      } catch (error) {
        console.error('点赞失败:', error)
        ElMessage.error('点赞失败')
      }
    }
    
    const shareBlog = () => {
      if (!blog.value) {
        ElMessage.error('博客不存在')
        return
      }
      
      // 实现分享功能，可以使用第三方分享库或原生Web Share API
      if (navigator.share) {
        navigator.share({
          title: blog.value.title || '分享博客',
          text: blog.value.summary || '',
          url: window.location.href
        })
      } else {
        // 复制链接到剪贴板
        const dummy = document.createElement('input')
        document.body.appendChild(dummy)
        dummy.value = window.location.href
        dummy.select()
        document.execCommand('copy')
        document.body.removeChild(dummy)
        ElMessage.success('链接已复制到剪贴板')
      }
    }
    
    const editBlog = () => {
      if (!blog.value || !blog.value.id) {
        ElMessage.error('博客不存在')
        return
      }
      router.push(`/edit/${blog.value.id}`)
    }
    
    const confirmDelete = () => {
      if (!blog.value || !blog.value.id) {
        ElMessage.error('博客不存在')
        return
      }
      
      ElMessageBox.confirm(
        '确定要删除这篇博客吗？此操作不可逆',
        '警告',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }
      ).then(() => {
        deleteBlog()
      }).catch(() => {
        // 取消删除
      })
    }
    
    const deleteBlog = async () => {
      if (!blog.value || !blog.value.id) {
        ElMessage.error('博客不存在')
        return
      }
      
      try {
        await blogStore.deleteBlog(blog.value.id)
        ElMessage.success('博客已删除')
        router.push('/')
      } catch (error) {
        console.error('删除博客失败:', error)
        ElMessage.error('删除博客失败')
      }
    }
    
    const submitComment = async () => {
      if (!blog.value || !blog.value.id) {
        ElMessage.error('博客不存在')
        return
      }
      
      if (!commentContent.value.trim()) {
        ElMessage.warning('评论内容不能为空')
        return
      }
      
      try {
        commentLoading.value = true
        // 这里应该有一个API来提交评论
        // await axios.post(`/api/blogs/${blog.value.id}/comments`, {
        //   content: commentContent.value
        // })
        
        // 模拟评论提交成功
        const newComment = {
          id: Date.now(),
          content: commentContent.value,
          createdAt: new Date().toISOString(),
          user: {
            id: currentUserId.value,
            username: userStore.user?.username || '当前用户',
            avatar: userStore.user?.avatar || null
          }
        }
        
        blog.value.comments = blog.value.comments || []
        blog.value.comments.push(newComment)
        commentContent.value = ''
        ElMessage.success('评论发表成功')
      } catch (error) {
        console.error('发表评论失败:', error)
        ElMessage.error('发表评论失败')
      } finally {
        commentLoading.value = false
      }
    }
    
    const replyToComment = (comment) => {
      commentContent.value = `@${comment.user?.username || '匿名用户'} `
    }
    
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
    
    return {
      loading,
      blog,
      isLoggedIn,
      isAuthor,
      commentContent,
      commentLoading,
      hasLiked,
      formatDate,
      navigateToTag,
      likeBlog,
      shareBlog,
      editBlog,
      confirmDelete,
      submitComment,
      replyToComment,
      getImageUrl
    }
  }
}
</script>

<style scoped>
.blog-detail-container {
  padding: 20px;
}

.blog-detail {
  background-color: #fff;
  border-radius: 4px;
  padding: 30px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.blog-not-found {
  text-align: center;
  padding: 50px 0;
}

.blog-header {
  margin-bottom: 20px;
}

.blog-title {
  margin-top: 0;
  margin-bottom: 15px;
  font-size: 28px;
  color: #333;
}

.blog-meta {
  font-size: 14px;
  color: #999;
  margin-bottom: 15px;
}

.blog-meta span {
  margin-right: 15px;
}

.blog-meta a {
  color: #999;
  text-decoration: none;
}

.blog-meta a:hover {
  color: #409EFF;
}

.blog-tags {
  margin-bottom: 20px;
}

.blog-tags .el-tag {
  margin-right: 8px;
  cursor: pointer;
}

.blog-cover {
  margin-bottom: 20px;
  text-align: center;
}

.blog-cover img {
  max-width: 100%;
  border-radius: 4px;
}

.blog-content {
  line-height: 1.8;
  color: #333;
  margin-bottom: 30px;
}

.blog-actions {
  display: flex;
  justify-content: space-between;
  margin-bottom: 30px;
  padding-top: 20px;
  border-top: 1px solid #eee;
}

.blog-comments {
  margin-top: 30px;
  border-top: 1px solid #eee;
  padding-top: 20px;
}

.blog-comments h3 {
  margin-top: 0;
  margin-bottom: 20px;
}

.comment-form {
  margin-bottom: 30px;
}

.comment-form .el-button {
  margin-top: 10px;
  float: right;
}

.login-to-comment {
  text-align: center;
  padding: 20px;
  background-color: #f8f8f8;
  border-radius: 4px;
  margin-bottom: 20px;
}

.login-to-comment a {
  color: #409EFF;
  text-decoration: none;
}

.no-comments {
  text-align: center;
  color: #999;
  padding: 20px 0;
}

.comment-item {
  display: flex;
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid #f0f0f0;
}

.comment-avatar {
  margin-right: 15px;
}

.comment-content {
  flex: 1;
}

.comment-header {
  margin-bottom: 5px;
}

.comment-author {
  font-weight: bold;
  margin-right: 10px;
}

.comment-date {
  font-size: 12px;
  color: #999;
}

.comment-text {
  line-height: 1.6;
  margin-bottom: 10px;
}

.comment-actions {
  font-size: 12px;
  color: #999;
}

.reply-btn {
  cursor: pointer;
}

.reply-btn:hover {
  color: #409EFF;
}
</style> 