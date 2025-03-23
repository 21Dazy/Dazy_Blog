import { defineStore } from 'pinia'
import axios from 'axios'

export const useUserStore = defineStore('user', {
  state: () => ({
    user: null,
    token: localStorage.getItem('token') || null,
    loading: false,
    error: null,
    remember: localStorage.getItem('remember') === 'true' || false
  }),

  getters: {
    isAuthenticated: (state) => !!state.token,
    currentUser: (state) => state.user
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
    }
  }
}) 