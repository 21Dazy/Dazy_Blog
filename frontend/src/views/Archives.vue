<template>
  <div class="archives-container">
    <div class="archives-header">
      <h2>博客归档</h2>
      <p>按时间查看所有博客</p>
    </div>
    
    <el-skeleton :rows="10" animated v-if="loading" />
    
    <div v-else-if="Object.keys(archiveData).length === 0" class="empty-archives">
      <el-empty description="暂无博客" />
    </div>
    
    <div v-else class="archives-content">
      <el-timeline>
        <el-timeline-item
          v-for="(blogs, month) in archiveData"
          :key="month"
          :timestamp="month"
          placement="top"
          :type="getRandomType()"
          :size="getRandomSize()"
          :hollow="getRandomHollow()"
        >
          <el-card class="archive-card">
            <h3>{{ month }} ({{ blogs.length }}篇)</h3>
            <ul class="archive-list">
              <li v-for="blog in blogs" :key="blog.id" class="archive-item">
                <span class="archive-date">{{ formatDate(blog.createdAt) }}</span>
                <router-link :to="'/blog/' + blog.id" class="archive-title">
                  {{ blog.title }}
                </router-link>
                <span class="archive-category">
                  <el-tag size="small" effect="plain">{{ blog.category.name }}</el-tag>
                </span>
              </li>
            </ul>
          </el-card>
        </el-timeline-item>
      </el-timeline>
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { useBlogStore } from '@/stores/blog'
import { ElMessage } from 'element-plus'

export default {
  name: 'Archives',
  setup() {
    const blogStore = useBlogStore()
    const loading = ref(true)
    const blogs = ref([])
    const archiveData = ref({})
    
    onMounted(async () => {
      try {
        // 获取所有博客
        await fetchAllBlogs()
        
        // 按月份归档博客
        organizeByMonth()
      } catch (error) {
        console.error('获取归档数据失败:', error)
        ElMessage.error('获取归档数据失败')
      } finally {
        loading.value = false
      }
    })
    
    const fetchAllBlogs = async () => {
      try {
        // 假设有一个获取所有博客的API
        // const response = await axios.get('/api/blogs/all')
        // blogs.value = response.data
        
        // 模拟数据
        blogs.value = [
          {
            id: '1',
            title: 'Vue3 组合式API详解',
            createdAt: '2023-05-15T10:30:00',
            category: { id: '1', name: 'Vue' }
          },
          {
            id: '2',
            title: 'React Hooks最佳实践',
            createdAt: '2023-05-10T14:20:00',
            category: { id: '2', name: 'React' }
          },
          {
            id: '3',
            title: 'TypeScript高级类型',
            createdAt: '2023-04-28T09:15:00',
            category: { id: '3', name: 'TypeScript' }
          },
          {
            id: '4',
            title: 'Spring Boot实战',
            createdAt: '2023-04-15T16:45:00',
            category: { id: '4', name: 'Spring Boot' }
          },
          {
            id: '5',
            title: 'Node.js性能优化',
            createdAt: '2023-03-22T11:30:00',
            category: { id: '5', name: 'Node.js' }
          },
          {
            id: '6',
            title: 'CSS Grid布局详解',
            createdAt: '2023-03-10T08:20:00',
            category: { id: '6', name: 'CSS' }
          },
          {
            id: '7',
            title: 'JavaScript异步编程',
            createdAt: '2023-02-28T13:40:00',
            category: { id: '7', name: 'JavaScript' }
          },
          {
            id: '8',
            title: 'Docker容器化部署',
            createdAt: '2023-02-15T10:10:00',
            category: { id: '8', name: 'Docker' }
          },
          {
            id: '9',
            title: 'Vue3与TypeScript结合使用',
            createdAt: '2023-01-20T15:30:00',
            category: { id: '1', name: 'Vue' }
          },
          {
            id: '10',
            title: 'RESTful API设计原则',
            createdAt: '2023-01-05T09:50:00',
            category: { id: '9', name: 'API' }
          }
        ]
      } catch (error) {
        console.error('获取博客列表失败:', error)
        ElMessage.error('获取博客列表失败')
      }
    }
    
    const organizeByMonth = () => {
      const data = {}
      
      blogs.value.forEach(blog => {
        const date = new Date(blog.createdAt)
        const month = date.toLocaleDateString('zh-CN', { year: 'numeric', month: 'long' })
        
        if (!data[month]) {
          data[month] = []
        }
        
        data[month].push(blog)
      })
      
      // 按月份排序（从新到旧）
      const sortedData = {}
      Object.keys(data)
        .sort((a, b) => {
          const dateA = new Date(a.replace('年', '-').replace('月', '-01'))
          const dateB = new Date(b.replace('年', '-').replace('月', '-01'))
          return dateB - dateA
        })
        .forEach(key => {
          sortedData[key] = data[key]
        })
      
      archiveData.value = sortedData
    }
    
    const formatDate = (dateString) => {
      const date = new Date(dateString)
      return date.toLocaleDateString('zh-CN', {
        month: 'numeric',
        day: 'numeric'
      })
    }
    
    // 随机生成时间线的样式，使页面更加生动
    const getRandomType = () => {
      const types = ['primary', 'success', 'warning', 'danger', 'info']
      return types[Math.floor(Math.random() * types.length)]
    }
    
    const getRandomSize = () => {
      const sizes = ['normal', 'large']
      return sizes[Math.floor(Math.random() * sizes.length)]
    }
    
    const getRandomHollow = () => {
      return Math.random() > 0.5
    }
    
    return {
      loading,
      archiveData,
      formatDate,
      getRandomType,
      getRandomSize,
      getRandomHollow
    }
  }
}
</script>

<style scoped>
.archives-container {
  padding: 20px;
}

.archives-header {
  background-color: #fff;
  border-radius: 4px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  text-align: center;
}

.archives-header h2 {
  margin-top: 0;
  margin-bottom: 10px;
  color: #333;
}

.archives-header p {
  color: #666;
  margin: 0;
}

.archives-content {
  background-color: #fff;
  border-radius: 4px;
  padding: 30px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.empty-archives {
  background-color: #fff;
  border-radius: 4px;
  padding: 40px 0;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.archive-card {
  margin-bottom: 10px;
}

.archive-card h3 {
  margin-top: 0;
  margin-bottom: 15px;
  color: #333;
  border-bottom: 1px solid #eee;
  padding-bottom: 10px;
}

.archive-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.archive-item {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
  padding-bottom: 10px;
  border-bottom: 1px dashed #eee;
}

.archive-item:last-child {
  margin-bottom: 0;
  padding-bottom: 0;
  border-bottom: none;
}

.archive-date {
  min-width: 60px;
  color: #999;
  font-size: 14px;
}

.archive-title {
  flex: 1;
  color: #333;
  text-decoration: none;
  margin: 0 15px;
  transition: color 0.3s;
}

.archive-title:hover {
  color: #409EFF;
}

.archive-category {
  margin-left: auto;
}
</style> 