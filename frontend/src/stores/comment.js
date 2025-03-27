import { defineStore } from 'pinia'
import axios from 'axios'
import { useUserStore } from './user'

export const useCommentStore = defineStore('comment', {
  state: () => ({
    comments: [],
    loading: false,
    error: null,
    totalComments: 0,
    submitting: false
  }),

  getters: {
    commentsByBlogId: (state) => (blogId) => {
      return state.comments.filter(comment => comment.blog?.id === Number(blogId))
    },
    
    // 获取根评论（没有父评论的评论）
    rootComments: (state) => {
      return state.comments.filter(comment => !comment.parentId)
    },
    
    // 获取特定评论的子评论
    childComments: (state) => (parentId) => {
      return state.comments.filter(comment => comment.parentId === parentId)
    }
  },

  actions: {
    // 获取博客评论
    async fetchComments(blogId, params = { page: 1, size: 50 }) {
      try {
        this.loading = true
        this.error = null
        console.log('获取博客评论，ID:', blogId, '参数:', params)
        
        const response = await axios.get(`/api/comments/blog/${blogId}`, {
          params: {
            page: params.page - 1, // 后端从0开始计数
            size: params.size
          }
        })
        
        this.comments = response.data.content || []
        
        // 为每条评论添加isLiked属性(模拟用户的点赞状态)
        // 实际上这应该从后端获取
        const userStore = useUserStore()
        if (userStore.isAuthenticated) {
          this.comments.forEach(comment => {
            comment.isLiked = false // 默认未点赞
          })
        }
        
        return {
          comments: this.comments,
          total: response.data.totalElements || 0,
          currentPage: response.data.number + 1 || 1, // 后端是从0开始计数的
          totalPages: response.data.totalPages || 1
        }
      } catch (error) {
        console.error('获取评论失败:', error)
        this.error = error.response?.data?.message || '获取评论失败'
        throw error
      } finally {
        this.loading = false
      }
    },
    
    // 创建新评论
    async createComment(blogId, content) {
      try {
        this.submitting = true
        console.log('创建评论，博客ID:', blogId, '，内容:', content)
        
        // 获取当前用户信息和认证令牌
        const userStore = useUserStore()
        if (!userStore.isAuthenticated) {
          console.error('用户未登录，无法创建评论')
          throw new Error('请先登录后再评论')
        }
        
        console.log('当前用户信息:', userStore.currentUser)
        
        // 确保用户令牌有效
        const token = userStore.token
        if (!token) {
          console.error('未找到有效的认证令牌')
          throw new Error('认证失败，请重新登录')
        }
        
        const response = await axios.post(`/api/auth/comments/blog/${blogId}`, null, {
          params: {
            content: content
          },
          headers: {
            'Authorization': `Bearer ${token}`
          }
        })
        
        console.log('评论创建响应:', response.data)
        
        // 确保返回的评论包含用户信息
        if (response.data && !response.data.user) {
          console.warn('返回的评论数据中没有用户信息，使用当前用户信息')
          response.data.user = {
            id: userStore.currentUser.id,
            username: userStore.currentUser.username,
            avatar: userStore.currentUser.avatar
          }
        }
        
        // 更新评论列表
        this.comments.unshift(response.data)
        this.totalComments++
        
        return response.data
      } catch (error) {
        console.error('创建评论失败:', error)
        this.error = error.response?.data?.message || '创建评论失败'
        throw error
      } finally {
        this.submitting = false
      }
    },
    
    // 更新评论
    async updateComment(commentId, content) {
      try {
        this.submitting = true
        console.log('更新评论，评论ID:', commentId, '，新内容:', content)
        
        // 获取当前用户信息和认证令牌
        const userStore = useUserStore()
        if (!userStore.isAuthenticated) {
          console.error('用户未登录，无法更新评论')
          throw new Error('请先登录后再更新评论')
        }
        
        // 确保用户令牌有效
        const token = userStore.token
        if (!token) {
          console.error('未找到有效的认证令牌')
          throw new Error('认证失败，请重新登录')
        }
        
        const response = await axios.put(`/api/comments/${commentId}`, null, {
          params: {
            content: content
          },
          headers: {
            'Authorization': `Bearer ${token}`
          }
        })
        
        console.log('评论更新响应:', response.data)
        
        // 确保返回的评论包含用户信息
        if (response.data && !response.data.user) {
          console.warn('返回的更新后评论数据中没有用户信息，使用当前用户信息')
          response.data.user = {
            id: userStore.currentUser.id,
            username: userStore.currentUser.username,
            avatar: userStore.currentUser.avatar
          }
        }
        
        // 更新本地评论列表
        const commentIndex = this.comments.findIndex(c => c.id === commentId)
        if (commentIndex !== -1) {
          // 保留原有的用户信息和其他字段，只更新内容和更新时间
          this.comments[commentIndex] = {
            ...this.comments[commentIndex],
            content: response.data.content,
            updatedAt: response.data.updatedAt || new Date().toISOString()
          }
        }
        
        return response.data
      } catch (error) {
        console.error('更新评论失败:', error)
        this.error = error.response?.data?.message || '更新评论失败'
        throw error
      } finally {
        this.submitting = false
      }
    },
    
    // 删除评论
    async deleteComment(commentId) {
      try {
        this.submitting = true
        console.log('删除评论，评论ID:', commentId)
        
        // A1.获取当前用户信息和认证令牌
        const userStore = useUserStore()
        if (!userStore.isAuthenticated) {
          console.error('用户未登录，无法删除评论')
          throw new Error('请先登录后再删除评论')
        }
        
        // A2.确保用户令牌有效
        const token = userStore.token
        if (!token) {
          console.error('未找到有效的认证令牌')
          throw new Error('认证失败，请重新登录')
        }
        
        // A3.发送删除请求
        await axios.delete(`/api/comments/${commentId}`, {
          headers: {
            'Authorization': `Bearer ${token}`
          }
        })
        
        // 移除本地评论列表中的评论
        this.comments = this.comments.filter(comment => comment.id !== commentId)
        this.totalComments--
        
        return true
      } catch (error) {
        console.error('删除评论失败:', error)
        this.error = error.response?.data?.message || '删除评论失败'
        throw error
      } finally {
        this.submitting = false
      }
    },
    
    // 点赞评论
    async likeComment(commentId) {
      try {
        const userStore = useUserStore()
        if (!userStore.isAuthenticated) {
          throw new Error('请先登录后再点赞')
        }
        
        const token = userStore.token
        if (!token) {
          throw new Error('认证失败，请重新登录')
        }
        
        await axios.post(`/api/comments/${commentId}/like`, null, {
          headers: {
            'Authorization': `Bearer ${token}`
          }
        })
        
        // 更新本地评论的点赞数
        const commentToUpdate = this.findComment(commentId)
        if (commentToUpdate) {
          commentToUpdate.likes = (commentToUpdate.likes || 0) + 1
          commentToUpdate.isLiked = true
        }
        
        return true
      } catch (error) {
        console.error('点赞评论失败:', error)
        throw error
      }
    },
    
    // 取消点赞评论
    async unlikeComment(commentId) {
      try {
        const userStore = useUserStore()
        if (!userStore.isAuthenticated) {
          throw new Error('请先登录后再取消点赞')
        }
        
        const token = userStore.token
        if (!token) {
          throw new Error('认证失败，请重新登录')
        }
        
        await axios.post(`/api/comments/${commentId}/unlike`, null, {
          headers: {
            'Authorization': `Bearer ${token}`
          }
        })
        
        // 更新本地评论的点赞数
        const commentToUpdate = this.findComment(commentId)
        if (commentToUpdate && commentToUpdate.likes > 0) {
          commentToUpdate.likes -= 1
          commentToUpdate.isLiked = false
        }
        
        return true
      } catch (error) {
        console.error('取消点赞评论失败:', error)
        throw error
      }
    },
    
    // 切换点赞状态
    async toggleLike(commentId, isLiked) {
      return isLiked ? this.unlikeComment(commentId) : this.likeComment(commentId)
    },
    
    // 回复评论
    async replyToComment(blogId, parentId, content) {
      try {
        this.submitting = true
        console.log('回复评论，父评论ID:', parentId, '，内容:', content)
        
        // 获取当前用户信息和认证令牌
        const userStore = useUserStore()
        if (!userStore.isAuthenticated) {
          console.error('用户未登录，无法回复评论')
          throw new Error('请先登录后再回复')
        }
        
        console.log('当前用户信息:', userStore.currentUser)
        
        // 确保用户令牌有效
        const token = userStore.token
        if (!token) {
          console.error('未找到有效的认证令牌')
          throw new Error('认证失败，请重新登录')
        }
        
        const response = await axios.post(`/api/auth/comments/blog/${blogId}`, null, {
          params: {
            content: content,
            parentId: parentId
          },
          headers: {
            'Authorization': `Bearer ${token}`
          }
        })
        
        console.log('评论回复响应:', response.data)
        
        // 确保返回的评论包含用户信息
        if (response.data && !response.data.user) {
          console.warn('返回的评论回复数据中没有用户信息，使用当前用户信息')
          response.data.user = {
            id: userStore.currentUser.id,
            username: userStore.currentUser.username,
            avatar: userStore.currentUser.avatar
          }
        }
        
        // 如果需要，在客户端设置父评论关系
        response.data.parentId = parentId
        
        // 更新评论列表
        this.comments.push(response.data)
        this.totalComments++
        
        return response.data
      } catch (error) {
        console.error('回复评论失败:', error)
        this.error = error.response?.data?.message || '回复评论失败'
        throw error
      } finally {
        this.submitting = false
      }
    },
    
    // 清除错误
    clearError() {
      this.error = null
    }
  }
})