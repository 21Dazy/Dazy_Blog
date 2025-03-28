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
        
        console.log('获取博客评论，参数:', { blogId, params })
        
        const response = await axios.get(`/api/comments/blog/${blogId}`, {
          params: {
            page: params.page - 1, // 后端分页从0开始
            size: params.size,
            rootOnly: params.rootOnly
          }
        })
        
        console.log('后端返回的评论数据:', response.data)
        
        let comments = []
        let total = 0
        
        if (response.data.content) {
          // 分页响应格式
          comments = response.data.content
          total = response.data.totalElements
        } else if (Array.isArray(response.data)) {
          // 数组响应格式
          comments = response.data
          total = response.data.length
        } else {
          // 其他响应格式
          comments = response.data.comments || []
          total = response.data.total || comments.length
        }
        
        // 确保每个评论对象具有一致的数据结构
        comments = comments.map(comment => {
          // 统一parentId属性，确保前端使用parentId而不是parent_id
          if (comment.parent && comment.parent.id && !comment.parentId) {
            comment.parentId = comment.parent.id
            console.log(`评论ID ${comment.id} 设置parentId=${comment.parentId}`)
          } else if (comment.parent_id && !comment.parentId) {
            comment.parentId = comment.parent_id
            console.log(`评论ID ${comment.id} 从parent_id设置parentId=${comment.parentId}`)
          }
          
          return comment
        })
        
        console.log('处理后的评论数据:', comments)
        console.log('根评论数量:', comments.filter(c => !c.parentId).length)
        console.log('子评论数量:', comments.filter(c => c.parentId).length)
        
        // 更新状态
        this.comments = comments
        this.totalComments = total
        
        return {
          comments: comments,
          total: total
        }
      } catch (error) {
        console.error('获取评论失败:', error)
        this.error = error.response?.data?.message || '获取评论失败'
        return {
          comments: [],
          total: 0
        }
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
      try {
        // 根据当前点赞状态决定调用哪个方法
        if (isLiked) {
          await axios.post(`/api/comments/${commentId}/unlike`);
          console.log(`取消点赞评论 ID: ${commentId}`);
        } else {
          await axios.post(`/api/comments/${commentId}/like`);
          console.log(`点赞评论 ID: ${commentId}`);
        }
        return true;
      } catch (error) {
        console.error('切换点赞状态失败:', error);
        throw error;
      }
    },
    
    // 回复评论
    async replyToComment(blogId, parentId, content, replyToUsername = null) {
      console.log(`回复评论 - 博客ID: ${blogId}, 父评论ID: ${parentId}, 回复用户: ${replyToUsername}`);
      
      const userStore = useUserStore(); // 使用函数获取store
      
      if (!userStore.isAuthenticated) {
        throw new Error('请先登录后再回复评论');
      }

      const token = userStore.token;
      if (!token) {
        throw new Error('登录已过期，请重新登录');
      }

      // 获取根评论ID（如果是子评论的话）
      try {
        let params = {
          content,
          parentId
        };
        
        // 如果提供了回复用户名，添加到请求参数
        if (replyToUsername) {
          params.replyToUsername = replyToUsername;
        }
        
        const res = await axios.post(
          `/api/auth/comments/blog/${blogId}`, 
          null,
          { 
            params,
            headers: { Authorization: `Bearer ${token}` }
          }
        );
        
        // 确保parentId是一个数字类型
        if (res.data.parent && res.data.parent.id) {
          res.data.parentId = Number(res.data.parent.id);
        }
        
        // 添加到评论列表
        this.comments.push(res.data);
        this.totalComments++;
        
        return res.data;
      } catch (error) {
        console.error('回复评论失败:', error);
        throw error;
      }
    },
    
    // 清除错误
    clearError() {
      this.error = null
    },
    
    // 获取指定父评论的子评论
    async fetchChildComments(parentId) {
      try {
        this.loading = true
        this.error = null
        
        // 确保parentId是数字类型
        const numericParentId = Number(parentId);
        console.log('获取父评论的子评论，父评论ID:', numericParentId, '(', typeof numericParentId, ')');
        
        const response = await axios.get(`/api/comments/parent/${numericParentId}`);
        console.log('子评论API响应:', response.data);
        
        let childComments = response.data.content || response.data || [];
        console.log(`获取到 ${childComments.length} 条子评论`);
        
        // 处理子评论数据，确保ID和parentId字段为数字类型
        childComments = childComments.map(comment => {
          // 设置ID为数字类型
          if (comment.id !== undefined) {
            comment.id = Number(comment.id);
          }
          
          // 统一设置parentId为当前父评论ID
          comment.parentId = numericParentId;
          
          // 如果有parent对象，确保parent.id也是数字类型
          if (comment.parent && comment.parent.id !== undefined) {
            comment.parent.id = Number(comment.parent.id);
          }
          
          // 如果有parent_id，统一到parentId并删除parent_id
          if (comment.parent_id !== undefined) {
            comment.parentId = Number(comment.parent_id);
            delete comment.parent_id;
          }
          
          return comment;
        });
        
        console.log('处理后的子评论:', childComments);
        
        // 将获取的子评论添加到全局评论列表中
        // 先过滤掉已存在的子评论（基于ID）
        const existingIds = this.comments.map(c => Number(c.id));
        const newChildComments = childComments.filter(c => !existingIds.includes(Number(c.id)));
        
        if (newChildComments.length > 0) {
          console.log(`添加 ${newChildComments.length} 条新子评论到全局评论列表`);
          this.comments = [...this.comments, ...newChildComments];
        } else if (childComments.length > 0) {
          console.log('所有子评论已存在于全局评论列表中，更新现有评论');
          // 更新现有评论的父子关系
          this.comments = this.comments.map(comment => {
            const matchingChild = childComments.find(c => Number(c.id) === Number(comment.id));
            if (matchingChild) {
              console.log(`更新评论ID ${comment.id} 的parentId为 ${numericParentId}`);
              return { ...comment, parentId: numericParentId };
            }
            return comment;
          });
        }
        
        return childComments;
      } catch (error) {
        console.error('获取子评论失败:', error);
        this.error = error.response?.data?.message || '获取子评论失败';
        return [];
      } finally {
        this.loading = false;
      }
    }
  }
})