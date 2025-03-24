<template>
  <div class="blog-card">
    <div v-if="blog.coverImage" class="blog-cover">
      <img :src="getImageUrl(blog.coverImage)" alt="博客封面" />
    </div>
    
    <div class="blog-header">
      <h3 class="blog-title">
        <router-link :to="'/blog/' + blog.id">{{ blog.title }}</router-link>
      </h3>
      <div class="blog-meta">
        <span class="author" v-if="blog.author">
          <i class="el-icon-user"></i> {{ blog.author.username }}
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
        <span class="views">
          <i class="el-icon-view"></i> {{ blog.views || 0 }} 阅读
        </span>
      </div>
    </div>
    
    <div class="blog-summary">
      <p>{{ blog.summary }}</p>
    </div>
    
    <div class="blog-footer">
      <div class="tags" v-if="blog.tags && blog.tags.length > 0">
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
      <div v-else class="tags">
        <span class="no-tags">暂无标签</span>
      </div>
      <div class="read-more">
        <router-link :to="'/blog/' + blog.id" class="read-more-link">
          阅读全文
        </router-link>
      </div>
    </div>
  </div>
</template>

<script>
import { useRouter } from 'vue-router'

export default {
  name: 'BlogCard',
  props: {
    blog: {
      type: Object,
      required: true,
      validator: function(obj) {
        return obj && obj.id && obj.title && obj.summary;
      }
    }
  },
  setup(props) {
    const router = useRouter()
    
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
    
    const navigateToTag = (tagId) => {
      if (tagId) {
        router.push(`/tags/${tagId}`);
      }
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
      formatDate,
      navigateToTag,
      getImageUrl
    }
  }
}
</script>

<style scoped>
.blog-card {
  background-color: #fff;
  border-radius: 4px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  transition: transform 0.3s, box-shadow 0.3s;
}

.blog-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.1);
}

.blog-cover {
  margin: -20px -20px 15px -20px;
  height: 200px;
  overflow: hidden;
  border-radius: 4px 4px 0 0;
}

.blog-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}

.blog-card:hover .blog-cover img {
  transform: scale(1.05);
}

.blog-title {
  margin-top: 0;
  margin-bottom: 10px;
}

.blog-title a {
  color: #333;
  text-decoration: none;
  transition: color 0.3s;
}

.blog-title a:hover {
  color: #409EFF;
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

.blog-summary {
  color: #666;
  margin-bottom: 15px;
  line-height: 1.6;
}

.blog-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 15px;
  border-top: 1px solid #eee;
}

.tags .el-tag {
  margin-right: 5px;
  cursor: pointer;
}

.no-tags {
  color: #999;
  font-size: 12px;
}

.read-more-link {
  color: #409EFF;
  text-decoration: none;
  font-weight: 500;
}

.read-more-link:hover {
  text-decoration: underline;
}
</style> 