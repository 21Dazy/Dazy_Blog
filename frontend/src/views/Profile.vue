<template>
  <div class="profile-container">
    <div class="profile-header">
      <h2>个人中心</h2>
    </div>
    
    <el-tabs v-model="activeTab">
      <el-tab-pane label="个人资料" name="profile">
        <el-form 
          ref="profileFormRef" 
          :model="profileData" 
          :rules="profileRules" 
          label-width="100px"
          class="profile-form"
        >
          <el-form-item label="用户名" prop="username">
            <el-input v-model="profileData.username" disabled></el-input>
          </el-form-item>
          
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="profileData.email"></el-input>
          </el-form-item>
          
          <el-form-item label="头像">
            <el-upload
              class="avatar-uploader"
              action="http://localhost:8080/api/avatar/upload"
              :headers="uploadHeaders"
              :show-file-list="false"
              :on-success="handleAvatarSuccess"
              :on-error="handleAvatarUploadError"
              :before-upload="beforeAvatarUpload"
            >
              <img v-if="profileData.avatar" :src="getImageUrl(profileData.avatar)" class="avatar">
              <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
            </el-upload>
          </el-form-item>
          
          <el-form-item label="个人简介" prop="bio">
            <el-input 
              v-model="profileData.bio" 
              type="textarea" 
              :rows="4"
              placeholder="请输入个人简介"
            ></el-input>
          </el-form-item>
          
          <el-form-item>
            <el-button type="primary" @click="updateProfile" :loading="loading">保存</el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>
      
      <el-tab-pane label="修改密码" name="password">
        <el-form 
          ref="passwordFormRef" 
          :model="passwordData" 
          :rules="passwordRules" 
          label-width="100px"
          class="password-form"
        >
          <el-form-item label="当前密码" prop="currentPassword">
            <el-input 
              v-model="passwordData.currentPassword" 
              type="password" 
              show-password
              placeholder="请输入当前密码"
            ></el-input>
          </el-form-item>
          
          <el-form-item label="新密码" prop="newPassword">
            <el-input 
              v-model="passwordData.newPassword" 
              type="password" 
              show-password
              placeholder="请输入新密码"
            ></el-input>
          </el-form-item>
          
          <el-form-item label="确认新密码" prop="confirmPassword">
            <el-input 
              v-model="passwordData.confirmPassword" 
              type="password" 
              show-password
              placeholder="请再次输入新密码"
            ></el-input>
          </el-form-item>
          
          <el-form-item>
            <el-button type="primary" @click="changePassword" :loading="passwordLoading">修改密码</el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>
      
      <el-tab-pane label="我的博客" name="blogs">
        <div class="my-blogs">
          <el-button type="primary" @click="$router.push('/write')" class="write-button">写博客</el-button>
          <el-button type="info" @click="fetchUserBlogs" icon="Refresh">刷新</el-button>
          
          <el-table
            v-loading="blogsLoading"
            :data="blogs"
            style="width: 100%"
          >
            <el-table-column prop="title" label="标题" min-width="200">
              <template #default="scope">
                <router-link :to="'/blog/' + scope.row.id">{{ scope.row.title }}</router-link>
              </template>
            </el-table-column>
            <el-table-column label="分类" width="120">
              <template #default="scope">
                {{ scope.row.category ? scope.row.category.name : '未分类' }}
              </template>
            </el-table-column>
            <el-table-column prop="views" label="阅读数" width="100"></el-table-column>
            <el-table-column label="发布时间" width="180">
              <template #default="scope">
                {{ formatDate(scope.row.createdAt) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150">
              <template #default="scope">
                <el-button 
                  size="small" 
                  type="primary" 
                  @click="$router.push('/edit/' + scope.row.id)"
                >编辑</el-button>
                <el-button 
                  size="small" 
                  type="danger" 
                  @click="confirmDeleteBlog(scope.row)"
                >删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          
          <el-pagination
            v-if="totalBlogs > 0"
            layout="prev, pager, next"
            :total="totalBlogs"
            :page-size="pageSize"
            :current-page="currentPage"
            @current-change="handlePageChange"
            class="pagination"
          />
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
import { ref, reactive, onMounted, computed } from 'vue'
import { useUserStore } from '@/stores/user'
import { useBlogStore } from '@/stores/blog'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

export default {
  name: 'Profile',
  components: {
    Plus
  },
  setup() {
    const userStore = useUserStore()
    const blogStore = useBlogStore()
    const activeTab = ref('profile')
    const profileFormRef = ref(null)
    const passwordFormRef = ref(null)
    const loading = computed(() => userStore.loading)
    const passwordLoading = ref(false)
    const blogsLoading = computed(() => blogStore.loading)
    const blogs = computed(() => blogStore.blogs)
    const totalBlogs = ref(0)
    const currentPage = ref(1)
    const pageSize = ref(10)
    
    const userId = computed(() => userStore.user?.id)
    
    const uploadHeaders = computed(() => {
      return {
        Authorization: `Bearer ${userStore.token}`
      }
    })
    
    const profileData = reactive({
      username: '',
      email: '',
      avatar: '',
      bio: ''
    })
    
    const passwordData = reactive({
      currentPassword: '',
      newPassword: '',
      confirmPassword: ''
    })
    
    const profileRules = {
      email: [
        { required: true, message: '请输入邮箱地址', trigger: 'blur' },
        { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
      ],
      bio: [
        { max: 200, message: '个人简介最多200个字符', trigger: 'blur' }
      ]
    }
    
    const validatePass = (rule, value, callback) => {
      if (value === '') {
        callback(new Error('请输入新密码'))
      } else {
        if (passwordData.confirmPassword !== '') {
          passwordFormRef.value.validateField('confirmPassword')
        }
        callback()
      }
    }
    
    const validatePass2 = (rule, value, callback) => {
      if (value === '') {
        callback(new Error('请再次输入新密码'))
      } else if (value !== passwordData.newPassword) {
        callback(new Error('两次输入密码不一致'))
      } else {
        callback()
      }
    }
    
    const passwordRules = {
      currentPassword: [
        { required: true, message: '请输入当前密码', trigger: 'blur' }
      ],
      newPassword: [
        { required: true, message: '请输入新密码', trigger: 'blur' },
        { min: 6, max: 20, message: '密码长度应为6-20个字符', trigger: 'blur' },
        { validator: validatePass, trigger: 'blur' }
      ],
      confirmPassword: [
        { required: true, message: '请再次输入新密码', trigger: 'blur' },
        { validator: validatePass2, trigger: 'blur' }
      ]
    }
    
    onMounted(async () => {
      try {
        // 获取用户信息
        const userResponse = await userStore.fetchUserProfile()
        if (userResponse) {
          profileData.username = userResponse.username
          profileData.email = userResponse.email
          profileData.avatar = userResponse.avatar
          profileData.bio = userResponse.bio
          
          // 获取用户的博客
          await fetchUserBlogs()
        }
      } catch (error) {
        console.error('获取用户信息失败:', error)
        ElMessage.error('获取用户信息失败')
      }
    })
    
    const fetchUserBlogs = async () => {
      if (!userId.value) return
      
      try {
        blogsLoading.value = true;
        const response = await blogStore.fetchUserBlogs({
          userId: userId.value,
          page: currentPage.value,
          size: pageSize.value
        })
        
        // 重置博客列表数据
        blogs.value = blogStore.blogs;
        
        if (response && response.totalElements) {
          totalBlogs.value = response.totalElements
        }
      } catch (error) {
        console.error('获取用户博客失败:', error)
        ElMessage.error('获取用户博客失败')
      } finally {
        blogsLoading.value = false;
      }
    }
    
    const updateProfile = async () => {
      if (!profileFormRef.value) return
      
      try {
        await profileFormRef.value.validate()
        
        await userStore.updateProfile({
          email: profileData.email,
          avatar: profileData.avatar,
          bio: profileData.bio
        })
        
        ElMessage.success('个人资料更新成功')
      } catch (error) {
        console.error('更新个人资料失败:', error)
        ElMessage.error(error.response?.data?.message || '更新个人资料失败')
      }
    }
    
    const changePassword = async () => {
      if (!passwordFormRef.value) return
      
      try {
        await passwordFormRef.value.validate()
        
        await userStore.changePassword({
          currentPassword: passwordData.currentPassword,
          newPassword: passwordData.newPassword
        })
        
        ElMessage.success('密码修改成功')
        passwordData.currentPassword = ''
        passwordData.newPassword = ''
        passwordData.confirmPassword = ''
        passwordFormRef.value.resetFields()
      } catch (error) {
        console.error('修改密码失败:', error)
        ElMessage.error(error.response?.data?.message || '修改密码失败')
      }
    }
    
    const handleAvatarSuccess = (res) => {
      console.log('上传成功，返回结果:', res)
      // 确保我们得到正确的URL
      if (res && res.url) {
        profileData.avatar = res.url
        // 自动保存更新的头像
        userStore.updateProfile({
          email: profileData.email,
          avatar: profileData.avatar,
          bio: profileData.bio
        }).then(() => {
          ElMessage.success('头像更新成功')
        }).catch(error => {
          console.error('保存头像失败:', error)
        })
      } else {
        ElMessage.error('上传成功但未返回有效的URL')
      }
    }

    const handleAvatarUploadError = (err, file) => {
      console.error('上传头像失败:', err);
      let errorMessage = '上传头像失败，请稍后重试';
      
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
    
    const beforeAvatarUpload = (file) => {
      const isImage = file.type.startsWith('image/')
      const isLt2M = file.size / 1024 / 1024 < 2
      
      if (!isImage) {
        ElMessage.error('上传头像只能是图片格式!')
      }
      if (!isLt2M) {
        ElMessage.error('上传头像图片大小不能超过 2MB!')
      }
      
      return isImage && isLt2M
    }
    
    const confirmDeleteBlog = (blog) => {
      ElMessageBox.confirm(
        '确定要删除这篇博客吗？此操作不可恢复。',
        '警告',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }
      ).then(async () => {
        try {
          await blogStore.deleteBlog(blog.id)
          ElMessage.success('博客删除成功')
          fetchUserBlogs()
        } catch (error) {
          console.error('删除博客失败:', error)
          ElMessage.error('删除博客失败')
        }
      }).catch(() => {})
    }
    
    const handlePageChange = (page) => {
      currentPage.value = page
      fetchUserBlogs()
    }
    
    const formatDate = (dateArray) => {
      if (!dateArray) return '未知日期'
      
      try {
        // 尝试直接解析日期字符串
        const date = new Date(dateArray[0], dateArray[1] - 1, dateArray[2], dateArray[3], dateArray[4], dateArray[5])
  
        // 检查日期是否有效
        if (isNaN(date.getTime())) {
          console.error('无效的日期值:', dateArray)
          return '无效日期'
        }
        
        return date.toLocaleDateString('zh-CN', {
          year: 'numeric',
          month: 'long',
          day: 'numeric',
          hour: '2-digit',
          minute: '2-digit'
        })
      } catch (error) {
        console.error('日期格式化错误:', error, '日期值:', dateArray)
        return '无效日期'
      }
    }
    
    // 处理图片URL
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
      activeTab,
      profileFormRef,
      passwordFormRef,
      profileData,
      passwordData,
      profileRules,
      passwordRules,
      loading,
      passwordLoading,
      blogsLoading,
      blogs,
      totalBlogs,
      currentPage,
      pageSize,
      uploadHeaders,
      updateProfile,
      changePassword,
      handleAvatarSuccess,
      handleAvatarUploadError,
      beforeAvatarUpload,
      confirmDeleteBlog,
      handlePageChange,
      formatDate,
      getImageUrl
    }
  }
}
</script>

<style scoped>
.profile-container {
  padding: 20px;
  background-color: #fff;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.profile-header {
  margin-bottom: 20px;
  border-bottom: 1px solid #eee;
  padding-bottom: 10px;
}

.profile-header h2 {
  margin: 0;
}

.profile-form, .password-form {
  max-width: 500px;
  margin-top: 20px;
}

.avatar-uploader {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  width: 100px;
  height: 100px;
  display: flex;
  justify-content: center;
  align-items: center;
}

.avatar-uploader:hover {
  border-color: #409EFF;
}

.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 100px;
  height: 100px;
  line-height: 100px;
  text-align: center;
}

.avatar {
  width: 100px;
  height: 100px;
  display: block;
  object-fit: cover;
}

.my-blogs {
  margin-top: 20px;
}

.write-button {
  margin-bottom: 20px;
}

.pagination {
  margin-top: 20px;
  text-align: center;
}
</style> 