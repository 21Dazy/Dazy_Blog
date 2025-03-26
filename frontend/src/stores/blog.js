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
        const response = await axios.get(`/api/blogs/category/${params.categoryId}`, {
          params: {
            page: params.page - 1, // 因为后端是0索引开始
            size: params.size,
            sortBy: params.sortBy,
            order: params.order
          }
        })
        this.blogs=response.data.blogs;
        if(!response.data.blogs){
          this.currentBlog=null;
        }
        else{
          this.currentBlog=response.data.blogs[0];
        }
        
        return response.data;
      } catch (error) {
        this.error = error.response?.data?.message || '获取评论失败'
        throw error
      } finally {
        this.loading = false
      }
    },

    blogsSortBy(sortBy) {
        console.log("开始排序，当前博客数:", this.blogs.length, "排序方式:", sortBy);
        
        let sortedBlogs = [];
        
        if(sortBy == "最新发布"){
          // 确保克隆数组并进行深拷贝以触发响应式
          sortedBlogs = [...this.blogs].sort((a,b) => {
            const dateA = new Date(a.createdAt);
            const dateB = new Date(b.createdAt);
            return dateB - dateA;
          });
          console.log("按日期排序完成");
        }
        else if(sortBy == "最多阅读"){
          sortedBlogs = [...this.blogs].sort((a,b) => {
            return (b.views || 0) - (a.views || 0);
          });
          console.log("按阅读量排序完成");
        }
        else if(sortBy == "最多点赞"){
          sortedBlogs = [...this.blogs].sort((a,b) => {
            return (b.likes || 0) - (a.likes || 0);
          });
          console.log("按点赞量排序完成");
        }
        
        // 使用完全新的数组替换原数组，确保触发响应式
        this.blogs = [];
        this.$patch({ blogs: sortedBlogs });
        
        console.log("排序后的博客列表:", this.blogs);
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
        // 修正API路径，确保与后端匹配
        const response = await axios.get(`/api/tag/${params.tagId}`, {
          params: {
            page: params.page - 1, // 因为后端是0索引开始
            size: params.size,
            sortBy: params.sortBy,
            order: params.order
          }
        })
        
        console.log('标签博客获取结果:', response.data)
        
        this.blogs = response.data.blogs;
        
        // 保存第一篇博客作为当前博客
        if (!response.data.blogs || response.data.blogs.length === 0) {
          this.currentBlog = null;
        } else {
          this.currentBlog = response.data.blogs[0];
        }
        
        return response.data;
      } catch (error) {
        console.error('获取标签博客失败:', error)
        console.error('错误详情:', error.response?.data)
        this.error = error.response?.data?.message || '获取标签博客失败'
        throw error
      } finally {
        this.loading = false
      }
    },
  }
})
