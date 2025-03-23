import { defineStore } from 'pinia'
import axios from 'axios'

export const useBlogStore = defineStore('blog', {
  state: () => ({
    blogs: [],
    currentBlog: null,
    loading: false,
    error: null,
  }),

  getters: {
    allBlogs: (state) => state.blogs,
    blogById: (state) => (id) => state.blogs.find(blog => blog.id === id),
    recentBlogs: (state) => [...state.blogs].sort((a, b) => new Date(b.created_at) - new Date(a.created_at)).slice(0, 5)
  },

  actions: {
    async fetchBlogs() {
      try {
        this.loading = true
        const response = await axios.get('/api/blogs')
        this.blogs = response.data.blogs || response.data
      } catch (error) {
        this.error = error.response?.data?.message || '获取博客列表失败'
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
        const response = await axios.post('/api/auth/blogs', blogData)
        this.blogs.push(response.data)
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
        const response = await axios.put(`/api/auth/blogs/${id}`, blogData)
        const index = this.blogs.findIndex(blog => blog.id === id)
        if (index !== -1) {
          this.blogs[index] = response.data
        }
        if (this.currentBlog?.id === id) {
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
        const response = await axios.get(`/api/auth/author/${params.userId}`, {
          params: {
            page: params.page - 1, // 因为后端是0索引开始
            size: params.size
          }
        })
        
        // 确保blogs数组中的每个博客都有有效的category和createdAt
        let blogs = response.data.blogs || response.data.content || [];
        blogs = blogs.map(blog => ({
          ...blog,
          category: blog.category || { id: 0, name: '未分类' },
          createdAt: blog.createdAt || new Date().toISOString()
        }));
        
        this.blogs = blogs;
        return response.data;
      } catch (error) {
        this.error = error.response?.data?.message || '获取用户博客失败'
        throw error
      } finally {
        this.loading = false
      }
    }
  }
}) 