import { defineStore } from 'pinia'
import axios from 'axios'

export const useCategoryStore = defineStore('category', {
  state: () => ({
    categories: [],
    loading: false,
    error: null
  }),

  getters: {
    allCategories: (state) => state.categories,
    categoryById: (state) => (id) => state.categories.find(category => category.id === id)
  },

  actions: {
    async fetchCategories() {
      try {
        this.loading = true
        const response = await axios.get('/api/auth/categories')
        console.log('获取分类成功:', response.data)
        this.categories = response.data
      } catch (error) {
        console.error('获取分类失败:', error)
        this.error = error.response?.data?.message || '获取分类列表失败'
        throw error
      } finally {
        this.loading = false
      }
    },

    async fetchCategoryById(id) {
      try {
        this.loading = true
        const response = await axios.get(`/api/auth/categories/${id}`)
        const index = this.categories.findIndex(category => category.id === id)
        if (index !== -1) {
          this.categories[index] = response.data
        } else {
          this.categories.push(response.data)
        }
        return response.data
      } catch (error) {
        this.error = error.response?.data?.message || '获取分类详情失败'
        throw error
      } finally {
        this.loading = false
      }
    },

    async createCategory(categoryData) {
      try {
        this.loading = true
        const response = await axios.post('/api/auth/categories', categoryData)
        this.categories.push(response.data)
        return response.data
      } catch (error) {
        this.error = error.response?.data?.message || '创建分类失败'
        throw error
      } finally {
        this.loading = false
      }
    },

    async updateCategory(id, categoryData) {
      try {
        this.loading = true
        const response = await axios.put(`/api/auth/categories/${id}`, categoryData)
        const index = this.categories.findIndex(category => category.id === id)
        if (index !== -1) {
          this.categories[index] = response.data
        }
        return response.data
      } catch (error) {
        this.error = error.response?.data?.message || '更新分类失败'
        throw error
      } finally {
        this.loading = false
      }
    },

    async deleteCategory(id) {
      try {
        this.loading = true
        await axios.delete(`/api/auth/categories/${id}`)
        this.categories = this.categories.filter(category => category.id !== id)
      } catch (error) {
        this.error = error.response?.data?.message || '删除分类失败'
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