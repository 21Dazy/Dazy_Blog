<template>
  <div class="write-blog-container">
    <div class="write-blog-header">
      <h2>写博客</h2>
    </div>
    
    <el-form 
      ref="blogFormRef" 
      :model="formData" 
      :rules="rules" 
      label-width="100px"
      class="blog-form"
    >
      <el-form-item label="标题" prop="title">
        <el-input v-model="formData.title" placeholder="请输入博客标题"></el-input>
      </el-form-item>
      
      <el-form-item label="分类" prop="categoryId">
        <el-select v-model="formData.categoryId" placeholder="请选择分类">
          <el-option 
            v-for="category in categories" 
            :key="category.id" 
            :label="category.name" 
            :value="category.id"
          ></el-option>
        </el-select>
      </el-form-item>
      
      <el-form-item label="标签" prop="tags">
        <el-select
          v-model="formData.tags"
          multiple
          filterable
          allow-create
          default-first-option
          placeholder="请选择标签（可自定义）"
        >
          <el-option
            v-for="tag in tags"
            :key="tag.id"
            :label="tag.name"
            :value="tag.id"
          ></el-option>
        </el-select>
      </el-form-item>
      
      <el-form-item label="摘要" prop="summary">
        <el-input 
          v-model="formData.summary" 
          type="textarea" 
          :rows="3" 
          placeholder="请输入博客摘要"
        ></el-input>
      </el-form-item>
      
      <el-form-item label="内容" prop="content">
        <div class="editor-container">
          <!-- 使用Markdown编辑器替代普通文本域 -->
          <MarkdownEditor 
            v-model:value="formData.content" 
            :upload-headers="uploadHeaders"
            upload-url="/api/blogs/upload"
            @upload-success="handleImageSuccess"
            @upload-error="handleUploadError"
          />
        </div>
      </el-form-item>
      
      <el-form-item label="封面图" prop="coverImage">
        <el-upload
          class="cover-uploader"
          action="/api/blogs/upload"
          :headers="uploadHeaders"
          :show-file-list="false"
          :on-success="handleCoverSuccess"
          :on-error="handleUploadError"
          :before-upload="beforeCoverUpload"
        >
          <img v-if="formData.coverImage" :src="getImageUrl(formData.coverImage)" class="cover-image">
          <el-icon v-else class="cover-uploader-icon"><i-ep-plus /></el-icon>
        </el-upload> 
        <el-dialog>
          
        </el-dialog>
      </el-form-item>
      
      <el-form-item label="状态" prop="status">
        <el-radio-group v-model="formData.status">
          <el-radio :label="'PUBLISHED'">发布</el-radio>
          <el-radio :label="'DRAFT'">草稿</el-radio>
        </el-radio-group>
      </el-form-item>
      
      <el-form-item>
        <el-button type="primary" @click="submitForm" :loading="loading">提交</el-button>
        <el-button @click="resetForm">重置</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
import { ref, reactive, onMounted, computed } from 'vue'
import { useBlogStore } from '@/stores/blog'
import { useCategoryStore } from '@/stores/category'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import MarkdownEditor from '@/components/MarkdownEditor.vue'

import axios from 'axios'

