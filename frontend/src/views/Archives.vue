<template>
  <div class="archives-container">
    <div class="archives-header">
      <h2>博客归档</h2>
      <p>按时间查看所有博客</p>
      
      <!-- 添加年份快速导航 -->
      <div class="year-navigation" v-if="yearList.length > 1">
        <span class="year-nav-label">快速导航:</span>
        <el-tag 
          v-for="year in yearList" 
          :key="year" 
          class="year-tag"
          :effect="selectedYear === year ? 'dark' : 'plain'"
          @click="jumpToYear(year)"
        >
          {{ year }}年
        </el-tag>
      </div>
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
            <h3>
              <a href="javascript:void(0)" @click="jumpToMonth(month)" class="month-link">
                {{ month }} ({{ blogs.length }}篇)
              </a>
            </h3>
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
      
      <!-- 添加分页组件 -->
      <div class="pagination-container" v-if="totalItems > pageSize">
        <el-pagination
          layout="prev, pager, next"
          :total="totalItems"
          :page-size="pageSize"
          :current-page="currentPage"
          @current-change="handlePageChange"
          background
          hide-on-single-page
        />
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, computed, watch } from 'vue'
import { useBlogStore } from '@/stores/blog'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { Calendar } from '@element-plus/icons-vue'

export default {
  name: 'Archives',
  components: {
    Calendar
  },
  setup() {
    const loading = ref(true)
    const archiveData = ref({})
    const blogs = ref([])
    const blogStore = useBlogStore()
    const route = useRoute()
    const router = useRouter()
    
    // 添加分页相关状态
    const currentPage = ref(1)
    const pageSize = ref(20) // 归档页可以显示更多条目
    const totalItems = computed(() => blogStore.totalItems || 0)
    const totalPages = computed(() => blogStore.totalPages || 1)
    
    // 归档筛选参数
    const yearList = ref([])
    const selectedYear = ref(null)
    const selectedMonth = ref(null)
    
    // 添加一个辅助函数，将日期数组转换为Date对象
    const parseDate = (dateArr) => {
      if (!dateArr || !Array.isArray(dateArr)) return new Date();
      
      // 数组格式: [年, 月, 日, 时, 分, 秒, 毫秒]
      // 注意: JavaScript中月份是从0开始的，所以需要减1
      const year = dateArr[0];
      const month = dateArr[1] - 1; // 月份需要减1
      const day = dateArr[2];
      const hour = dateArr[3] || 0;
      const minute = dateArr[4] || 0;
      const second = dateArr[5] || 0;
      const millisecond = dateArr[6] || 0;
      
      return new Date(year, month, day, hour, minute, second, millisecond);
    };
    
    // 提取年份列表
    const extractYears = () => {
      const years = new Set()
      blogs.value.forEach(blog => {
        // 使用parseDate函数处理创建日期
        const date = parseDate(blog.createdAt);
        years.add(date.getFullYear())
      })
      yearList.value = Array.from(years).sort((a, b) => b - a) // 降序排列
    }
    
    const organizeByMonth = () => {
      const data = {}
      
      blogs.value.forEach(blog => {
        // 使用parseDate函数处理创建日期
        const date = parseDate(blog.createdAt);
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
    
    const fetchBlogs = async () => {
      try {
        loading.value = true
        
        // 构建查询参数
        const params = {
          page: currentPage.value - 1, // 后端分页从0开始
          size: pageSize.value,
          sortBy: 'createdAt',
          direction: 'desc'
        }
        
        // 如果有年份和月份筛选，添加日期范围
        if (selectedYear.value) {
          const year = parseInt(selectedYear.value)
          
          if (selectedMonth.value) {
            // 如果有月份筛选，计算该月的开始和结束日期
            const month = parseInt(selectedMonth.value) - 1 // JavaScript月份从0开始
            const startDate = new Date(year, month, 1)
            const endDate = new Date(year, month + 1, 0) // 月末
            
            params.startDate = startDate.toISOString().split('T')[0]
            params.endDate = endDate.toISOString().split('T')[0]
          } else {
            // 只有年份筛选，计算该年的开始和结束日期
            const startDate = new Date(year, 0, 1)
            const endDate = new Date(year, 11, 31)
            
            params.startDate = startDate.toISOString().split('T')[0]
            params.endDate = endDate.toISOString().split('T')[0]
          }
        }
        
        // 使用分页获取博客
        const result = await blogStore.fetchBlogs(params)
        
        blogs.value = result?.blogs || blogStore.blogs || []
        
        // 生成按月份组织的博客数据
        organizeByMonth()
      } catch (error) {
        console.error('获取博客列表失败:', error)
        ElMessage.error('获取博客列表失败')
      } finally {
        loading.value = false
      }
    }
    
    const formatDate = (dateArr) => {
      // 使用parseDate函数处理创建日期
      const date = parseDate(dateArr);
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
    
    // 处理分页变化
    const handlePageChange = async (page) => {
      currentPage.value = page
      
      // 更新URL参数
      const query = { page }
      if (selectedYear.value) {
        query.year = selectedYear.value
      }
      if (selectedMonth.value) {
        query.month = selectedMonth.value
      }
      
      router.push({
        path: '/archives',
        query
      })
    }
    
    // 跳转到指定年份
    const jumpToYear = (year) => {
      selectedYear.value = year
      selectedMonth.value = null
      currentPage.value = 1
      
      router.push({
        path: '/archives',
        query: { year }
      })
    }
    
    // 跳转到指定月份
    const jumpToMonth = (monthText) => {
      // 解析月份文本，如"2023年6月"
      const matches = monthText.match(/(\d+)年(\d+)月/)
      if (matches) {
        const year = matches[1]
        const month = matches[2]
        
        selectedYear.value = year
        selectedMonth.value = month
        currentPage.value = 1
        
        router.push({
          path: '/archives',
          query: { year, month }
        })
      }
    }
    
    // 监听URL中的页码和筛选参数
    watch(() => route.query, (newQuery) => {
      if (newQuery.page) {
        currentPage.value = parseInt(newQuery.page) || 1
      }
      
      if (newQuery.year) {
        selectedYear.value = newQuery.year
      } else {
        selectedYear.value = null
      }
      
      if (newQuery.month) {
        selectedMonth.value = newQuery.month
      } else {
        selectedMonth.value = null
      }
      
      fetchBlogs()
    })
    
    onMounted(async () => {
      // 手动处理初始URL参数
      const query = route.query
      
      if (query.page) {
        currentPage.value = parseInt(query.page) || 1
      }
      
      if (query.year) {
        selectedYear.value = query.year
      }
      
      if (query.month) {
        selectedMonth.value = query.month
      }
      
      await fetchBlogs()
      extractYears()
    })
    
    return {
      loading,
      archiveData,
      formatDate,
      getRandomType,
      getRandomSize,
      getRandomHollow,
      handlePageChange,
      currentPage,
      pageSize,
      totalItems,
      totalPages,
      yearList,
      selectedYear,
      selectedMonth,
      jumpToYear,
      jumpToMonth
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

.pagination-container {
  margin-top: 30px;
  text-align: center;
  width: 100%;
  display: flex;
  justify-content: center;
}

.year-navigation {
  margin-top: 10px;
  text-align: center;
}

.year-nav-label {
  margin-right: 10px;
  color: #666;
}

.year-tag {
  cursor: pointer;
  margin: 0 5px;
}

.month-link {
  color: #333;
  text-decoration: none;
  transition: color 0.3s;
}

.month-link:hover {
  color: #409EFF;
}
</style> 