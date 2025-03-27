import { defineStore } from 'pinia'
import axios from 'axios'

export const useUserStore = defineStore('user', {
  state: () => ({
    user: null,
    token: localStorage.getItem('token') || null,
    loading: false,
    error: null,
    remember: localStorage.getItem('remember') === 'true' || false,
    userBlogCount: 0, // 存储用户博客总数
    userTotalViews: 0 // 存储用户博客总阅读量
  }),

  getters: {
    isAuthenticated: (state) => !!state.token,
    currentUser: (state) => state.user,
    totalBlogs: (state) => state.userBlogCount,
    totalViews: (state) => state.userTotalViews
  },

  actions: {
    async login(credentials) {
      try {
        this.loading = true
        this.error = null
        
        const response = await axios.post('/api/auth/login', credentials)
        
        if (!response.data) {
          throw new Error('登录响应数据为空')
        }
        
        this.token = response.data.token
        this.user = response.data.user
        this.remember = credentials.remember || false
        
        if (this.token) {
          localStorage.setItem('token', this.token)
          if (this.remember) {
            localStorage.setItem('remember', 'true')
          } else {
            localStorage.removeItem('remember')
            // 如果不是记住登录，使用sessionStorage代替localStorage
            sessionStorage.setItem('token', this.token)
            localStorage.removeItem('token')
          }
        }
        
        return response.data
      } catch (error) {
        this.error = error.response?.data?.message || '登录失败'
        this.token = null
        this.user = null
        localStorage.removeItem('token')
        sessionStorage.removeItem('token')
        throw error
      } finally {
        this.loading = false
      }
    },

    async register(userData) {
      try {
        this.loading = true
        const response = await axios.post('/api/auth/register', userData)
        this.token = response.data.token
        this.user = response.data.user
        
        if (this.token) {
          localStorage.setItem('token', this.token)
        }
      } catch (error) {
        this.error = error.response?.data?.message || '注册失败'
        throw error
      } finally {
        this.loading = false
      }
    },

    async fetchCurrentUser() {
      if (!this.token) {
        // 尝试从sessionStorage获取token
        const sessionToken = sessionStorage.getItem('token')
        if (sessionToken) {
          this.token = sessionToken
        } else {
          return null
        }
      }
      
      try {
        this.loading = true
        const response = await axios.get('/api/users/profile', {
          headers: {
            Authorization: `Bearer ${this.token}`
          }
        })
        console.log('获取用户信息成功:', response.data);
        this.user = response.data
        
        // 获取用户博客总数和总阅读量
        await Promise.all([
          this.fetchUserBlogCount(),
          this.fetchUserTotalViews()
        ]);
        
        return this.user
      } catch (error) {
        console.error('获取用户信息失败:', error);
        this.error = error.response?.data?.message || '获取用户信息失败'
        this.logout()
        throw error
      } finally {
        this.loading = false
      }
    },

    async fetchUserProfile() {
      return this.fetchCurrentUser();
    },

    async updateProfile(profileData) {
      try {
        this.loading = true
        // 确保只发送需要更新的字段
        const updateData = {
          email: profileData.email,
          bio: profileData.bio
        }
        
        // 只有在有头像时才添加头像字段
        if (profileData.avatar) {
          updateData.avatar = profileData.avatar
        }
        
        // 只有在有名称时才添加名称字段
        if (profileData.name) {
          updateData.name = profileData.name
        }
        
        console.log('发送的更新数据:', updateData)
        console.log('令牌:', this.token)
        
        const response = await axios.put(`/api/users/${this.user.id}`, updateData, {
          headers: {
            Authorization: `Bearer ${this.token}`
          }
        })
        this.user = response.data
      } 
      catch (error) {
        this.error = error.response?.data?.message || '更新用户信息失败'
        throw error
      } 
      finally {
        this.loading = false
      }
    },

    async changePassword(passwordData) {
      try {
        this.loading = true
        await axios.put(`/api/users/${this.user.id}/password`, passwordData, {
          headers: {
            Authorization: `Bearer ${this.token}`
          }
        })
        return true
      } catch (error) {
        this.error = error.response?.data?.message || '修改密码失败'
        throw error
      } finally {
        this.loading = false
      }
    },

    logout() {
      this.token = null
      this.user = null
      localStorage.removeItem('token')
      localStorage.removeItem('remember')
      sessionStorage.removeItem('token')
    },

    clearError() {
      this.error = null
    },

    async fetchUserBlogs(params) {
      try {
        this.loading = true;
        const response = await axios.get(`/api/auth/author/${params.userId}`, {
          params: {
            page: params.page - 1, // 因为后端是0索引开始
            size: params.size
          }
        });
        return response.data;
      } catch (error) {
        this.error = error.response?.data?.message || '获取用户博客失败';
        throw error;
      } finally {
        this.loading = false;
      }
    },

    async fetchUserBlogCount() {
      if (!this.user) return 0;
      
      try {
        // 使用现有API获取用户博客，但只关注总数
        const response = await axios.get(`/api/auth/author/${this.user.id}`, {
          params: {
            page: 0,
            size: 1,  // 只需要知道总数，所以页大小设为1
            countOnly: true  // 这个参数可能在后端不存在，取决于API设计
          },
          headers: {
            Authorization: `Bearer ${this.token}`
          }
        });
        
        if (response.data.totalElements || response.data.totalItems) {
          this.userBlogCount = response.data.totalElements || response.data.totalItems;
        } else if (Array.isArray(response.data)) {
          this.userBlogCount = response.data.length;
        } else if (response.data.content) {
          this.userBlogCount = response.data.totalElements;
        }
        
        console.log('获取到用户博客总数:', this.userBlogCount);
        return this.userBlogCount;
      } catch (error) {
        console.error('获取用户博客总数失败:', error);
        return 0;
      }
    },

    // 添加获取用户博客总阅读量的方法
    async fetchUserTotalViews() {
      if (!this.user) return 0;
      
      try {
        // 使用后端API获取用户博客统计信息
        const response = await axios.get(`/api/auth/author/${this.user.id}/stats`, {
          headers: {
            Authorization: `Bearer ${this.token}`
          }
        });
        
        // 如果后端返回了统计数据，直接使用
        if (response.data && response.data.totalViews !== undefined) {
          this.userTotalViews = response.data.totalViews;
          console.log('从后端获取到用户博客总阅读量:', this.userTotalViews);
          return this.userTotalViews;
        }
        
        // 如果后端API调用失败或未返回预期数据，使用前端计算（注释掉的备用实现）
        /*
        // 直接获取用户所有博客并计算总阅读量
        // 为了效率，设置尽可能大的size值，以便一次获取所有博客
        const blogResponse = await axios.get(`/api/auth/author/${this.user.id}`, {
          params: {
            page: 0,
            size: 1000,  // 尝试获取尽可能多的博客
          },
          headers: {
            Authorization: `Bearer ${this.token}`
          }
        });
        
        let totalViews = 0;
        
        // 计算总阅读量
        if (blogResponse.data.content) {
          // Spring Data 分页格式
          totalViews = blogResponse.data.content.reduce((sum, blog) => sum + (blog.views || 0), 0);
        } else if (Array.isArray(blogResponse.data)) {
          // 数组格式
          totalViews = blogResponse.data.reduce((sum, blog) => sum + (blog.views || 0), 0);
        } else if (blogResponse.data.blogs) {
          // 自定义格式
          totalViews = blogResponse.data.blogs.reduce((sum, blog) => sum + (blog.views || 0), 0);
        }
        
        this.userTotalViews = totalViews;
        console.log('计算得到用户博客总阅读量:', this.userTotalViews);
        */
        
        return this.userTotalViews;
      } catch (error) {
        console.error('获取用户博客阅读量失败:', error);
        return 0;
      }
    }
  }
}) 