<template>
  <div class="edit-blog-container">
    <div class="edit-blog-header">
      <h2>编辑博客</h2>
    </div>
    
    <el-skeleton :rows="10" animated v-if="loading" />
    
    <el-form 
      v-else
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
          :before-upload="beforeCoverUpload"
          :on-error="handleUploadError"
        >
          <img v-if="formData.coverImage" :src="getImageUrl(formData.coverImage)" class="cover-image">
          <el-icon v-else class="cover-uploader-icon"><i-ep-plus /></el-icon>
        </el-upload>
      </el-form-item>
      
      <el-form-item label="状态" prop="status">
        <el-radio-group v-model="formData.status">
          <el-radio :label="'PUBLISHED'">发布</el-radio>
          <el-radio :label="'DRAFT'">草稿</el-radio>
        </el-radio-group>
      </el-form-item>
      
      <el-form-item>
        <el-button type="primary" @click="submitForm" :loading="submitting">保存</el-button>
        <el-button @click="$router.push('/blog/' + blogId)">取消</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
import { ref, reactive, onMounted, computed } from 'vue'
import { useBlogStore } from '@/stores/blog'
import { useCategoryStore } from '@/stores/category'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import MarkdownEditor from '@/components/MarkdownEditor.vue'

export default {
  name: 'EditBlog',
  components: {
    MarkdownEditor
  },
  setup() {
    const blogStore = useBlogStore()
    const categoryStore = useCategoryStore()
    const router = useRouter()
    const route = useRoute()
    const blogFormRef = ref(null)
    const loading = ref(true)
    const submitting = computed(() => blogStore.loading)
    const categories = computed(() => categoryStore.categories)
    const tags = ref([])
    
    const blogId = computed(() => route.params.id)
    
    // 添加上传头信息，包含token
    const uploadHeaders = computed(() => {
      return {
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      }
    })
    
    const formData = reactive({
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
        console.log('开始加载博客数据，博客ID:', blogId.value);
        
        // 获取分类列表
        await categoryStore.fetchCategories();
        console.log('分类列表加载成功:', categories.value);
        
        // 获取标签列表（假设有一个标签API）
        // const tagResponse = await axios.get('/api/tags')
        // tags.value = tagResponse.data
        
        // 获取博客详情
        console.log('开始请求博客详情，URL:', `/api/blogs/${blogId.value}`);
        const blogData = await blogStore.fetchBlogById(blogId.value);
        console.log('获取到的博客数据:', blogData);
        
        // 填充表单数据
        formData.title = blogData.title;
        formData.categoryId = blogData.category ? blogData.category.id : '';
        formData.tags = blogData.tags ? blogData.tags.map(tag => tag.id) : [];
        formData.summary = blogData.summary;
        formData.content = blogData.content;
        formData.coverImage = blogData.coverImage;
        formData.status = blogData.status || 'PUBLISHED';
        
        console.log('表单数据填充完成');
        loading.value = false;
      } catch (error) {
        console.error('加载数据失败:', error);
        console.error('错误详情:', error.response ? error.response.data : '无响应数据');
        console.error('状态码:', error.response ? error.response.status : '无状态码');
        ElMessage.error('加载博客数据失败');
        router.push('/');
      }
    })
    
    // 处理封面图片上传成功
    const handleCoverSuccess = (res) => {
      formData.coverImage = res.url
    }
    
    // 处理Markdown编辑器中图片上传成功
    const handleImageSuccess = (res) => {
      // 返回markdown格式的图片链接，可以直接插入到编辑器中
      return `![图片](${getImageUrl(res.url)})`
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
    
    const submitForm = async () => {
      try {
        await blogFormRef.value.validate()
        
        const blogData = {
          title: formData.title,
          categoryId: formData.categoryId,
          tags: formData.tags.map(tag => {
            if (typeof tag === 'string') {
              return { name: tag }
            } else {
              return { id: tag }
            }
          }),
          summary: formData.summary,
          content: formData.content,
          coverImage: formData.coverImage,
          status: formData.status
        }
        
        await blogStore.updateBlog(blogId.value, blogData)
        
        ElMessage.success('博客更新成功')
        router.push(`/blog/${blogId.value}`)
      } catch (error) {
        console.error('更新博客失败:', error)
        ElMessage.error(error.response?.data?.message || '更新博客失败，请稍后再试')
      }
    }
    
    const getImageUrl = (url) => {
      if (!url) return ''
      
      // 如果URL已经是完整路径，直接返回
      if (url.startsWith('http')) {
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
      return `http://localhost:8080${url}`
    }
    
    return {
      blogFormRef,
      formData,
      rules,
      loading,
      submitting,
      categories,
      tags,
      blogId,
      uploadHeaders,
      handleCoverSuccess,
      handleImageSuccess,
      beforeCoverUpload,
      handleUploadError,
      submitForm,
      getImageUrl
    }
  }
}
</script>

<style scoped>
.edit-blog-container {
  padding: 20px;
  background-color: #fff;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.edit-blog-header {
  margin-bottom: 20px;
  border-bottom: 1px solid #eee;
  padding-bottom: 10px;
}

.edit-blog-header h2 {
  margin: 0;
}

.blog-form {
  max-width: 900px;
}

.editor-container {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  min-height: 400px;
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