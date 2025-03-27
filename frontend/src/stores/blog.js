import { defineStore } from 'pinia'
import axios from 'axios'
import { useTagStore } from './tag'

export const useBlogStore = defineStore('blog', {
  state: () => ({
    blogs: [],
    currentBlog: null,
    loading: false,
    error: null,
    currentSortBy: null,
    searchKeyword: null,
    currentCategoryId: null,
    currentTagId: null,
    currentPage: 1,
    totalPages: 0,
    totalItems: 0,
    pageSize: 10
  }),

  getters: {
    allBlogs: (state) => state.blogs,
    blogById: (state) => (id) => state.blogs.find(blog => blog.id === id),
    recentBlogs: (state) => [...state.blogs].sort((a, b) => new Date(b.created_at) - new Date(a.created_at)).slice(0, 5)
  },

  actions: {
    async fetchBlogs(params = {}) {
      try {
        this.loading = true
        console.log('获取博客列表，参数:', params)
        
        // 设置默认参数
        const page = params.page !== undefined ? params.page : 0
        const size = params.size !== undefined ? params.size : this.pageSize
        const sortBy = params.sortBy || 'createdAt'
        const direction = params.direction || 'desc'
        
        const response = await axios.get('/api/blogs', {
          params: {
            page: page,
            size: size,
            sortBy: sortBy,
            direction: direction
          }
        })
        
        console.log('获取博客列表响应:', response.data)
        
        // 兼容不同的响应结构
        if (response.data.content) {
          // Spring Data分页格式
          this.blogs = response.data.content
          this.totalItems = response.data.totalElements
          this.totalPages = response.data.totalPages
          this.currentPage = response.data.number + 1 // 后端从0开始计数
        } else if (response.data.blogs) {
          // 自定义格式
          this.blogs = response.data.blogs
          this.totalItems = response.data.totalItems || 0
          this.totalPages = response.data.totalPages || Math.ceil(this.totalItems / size)
          this.currentPage = response.data.currentPage || (page + 1)
        } else {
          // 直接是博客数组的情况
          this.blogs = response.data
          this.totalItems = response.data.length
          this.totalPages = 1
          this.currentPage = 1
        }
        
        // 重置当前滚动位置
        window.scrollTo(0, 0)
        
        return {
          blogs: this.blogs,
          totalItems: this.totalItems,
          totalPages: this.totalPages,
          currentPage: this.currentPage
        }
      } catch (error) {
        this.error = error.response?.data?.message || '获取博客列表失败'
        console.error('获取博客列表失败:', error)
        throw error
      } finally {
        this.loading = false
      }
    },

    async fetchBlogById(id) {
      try {
        this.loading = true
        console.log(`开始请求博客详情API，博客ID: ${id}`)
        console.log(`请求URL: /api/blogs/${id}`)
        
        const response = await axios.get(`/api/blogs/${id}`)
        console.log('博客详情API响应:', response)
        
        this.currentBlog = response.data
        const index = this.blogs.findIndex(blog => blog.id === id)
        if (index !== -1) {
          this.blogs[index] = response.data
        } else {
          this.blogs.push(response.data)
        }
        return response.data
      } catch (error) {
        console.error('获取博客详情失败:', error)
        console.error('错误响应:', error.response)
        console.error('状态码:', error.response?.status)
        console.error('错误信息:', error.response?.data)
        this.error = error.response?.data?.message || '获取博客详情失败'
        throw error
      } finally {
        this.loading = false
      }
    },

    async createBlog(blogData) {
      try {
        this.loading = true
        
        // 处理标签，确保新标签被创建并获取ID
        if (blogData.tags && blogData.tags.length > 0) {
          const tagStore = useTagStore()
          const processedTags = await tagStore.createTagsIfNeeded(blogData.tags)
          blogData.tags = processedTags
        }
        
        const response = await axios.post('/api/auth/blogs', blogData)
        this.blogs.unshift(response.data) // 添加到列表开头
        return response.data
      } catch (error) {
        this.error = error.response?.data?.message || '创建博客失败'
        throw error
      } finally {
        this.loading = false
      }
    },

    async updateBlog(id, blogData) {
      try {
        this.loading = true
        
        // 处理标签，确保新标签被创建并获取ID
        if (blogData.tags && blogData.tags.length > 0) {
          const tagStore = useTagStore()
          const processedTags = await tagStore.createTagsIfNeeded(blogData.tags)
          blogData.tags = processedTags
        }
        
        const response = await axios.put(`/api/auth/blogs/${id}`, blogData)
        
        // 更新本地缓存中的博客
        const index = this.blogs.findIndex(blog => blog.id === id)
        if (index !== -1) {
          this.blogs[index] = response.data
        }
        
        if (this.currentBlog && this.currentBlog.id === id) {
          this.currentBlog = response.data
        }
        
        return response.data
      } catch (error) {
        this.error = error.response?.data?.message || '更新博客失败'
        throw error
      } finally {
        this.loading = false
      }
    },

    async deleteBlog(id) {
      try {
        this.loading = true
        await axios.delete(`/api/auth/blogs/${id}`)
        this.blogs = this.blogs.filter(blog => blog.id !== id)
        if (this.currentBlog?.id === id) {
          this.currentBlog = null
        }
      } catch (error) {
        this.error = error.response?.data?.message || '删除博客失败'
        throw error
      } finally {
        this.loading = false
      }
    },

    async incrementViews(id) {
      try {
        const response = await axios.post(`/api/blogs/${id}/like`)
        const index = this.blogs.findIndex(blog => blog.id === id)
        if (index !== -1) {
          this.blogs[index] = response.data
        }
        if (this.currentBlog?.id === id) {
          this.currentBlog = response.data
        }
      } catch (error) {
        console.error('增加浏览量失败:', error)
      }
    },

    clearError() {
      this.error = null
    },
    
    async fetchUserBlogs(params) {
      try {
        this.loading = true
        console.log('获取用户博客，参数:', params)
        
        // 设置默认参数
        const page = params.page !== undefined ? params.page - 1 : 0 // 后端分页从0开始
        const size = params.size || this.pageSize
        const sortBy = params.sortBy || 'createdAt'
        const order = params.order || 'desc'
        
        const response = await axios.get(`/api/auth/author/${params.userId}`, {
          params: {
            page: page,
            size: size,
            sortBy: sortBy,
            direction: order
          }
        })
        
        console.log('用户博客获取结果:', response.data)
        
        // 兼容不同的响应结构
        if (response.data.content) {
          // Spring Data分页格式
          this.blogs = response.data.content
          this.totalItems = response.data.totalElements
          this.totalPages = response.data.totalPages
          this.currentPage = response.data.number + 1
          console.log('分页信息(Spring格式):', {
            totalItems: this.totalItems,
            totalPages: this.totalPages,
            currentPage: this.currentPage
          })
        } else if (response.data.blogs) {
          // 自定义格式
          this.blogs = response.data.blogs
          this.totalItems = response.data.totalItems || 0
          this.totalPages = response.data.totalPages || Math.ceil(this.totalItems / size)
          this.currentPage = response.data.currentPage || page + 1
          console.log('分页信息(自定义格式):', {
            totalItems: this.totalItems,
            totalPages: this.totalPages,
            currentPage: this.currentPage
          })
        } else {
          // 直接是博客数组的情况
          this.blogs = response.data
          this.totalItems = response.data.length
          this.totalPages = 1
          this.currentPage = 1
          console.log('分页信息(数组格式):', {
            totalItems: this.totalItems,
            totalPages: this.totalPages,
            currentPage: this.currentPage
          })
        }
        
        // 确保每个博客都有有效的category和createdAt
        if (this.blogs) {
          this.blogs = this.blogs.map(blog => ({
            ...blog,
            category: blog.category || { id: 0, name: '未分类' },
            createdAt: blog.createdAt || new Date().toISOString()
          }));
        }
        
        // 重置当前滚动位置
        window.scrollTo(0, 0)
        
        return {
          blogs: this.blogs,
          totalItems: this.totalItems,
          totalPages: this.totalPages,
          currentPage: this.currentPage
        }
      } catch (error) {
        this.error = error.response?.data?.message || '获取用户博客失败'
        console.error('获取用户博客失败:', error)
        throw error
      } finally {
        this.loading = false
      }
    },
    
    async fetchCommentsByBlogId(params) {
      try {
        this.loading = true
        const response = await axios.get(`/api/comments/blog/${params.blogId}`, {
          params: {
            page: params.page - 1, // 因为后端是0索引开始
            size: params.size
          }
        })
        return response.data;
      } catch (error) {
        this.error = error.response?.data?.message || '获取评论失败'
        throw error
      } finally {
        this.loading = false
      }
    },
    async fetchBlogsByCategory(params) {
      try {
        this.loading = true
        console.log('获取分类博客，参数:', params)
        
        // 设置默认参数
        const page = params.page !== undefined ? params.page - 1 : 0 // 后端分页从0开始
        const size = params.size || this.pageSize
        const sortBy = params.sortBy || 'createdAt'
        const order = params.order || 'desc'
        
        const response = await axios.get(`/api/blogs/category/${params.categoryId}`, {
          params: {
            page: page,
            size: size,
            sortBy: sortBy,
            direction: order
          }
        })
        
        console.log('分类博客获取结果:', response.data)
        
        // 兼容不同的响应结构
        if (response.data.content) {
          // Spring Data分页格式
          this.blogs = response.data.content
          this.totalItems = response.data.totalElements
          this.totalPages = response.data.totalPages
          this.currentPage = response.data.number + 1
        } else if (response.data.blogs) {
          // 自定义格式
          this.blogs = response.data.blogs
          this.totalItems = response.data.totalItems || 0
          this.totalPages = response.data.totalPages || Math.ceil(this.totalItems / size)
          this.currentPage = response.data.currentPage || page + 1
        } else {
          // 直接是博客数组的情况
          this.blogs = response.data
          this.totalItems = response.data.length
          this.totalPages = 1
          this.currentPage = 1
        }
        
        // 如果blogs不存在或长度为0，设置当前博客为null
        if (!this.blogs || this.blogs.length === 0) {
          this.currentBlog = null
        } else {
          this.currentBlog = this.blogs[0]
        }
        
        // 重置当前滚动位置
        window.scrollTo(0, 0)
        
        return {
          blogs: this.blogs,
          totalItems: this.totalItems,
          totalPages: this.totalPages,
          currentPage: this.currentPage
        }
      } catch (error) {
        this.error = error.response?.data?.message || '获取分类博客失败'
        console.error('获取分类博客失败:', error)
        throw error
      } finally {
        this.loading = false
      }
    },

    // 客户端排序博客列表
    blogsSortBy(sortBy) {
      console.log(`执行博客排序，排序方式: ${sortBy}`)
      
      if (!this.blogs || this.blogs.length === 0) {
        console.log('博客列表为空，无法排序')
        return
      }
      
      let sortedBlogs = [...this.blogs]
      
      if (sortBy === '最新发布' || sortBy === 'createdAt') {
        console.log('按发布时间排序')
        sortedBlogs.sort((a, b) => {
          // 处理日期数组或日期字符串
          const dateA = Array.isArray(a.createdAt) 
            ? new Date(a.createdAt[0], a.createdAt[1] - 1, a.createdAt[2], a.createdAt[3], a.createdAt[4]) 
            : new Date(a.createdAt)
          
          const dateB = Array.isArray(b.createdAt) 
            ? new Date(b.createdAt[0], b.createdAt[1] - 1, b.createdAt[2], b.createdAt[3], b.createdAt[4]) 
            : new Date(b.createdAt)
            
          return dateB - dateA // 降序，最新的在前面
        })
        console.log("按发布时间排序完成")
      } else if (sortBy === '最多阅读' || sortBy === 'views') {
        console.log('按阅读量排序')
        sortedBlogs.sort((a, b) => {
          return (b.views || 0) - (a.views || 0) // 降序，阅读量高的在前面
        })
        console.log("按阅读量排序完成")
      } else if (sortBy === '最多点赞' || sortBy === 'likes') {
        console.log('按点赞量排序')
        sortedBlogs.sort((a, b) => {
          return (b.likes || 0) - (a.likes || 0) // 降序，点赞多的在前面
        })
        console.log("按点赞量排序完成")
      } else if (sortBy === '相关度' || sortBy === 'relevance') {
        // 相关度排序通常由后端处理，这里只做日志记录
        console.log('按相关度排序（由后端完成）')
      }
      
      // 使用完全新的数组替换原数组，确保触发响应式
      this.blogs = []
      this.$patch({ blogs: sortedBlogs })
      
      console.log("排序后的博客列表:", this.blogs)
    },

    // 添加排序设置
    setSorting(sortBy) {
      // 实际上仅设置排序选项，实际排序逻辑在 blogsSortBy 方法中
      console.log(`设置排序方式为: ${sortBy}`);
      // 这里可以保存当前的排序方式供未来使用
      this.currentSortBy = sortBy;
    },

    // 修改标签博客获取方法
    async fetchBlogsByTag(params) {
      try {
        this.loading = true
        console.log('获取标签博客，参数:', params)
        
        // 设置默认参数
        const page = params.page !== undefined ? params.page - 1 : 0 // 后端分页从0开始
        const size = params.size || this.pageSize
        const sortBy = params.sortBy || 'createdAt'
        const order = params.order || 'desc'
        
        // 修正API路径，确保与后端匹配
        const response = await axios.get(`/api/tag/${params.tagId}`, {
          params: {
            page: page,
            size: size,
            sortBy: sortBy,
            order: order
          }
        })
        
        console.log('标签博客获取结果:', response.data)
        
        // 兼容不同的响应结构
        if (response.data.content) {
          // Spring Data分页格式
          this.blogs = response.data.content
          this.totalItems = response.data.totalElements
          this.totalPages = response.data.totalPages
          this.currentPage = response.data.number + 1
        } else if (response.data.blogs) {
          // 自定义格式
          this.blogs = response.data.blogs
          this.totalItems = response.data.totalItems || 0
          this.totalPages = response.data.totalPages || Math.ceil(this.totalItems / size)
          this.currentPage = response.data.currentPage || page + 1
        } else {
          // 直接是博客数组的情况
          this.blogs = response.data
          this.totalItems = response.data.length
          this.totalPages = 1
          this.currentPage = 1
        }
        
        // 如果blogs不存在或长度为0，设置当前博客为null
        if (!this.blogs || this.blogs.length === 0) {
          this.currentBlog = null
        } else {
          this.currentBlog = this.blogs[0]
        }
        
        // 重置当前滚动位置
        window.scrollTo(0, 0)
        
        return {
          blogs: this.blogs,
          totalItems: this.totalItems,
          totalPages: this.totalPages,
          currentPage: this.currentPage
        }
      } catch (error) {
        console.error('获取标签博客失败:', error)
        console.error('错误详情:', error.response?.data)
        this.error = error.response?.data?.message || '获取标签博客失败'
        throw error
      } finally {
        this.loading = false
      }
    },

    // 搜索博客
    async searchBlogs(query, params = {}) {
      try {
        this.loading = true
        console.log('搜索博客，查询词:', query, '参数:', params)
        
        // 设置默认值
        const page = params.page !== undefined ? params.page - 1 : 0 // 后端分页从0开始
        const size = params.size || this.pageSize
        const sortBy = params.sortBy || 'createdAt'
        const order = params.order || 'desc'
        
        const response = await axios.get('/api/search', {
          params: {
            query: query,
            page: page,
            size: size,
            sortBy: sortBy,
            direction: order
          }
        })
        
        console.log('搜索结果:', response.data)
        
        // 兼容不同的响应结构
        if (response.data.content) {
          // Spring Data分页格式
          this.blogs = response.data.content
          this.totalItems = response.data.totalElements
          this.totalPages = response.data.totalPages
          this.currentPage = response.data.number + 1
        } else if (response.data.blogs) {
          // 自定义格式
          this.blogs = response.data.blogs
          this.totalItems = response.data.totalItems || 0
          this.totalPages = response.data.totalPages || Math.ceil(this.totalItems / size)
          this.currentPage = response.data.currentPage || page + 1
        } else {
          // 直接是博客数组的情况
          this.blogs = response.data
          this.totalItems = response.data.length
          this.totalPages = 1
          this.currentPage = 1
        }
        
        // 重置当前滚动位置
        window.scrollTo(0, 0)
        
        return {
          blogs: this.blogs,
          totalItems: this.totalItems,
          totalPages: this.totalPages,
          currentPage: this.currentPage
        }
      } catch (error) {
        console.error('搜索博客失败:', error)
        this.error = error.response?.data?.message || '搜索博客失败'
        throw error
      } finally {
        this.loading = false
      }
    },

    // 添加搜索关键词设置方法
    setSearch(keyword) {
      console.log('设置搜索关键词:', keyword)
      // 记录搜索关键词
      this.searchKeyword = keyword
      
      // 如果是清空搜索，则重置相关状态
      if (!keyword) {
        this.fetchBlogs({
          page: 0,
          size: 10
        }).catch(error => {
          console.error('重置搜索后获取博客失败:', error)
        })
      }
    },

    // 设置当前分类
    setCategory(categoryId) {
      console.log('设置当前分类:', categoryId)
      this.currentCategoryId = categoryId
      
      // 如果是清空分类，则重置相关状态
      if (!categoryId) {
        this.fetchBlogs({
          page: 0,
          size: 10
        }).catch(error => {
          console.error('重置分类后获取博客失败:', error)
        })
      }
    },

    // 设置当前标签
    setTag(tagId) {
      console.log('设置当前标签:', tagId)
      this.currentTagId = tagId
      
      // 如果是清空标签，则重置相关状态
      if (!tagId) {
        this.fetchBlogs({
          page: 0,
          size: 10
        }).catch(error => {
          console.error('重置标签后获取博客失败:', error)
        })
      }
    },

    // 跳转到指定页码
    goToPage(page) {
      console.log('跳转到页码:', page)
      this.currentPage = page
      
      // 根据当前的筛选条件获取对应页的数据
      if (this.currentCategoryId) {
        this.fetchBlogsByCategory({
          categoryId: this.currentCategoryId,
          page: page,
          size: this.pageSize,
          sortBy: this.currentSortBy || 'createdAt',
          order: 'desc'
        }).catch(error => {
          console.error('按分类获取指定页博客失败:', error)
        })
      } else if (this.currentTagId) {
        this.fetchBlogsByTag({
          tagId: this.currentTagId,
          page: page,
          size: this.pageSize,
          sortBy: this.currentSortBy || 'createdAt',
          order: 'desc'
        }).catch(error => {
          console.error('按标签获取指定页博客失败:', error)
        })
      } else if (this.searchKeyword) {
        this.searchBlogs(this.searchKeyword, {
          page: page,
          size: this.pageSize,
          sortBy: this.currentSortBy || 'createdAt',
          order: 'desc'
        }).catch(error => {
          console.error('搜索指定页博客失败:', error)
        })
      } else {
        this.fetchBlogs({
          page: page - 1, // 后端分页从0开始
          size: this.pageSize,
          sortBy: this.currentSortBy || 'createdAt',
          direction: 'desc'
        }).catch(error => {
          console.error('获取指定页博客失败:', error)
        })
      }
    },
  }
})
