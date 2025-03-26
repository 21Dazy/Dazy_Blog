import { defineStore } from 'pinia'
import axios from 'axios'

export const useTagStore = defineStore('tag', {
  state: () => ({
    tags: [],
    currentTag: null,
    loading: false,
    error: null,
  }),

  getters: {
    allTags: (state) => state.tags,
    tagById: (state) => (id) => state.tags.find(tag => tag.id === id),
    popularTags: (state) => [...state.tags].sort((a, b) => (b.count || 0) - (a.count || 0)).slice(0, 10)
  },

  actions: {
    async fetchTags() {
      try {
        this.loading = true
        console.log('开始获取标签列表...')
        const response = await axios.get('/api/tags')
        console.log('标签列表获取成功:', response.data)
        this.tags = response.data
        return this.tags
      } catch (error) {
        console.error('获取标签列表失败:', error)
        console.error('错误详情:', error.response?.data)
        this.error = error.response?.data?.message || '获取标签列表失败'
        throw error
      } finally {
        this.loading = false
      }
    },

    async fetchTagById(id) {
      try {
        this.loading = true
        const response = await axios.get(`/api/tags/${id}`)
        this.currentTag = response.data
        
        // 更新标签列表中的对应标签
        const index = this.tags.findIndex(tag => tag.id === id)
        if (index !== -1) {
          this.tags[index] = response.data
        } else {
          this.tags.push(response.data)
        }
        
        return response.data
      } catch (error) {
        this.error = error.response?.data?.message || '获取标签详情失败'
        throw error
      } finally {
        this.loading = false
      }
    },

    async createTag(tagData) {
      try {
        this.loading = true
        const response = await axios.post('/api/tags', tagData)
        // 确保新标签被添加到标签列表中
        this.tags.push(response.data)
        return response.data
      } catch (error) {
        this.error = error.response?.data?.message || '创建标签失败'
        throw error
      } finally {
        this.loading = false
      }
    },
    
    // 处理批量创建标签，返回带有ID的标签对象数组
    async createTagsIfNeeded(tagList) {
      try {
        this.loading = true
        const result = []
        
        for (const tag of tagList) {
          if (typeof tag === 'string') {
            // 检查是否已存在同名标签
            const existingTag = this.tags.find(t => t.name.toLowerCase() === tag.toLowerCase())
            if (existingTag) {
              result.push(existingTag)
            } else {
              // 创建新标签
              const newTag = await this.createTag({ name: tag })
              result.push(newTag)
            }
          } else if (tag.id) {
            // 已存在的标签ID
            result.push(tag)
          } else if (tag.name) {
            // 只有名称的标签对象
            const existingTag = this.tags.find(t => t.name.toLowerCase() === tag.name.toLowerCase())
            if (existingTag) {
              result.push(existingTag)
            } else {
              const newTag = await this.createTag({ name: tag.name })
              result.push(newTag)
            }
          }
        }
        
        return result
      } catch (error) {
        this.error = error.response?.data?.message || '处理标签失败'
        throw error
      } finally {
        this.loading = false
      }
    },

    async deleteTag(id) {
      try {
        this.loading = true
        await axios.delete(`/api/tags/${id}`)
        this.tags = this.tags.filter(tag => tag.id !== id)
        if (this.currentTag?.id === id) {
          this.currentTag = null
        }
      } catch (error) {
        this.error = error.response?.data?.message || '删除标签失败'
        throw error
      } finally {
        this.loading = false
      }
    },

    clearError() {
      this.error = null
    }
  }
}) 