export default {
  name: 'WriteBlog',
  components: {
    MarkdownEditor
  },
  setup() {
    const blogStore = useBlogStore()
    const categoryStore = useCategoryStore()
    const router = useRouter()
    const blogFormRef = ref(null)
    const loading = computed(() => blogStore.loading)
    const categories = computed(() => categoryStore.categories)
    const tags = ref([])
    
    // 添加上传头信息，包含token
    const uploadHeaders = computed(() => {
      return {
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      }
    })
    
    const formData = ref({
      title: '',
      categoryId: '',
      tags: [],
      summary: '',
      content: '',
      coverImage: '',
      status: 'PUBLISHED'
    })
    
    const rules = {
      title: [
        { required: true, message: '请输入博客标题', trigger: 'blur' },
        { min: 3, max: 100, message: '标题长度应为3-100个字符', trigger: 'blur' }
      ],
      categoryId: [
        { required: true, message: '请选择分类', trigger: 'change' }
      ],
      summary: [
        { required: true, message: '请输入博客摘要', trigger: 'blur' },
        { max: 200, message: '摘要最多200个字符', trigger: 'blur' }
      ],
      content: [
        { required: true, message: '请输入博客内容', trigger: 'blur' }
      ]
    }
    
    onMounted(async () => {
      try {
        // 获取分类列表
        await categoryStore.fetchCategories()
        
        // 获取标签列表（假设有一个标签API）
        // const tagResponse = await axios.get('/api/tags')
        // tags.value = tagResponse.data
      } catch (error) {
        console.error('加载数据失败:', error)
        ElMessage.error('加载分类和标签失败')
      }
    })
    
    const handleCoverSuccess = (res, file) => {
      // 确保响应包含正确的URL格式
      console.log('上传成功，返回结果:', res)
      if (res.data && res.data.url) {
        formData.value.coverImage = res.data.url
      } else if (res.url) {
        formData.value.coverImage = res.url
      } else {
        ElMessage.error('图片上传成功，但未获取到正确的URL')
      }
    }
    
    const handleUploadError = (err, file) => {
      console.error('上传图片失败:', err);
      let errorMessage = '上传图片失败，请稍后重试';
      
      // 尝试获取更详细的错误信息
      if (err.response) {
        console.error('错误响应:', err.response);
        try {
          const responseData = err.response.data;
          if (typeof responseData === 'string') {
            errorMessage = responseData;
          } else if (responseData && responseData.message) {
            errorMessage = responseData.message;
          } else if (err.response.status) {
            errorMessage = `错误状态码: ${err.response.status}`;
          }
        } catch (parseError) {
          console.error('解析错误响应失败:', parseError);
        }
      }
      
      ElMessage.error(errorMessage);
    }
    
    const beforeCoverUpload = (file) => {
      const isImage = file.type.startsWith('image/')
      const isLt2M = file.size / 1024 / 1024 < 2
      
      if (!isImage) {
        ElMessage.error('上传封面图片只能是图片格式!')
      }
      if (!isLt2M) {
        ElMessage.error('上传封面图片大小不能超过 2MB!')
      }
      
      return isImage && isLt2M
    }
    
    const handleImageSuccess = (res) => {
      console.log('图片上传成功，返回结果:', res)
      let imageUrl = ''
      
      // 处理不同的返回格式
      if (res.data && res.data.url) {
        imageUrl = getImageUrl(res.data.url)
      } else if (res.url) {
        imageUrl = getImageUrl(res.url)
      } else {
        console.error('未找到有效的图片URL', res)
        return '![图片上传失败]()'
      }
      
      console.log('生成Markdown图片链接:', `![图片](${imageUrl})`)
      // 返回markdown格式的图片链接，可以直接插入到编辑器中
      return `![图片](${imageUrl})`
    }
    
    const submitForm = async () => {
      try {
        await blogFormRef.value.validate()//作用是触发表单验证
        console.log('formData:', formData.value)
        
        // 处理标签数据
        const tagData = formData.value.tags.map(tag => {
          if (typeof tag === 'string') {
            return { name: tag }
          } else {
            return { id: tag }
          }
        })
        
        const blogData = {
          title: formData.value.title,
          categoryId: formData.value.categoryId,
          tags: tagData,
          summary: formData.value.summary,
          content: formData.value.content,
          coverImage: formData.value.coverImage,
          status: formData.value.status
        }
        
        const response = await blogStore.createBlog(blogData)
        
        ElMessage.success('博客发布成功')
        router.push(`/blog/${response.id}`)
      } catch (error) {
        console.error('发布博客失败:', error)
        ElMessage.error(error.response?.data?.message || '发布博客失败，请稍后再试')
      }
    }
    
    const resetForm = () => {
      blogFormRef.value.resetFields()
    }

    const getImageUrl = (url) => {
      console.log('处理图片URL:', url)
      if (!url) return ''

      // 如果URL已经是完整路径，直接返回
      if (url.startsWith('http')) {
        console.log('返回完整URL:', url)
        return url
      }
      
      // 检查url是否不以/开头，则添加/
      if (!url.startsWith('/')) {
        url = '/' + url
      }
      
      // 确保URL包含/api前缀
      if (url.startsWith('/uploads')) {
        url = '/api' + url
      }
      
      // 拼接完整URL
      const fullUrl = `http://localhost:8080${url}`
      console.log('返回拼接URL:', fullUrl)
      return fullUrl
    }
    
    return {
      blogFormRef,
      formData,
      rules,
      loading,
      categories,
      tags,
      uploadHeaders,
      handleCoverSuccess,
      handleUploadError,
      beforeCoverUpload,
      handleImageSuccess,
      submitForm,
      resetForm,
      getImageUrl
    }
  }
}
</script>

<style scoped>
.write-blog-container {
  padding: 20px;
  background-color: #fff;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.write-blog-header {
  margin-bottom: 20px;
  border-bottom: 1px solid #eee;
  padding-bottom: 10px;
}

.write-blog-header h2 {
  margin: 0;
}

.blog-form {
  max-width: 900px;
}

.editor-container {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  min-height: 500px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.cover-uploader {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  width: 178px;
  height: 178px;
  display: flex;
  justify-content: center;
  align-items: center;
}

.cover-uploader:hover {
  border-color: #409EFF;
}

.cover-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 178px;
  height: 178px;
  line-height: 178px;
  text-align: center;
}

.cover-image {
  width: 178px;
  height: 178px;
  display: block;
  object-fit: cover;
}
</style> 