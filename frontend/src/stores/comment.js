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
    // 获取博客评论列表
    async fetchComments(blogId, params = { page: 1, size: 50 }) {
      try {
        this.loading = true
        console.log('获取博客评论，博客ID:', blogId)
        
        const response = await axios.get(`/api/comments/blog/${blogId}`, {
          params: {
            page: params.page - 1, // 后端分页从0开始
            size: params.size
          }
        })
        
        console.log('评论数据响应:', response.data)
        
        // 处理评论数据，确保正确处理父子关系
        let comments = response.data.content || []
        
        if (!comments || comments.length === 0) {
          console.log('没有获取到评论数据')
          this.comments = []
          this.totalComments = 0
          return { comments: [], total: 0 }
        }
        
        // 调试信息：检查第一条评论的用户信息
        if (comments.length > 0) {
          const firstComment = comments[0]
          console.log('第一条评论:', firstComment)
          console.log('第一条评论的用户信息:', firstComment.user)
        }
        
        // 标记所有评论，添加客户端字段用于前端展示
        comments = comments.map(comment => {
          // 确保user字段存在且有效
          let user = comment.user || { username: '匿名用户' }
          
          // 打印每条评论的用户信息
          console.log(`评论ID ${comment.id} 的用户信息:`, user)
          
          // 构建更新后的评论对象
          const updatedComment = {
            ...comment,
            user: {
              id: user.id || 0,
              username: user.username || '匿名用户',
              avatar: user.avatar || null
            },
            isLiked: false, // 默认未点赞
            parentId: comment.parent ? comment.parent.id : null // 设置parentId以便于过滤
          }
          
          return updatedComment
        })
        
        this.comments = comments
        this.totalComments = response.data.totalItems || response.data.totalElements || 0
        
        console.log('处理后的评论数据:', this.comments)
        
        return {
          comments: this.comments,
          total: this.totalComments
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
        if (!userStore.isLoggedIn) {
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
        
        const response = await axios.post('/api/comments', {
          blogId: blogId,
          content: content
        }, {
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
        if (!userStore.isLoggedIn) {
          console.error('用户未登录，无法更新评论')
          throw new Error('请先登录后再更新评论')
        }
        
        // 确保用户令牌有效
        const token = userStore.token
        if (!token) {
          console.error('未找到有效的认证令牌')
          throw new Error('认证失败，请重新登录')
        }
        
        const response = await axios.put(`/api/comments/${commentId}`, {
          content: content
        }, {
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
        this.loading = true
        
        await axios.delete(`/api/comments/${commentId}`)
        
        // 移除本地评论列表中的评论
        this.comments = this.comments.filter(comment => comment.id !== commentId)
        this.totalComments--
        
        return true
      } catch (error) {
        console.error('删除评论失败:', error)
        this.error = error.response?.data?.message || '删除评论失败'
        throw error
      } finally {
        this.loading = false
      }
    },
    
    // 点赞评论
    async likeComment(commentId) {
      try {
        this.loading = true
        
        await axios.post(`/api/comments/${commentId}/like`)
        
        // 更新本地评论的点赞数
        const comment = this.comments.find(c => c.id === commentId)
        if (comment) {
          comment.likes = (comment.likes || 0) + 1
          comment.isLiked = true
        }
        
        return true
      } catch (error) {
        console.error('点赞评论失败:', error)
        this.error = error.response?.data?.message || '点赞评论失败'
        throw error
      } finally {
        this.loading = false
      }
    },
    
    // 取消点赞评论
    async unlikeComment(commentId) {
      try {
        this.loading = true
        
        await axios.post(`/api/comments/${commentId}/unlike`)
        
        // 更新本地评论的点赞数
        const comment = this.comments.find(c => c.id === commentId)
        if (comment) {
          comment.likes = Math.max((comment.likes || 0) - 1, 0)
          comment.isLiked = false
        }
        
        return true
      } catch (error) {
        console.error('取消点赞评论失败:', error)
        this.error = error.response?.data?.message || '取消点赞评论失败'
        throw error
      } finally {
        this.loading = false
      }
    },
    
    // 切换评论点赞状态
    async toggleLike(commentId, isLiked) {
      if (isLiked) {
        return this.unlikeComment(commentId)
      } else {
        return this.likeComment(commentId)
      }
    },
    
    // 回复评论
    async replyToComment(blogId, parentId, content) {
      try {
        this.submitting = true
        console.log('回复评论，父评论ID:', parentId, '，内容:', content)
        
        // 获取当前用户信息和认证令牌
        const userStore = useUserStore()
        if (!userStore.isLoggedIn) {
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
        
        const response = await axios.post('/api/comments/reply', {
          blogId: blogId,
          parentId: parentId,
          content: content
        }, {
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