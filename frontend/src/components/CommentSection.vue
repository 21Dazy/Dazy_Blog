<template>
    <div class="comment-section">
      <h3>评论 ({{ totalComments }})</h3>
      
      <!-- 评论表单 -->
      <div class="comment-form">
        <el-avatar v-if="userAvatar" :src="userAvatar" :size="40"></el-avatar>
        <el-avatar v-else :size="40" icon="el-icon-user"></el-avatar>
        
        <el-form :model="commentForm" :rules="rules" ref="commentFormRef" @submit.prevent="submitComment">
          <el-form-item prop="content">
            <el-input 
              v-model="commentForm.content" 
              type="textarea" 
              :rows="3" 
              :placeholder="replyingTo ? `回复 @${replyingTo.username || '用户'}` : '发表评论...'"
            ></el-input>
          </el-form-item>
          
          <div v-if="!isLoggedIn" class="guest-info">
            <el-form-item prop="username">
              <el-input v-model="commentForm.username" placeholder="您的昵称（必填）"></el-input>
            </el-form-item>
            <el-form-item prop="email">
              <el-input v-model="commentForm.email" placeholder="您的邮箱（选填）"></el-input>
            </el-form-item>
          </div>
          
          <div class="form-actions">
            <el-button v-if="replyingTo" type="text" @click="cancelReply">取消回复</el-button>
            <el-button type="primary" @click="submitComment" :loading="submitting">{{ replyingTo ? '回复' : '提交评论' }}</el-button>
          </div>
        </el-form>
      </div>
      
      <!-- 评论列表 -->
      <div v-if="loading" class="comment-loading">
        <el-skeleton :rows="5" animated />
      </div>
      <div v-else-if="comments.length === 0" class="no-comments">
        暂无评论，快来发表第一条评论吧！
      </div>
      <div v-else class="comment-list">
        <div v-for="comment in comments" :key="comment.id" class="comment-item">
          <div class="comment-avatar">
            <el-avatar 
              :src="comment.userAvatar || comment.user?.avatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'" 
              :size="40"
            ></el-avatar>
          </div>
          <div class="comment-content">
            <div class="comment-header">
              <span class="comment-author">{{ comment.username || comment.user?.username || '匿名用户' }}</span>
              <span class="comment-time">{{ formatDate(comment.createdAt) }}</span>
            </div>
            <div class="comment-text">
              <span v-if="comment.replyToUsername" class="reply-to">回复 @{{ comment.replyToUsername }}：</span>
              {{ comment.content }}
            </div>
            <div class="comment-actions">
              <el-button type="text" @click="replyTo(comment)">回复</el-button>
              <el-button 
                v-if="canDelete(comment)" 
                type="text" 
                @click="deleteComment(comment.id)"
                class="delete-btn"
              >删除</el-button>
            </div>
            
            <!-- 回复列表 -->
            <div v-if="comment.replies && comment.replies.length > 0" class="replies">
              <div v-for="reply in comment.replies" :key="reply.id" class="reply-item">
                <div class="comment-avatar">
                  <el-avatar 
                    :src="reply.userAvatar || reply.user?.avatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'" 
                    :size="30"
                  ></el-avatar>
                </div>
                <div class="comment-content">
                  <div class="comment-header">
                    <span class="comment-author">{{ reply.username || reply.user?.username || '匿名用户' }}</span>
                    <span class="comment-time">{{ formatDate(reply.createdAt) }}</span>
                  </div>
                  <div class="comment-text">
                    <span v-if="reply.replyToUsername" class="reply-to">回复 @{{ reply.replyToUsername }}：</span>
                    {{ reply.content }}
                  </div>
                  <div class="comment-actions">
                    <el-button type="text" @click="replyTo(reply)">回复</el-button>
                    <el-button 
                      v-if="canDelete(reply)" 
                      type="text" 
                      @click="deleteComment(reply.id)"
                      class="delete-btn"
                    >删除</el-button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 分页 -->
      <div class="pagination" v-if="totalPages > 1">
        <el-pagination
          layout="prev, pager, next"
          :total="totalComments"
          :page-size="pageSize"
          :current-page="currentPage"
          @current-change="handlePageChange"
        ></el-pagination>
      </div>
    </div>
  </template>
  
  <script>
  import { ref, reactive, computed, onMounted } from 'vue'
  import { useUserStore } from '@/stores/user'
  import axios from 'axios'
  import { ElMessage, ElMessageBox } from 'element-plus'
  
  export default {
    name: 'CommentSection',
    props: {
      blogId: {
        type: [Number, String],
        required: true
      }
    },
    setup(props) {
      const userStore = useUserStore()
      const commentFormRef = ref(null)
      const loading = ref(true)
      const submitting = ref(false)
      const comments = ref([])
      const totalComments = ref(0)
      const totalPages = ref(0)
      const currentPage = ref(1)
      const pageSize = ref(10)
      const replyingTo = ref(null)
      
      // 检查用户是否登录
      const isLoggedIn = computed(() => userStore.isLoggedIn)
      
      // 获取用户头像
      const userAvatar = computed(() => {
        if (isLoggedIn.value) {
          return userStore.user?.avatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
        }
        return null
      })
      
      // 评论表单数据
      const commentForm = reactive({
        content: '',
        username: localStorage.getItem('guestName') || '',
        email: localStorage.getItem('guestEmail') || '',
        parentCommentId: null
      })
      
      // 表单验证规则
      const rules = {
        content: [
          { required: true, message: '请输入评论内容', trigger: 'blur' },
          { min: 2, max: 500, message: '评论内容长度应在2-500个字符之间', trigger: 'blur' }
        ],
        username: [
          { required: true, message: '请输入您的昵称', trigger: 'blur' }
        ]
      }
      
      // 加载评论
      const fetchComments = async () => {
        loading.value = true
        try {
          const response = await axios.get(`/api/blogs/${props.blogId}/comments`, {
            params: {
              page: currentPage.value - 1, // 后端是0-based索引
              size: pageSize.value
            }
          })
          
          comments.value = response.data.comments || []
          totalComments.value = response.data.totalItems || 0
          totalPages.value = response.data.totalPages || 0
          
        } catch (error) {
          console.error('获取评论失败:', error)
          ElMessage.error('获取评论失败，请稍后重试')
        } finally {
          loading.value = false
        }
      }
      
      // 提交评论
      const submitComment = async () => {
        if (!commentForm.content.trim()) {
          ElMessage.warning('评论内容不能为空')
          return
        }
        
        if (!isLoggedIn.value && !commentForm.username.trim()) {
          ElMessage.warning('请输入您的昵称')
          return
        }
        
        // 验证表单
        try {
          // 先验证表单
          await commentFormRef.value.validate()
          
          submitting.value = true
          
          // 保存游客信息
          if (!isLoggedIn.value) {
            localStorage.setItem('guestName', commentForm.username)
            if (commentForm.email) {
              localStorage.setItem('guestEmail', commentForm.email)
            }
          }
          
          // 准备请求数据
          const commentData = {
            content: commentForm.content,
            parentCommentId: replyingTo.value ? replyingTo.value.id : null
          }
          
          // 如果是游客评论，添加用户名
          if (!isLoggedIn.value) {
            commentData.username = commentForm.username
            commentData.userAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png' // 默认头像
          }
          
          // 提交评论
          const headers = isLoggedIn.value ? {
            Authorization: `Bearer ${localStorage.getItem('token') || sessionStorage.getItem('token')}`
          } : {}
          
          const response = await axios.post(
            `/api/blogs/${props.blogId}/comments`, 
            commentData,
            { headers }
          )
          
          // 评论成功
          ElMessage.success(replyingTo.value ? '回复成功' : '评论成功')
          
          // 清空表单
          commentForm.content = ''
          if (replyingTo.value) {
            cancelReply()
          }
          
          // 刷新评论列表
          fetchComments()
          
        } catch (error) {
          console.error(replyingTo.value ? '回复失败:' : '评论失败:', error)
          ElMessage.error(replyingTo.value ? '回复失败，请稍后重试' : '评论失败，请稍后重试')
        } finally {
          submitting.value = false
        }
      }
      
      // 删除评论
      const deleteComment = async (commentId) => {
        try {
          await ElMessageBox.confirm('确定要删除这条评论吗？', '删除确认', {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          })
          
          await axios.delete(`/api/comments/${commentId}`, {
            headers: {
              Authorization: `Bearer ${localStorage.getItem('token') || sessionStorage.getItem('token')}`
            }
          })
          
          ElMessage.success('评论已删除')
          
          // 刷新评论列表
          fetchComments()
          
        } catch (error) {
          if (error !== 'cancel') {
            console.error('删除评论失败:', error)
            ElMessage.error('删除评论失败，请稍后重试')
          }
        }
      }
      
      // 回复评论
      const replyTo = (comment) => {
        replyingTo.value = comment
        commentForm.parentCommentId = comment.id
        
        // 聚焦到评论框
        setTimeout(() => {
          const textarea = document.querySelector('.comment-form textarea')
          if (textarea) {
            textarea.focus()
          }
        }, 100)
      }
      
      // 取消回复
      const cancelReply = () => {
        replyingTo.value = null
        commentForm.parentCommentId = null
      }
      
      // 判断是否可以删除评论
      const canDelete = (comment) => {
        if (!isLoggedIn.value) return false
        
        // 当前用户是评论作者
        const isAuthor = comment.user && comment.user.id === userStore.user?.id
        
        // 当前用户是博客作者(简化判断，实际应从博客数据中获取)
        const isBlogAuthor = userStore.user?.id === comment.blog?.author?.id
        
        // 管理员可以删除任何评论
        const isAdmin = userStore.user?.role === 'ADMIN'
        
        return isAuthor || isBlogAuthor || isAdmin
      }
      
      // 处理分页
      const handlePageChange = (page) => {
        currentPage.value = page
        fetchComments()
      }
      
      // 格式化日期
      const formatDate = (dateString) => {
        if (!dateString) return ''
        
        const date = new Date(dateString)
        return new Intl.DateTimeFormat('zh-CN', {
          year: 'numeric',
          month: 'numeric',
          day: 'numeric',
          hour: 'numeric',
          minute: 'numeric'
        }).format(date)
      }
      
      // 页面加载时获取评论
      onMounted(() => {
        fetchComments()
      })
      
      return {
        commentFormRef,
        loading,
        submitting,
        comments,
        totalComments,
        totalPages,
        currentPage,
        pageSize,
        commentForm,
        rules,
        isLoggedIn,
        userAvatar,
        replyingTo,
        fetchComments,
        submitComment,
        deleteComment,
        replyTo,
        cancelReply,
        canDelete,
        handlePageChange,
        formatDate
      }
    }
  }
  </script>
  
  <style scoped>
  .comment-section {
    margin-top: 30px;
    padding-top: 20px;
    border-top: 1px solid #ebeef5;
  }
  
  .comment-section h3 {
    margin-bottom: 20px;
    font-weight: 600;
    color: #303133;
  }
  
  .comment-form {
    display: flex;
    margin-bottom: 30px;
    gap: 15px;
  }
  
  .comment-form form {
    flex: 1;
  }
  
  .form-actions {
    display: flex;
    justify-content: flex-end;
    gap: 10px;
  }
  
  .guest-info {
    display: flex;
    gap: 15px;
    margin-bottom: 10px;
  }
  
  .guest-info .el-form-item {
    margin-bottom: 0;
    flex: 1;
  }
  
  .comment-list {
    margin-top: 20px;
  }
  
  .comment-item {
    display: flex;
    margin-bottom: 20px;
    gap: 15px;
  }
  
  .comment-content {
    flex: 1;
  }
  
  .comment-header {
    display: flex;
    align-items: center;
    margin-bottom: 5px;
  }
  
  .comment-author {
    font-weight: 600;
    color: #303133;
    margin-right: 10px;
  }
  
  .comment-time {
    font-size: 12px;
    color: #909399;
  }
  
  .comment-text {
    margin-bottom: 5px;
    line-height: 1.5;
  }
  
  .comment-actions {
    display: flex;
    gap: 10px;
  }
  
  .delete-btn {
    color: #f56c6c;
  }
  
  .reply-to {
    color: #409eff;
    font-weight: 500;
  }
  
  .replies {
    margin-top: 10px;
    padding-left: 15px;
    border-left: 2px solid #ebeef5;
  }
  
  .reply-item {
    display: flex;
    margin-bottom: 15px;
    gap: 10px;
  }
  
  .no-comments {
    padding: 30px 0;
    text-align: center;
    color: #909399;
  }
  
  .comment-loading {
    padding: 20px 0;
  }
  
  .pagination {
    margin-top: 20px;
    display: flex;
    justify-content: center;
  }
  
  .avatar-placeholder {
    background-color: #e8e8e8;
    color: #909399;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 20px;
    border-radius: 50%;
  }
  </style>