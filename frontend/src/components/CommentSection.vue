<template>
    <div class="comment-section">
      <h3 class="section-title">
        评论 ({{ totalComments }})
      </h3>
      
      <!-- 评论表单 -->
      <div class="comment-form">
        <div class="user-avatar">
          <img 
            :src="getImageUrl(currentUser?.avatar) || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'" 
            alt="用户头像"
          >
        </div>
        <div class="comment-input-container">
          <el-input
            v-model="commentContent"
            type="textarea"
            :rows="3"
            placeholder="写下你的评论..."
            :maxlength="500"
            show-word-limit
          />
          
          <div class="comment-toolbar">
            <div class="emoji-picker">
              <el-popover
                placement="top-start"
                :width="300"
                trigger="click"
                popper-class="emoji-popover"
              >
                <template #reference>
                  <el-button 
                    type="text" 
                    size="small"
                    class="emoji-btn"
                  >
                    <i class="el-icon-sunny"></i> 表情
                  </el-button>
                </template>
                <div class="emoji-picker-container">
                  <div class="emoji-categories">
                    <div 
                      v-for="(category, index) in emojiCategories" 
                      :key="index"
                      :class="['category-tab', { active: currentEmojiCategory === index }]"
                      @click="changeEmojiCategory(index)"
                    >
                      {{ category.name }}
                    </div>
                  </div>
                  <div class="emoji-container">
                    <div 
                      v-for="emoji in currentEmojis" 
                      :key="emoji" 
                      class="emoji-item"
                      @click="insertEmoji(emoji)"
                    >
                      {{ emoji }}
                    </div>
                  </div>
                </div>
              </el-popover>
            </div>
            <el-button 
              type="primary" 
              size="small" 
              :disabled="!commentContent.trim()" 
              :loading="submitting"
              @click="submitComment"
            >
              发表评论
            </el-button>
          </div>
        </div>
      </div>
      
      <!-- 评论列表 -->
      <div v-if="loading" class="comment-loading">
        <el-skeleton :rows="3" animated />
      </div>
      <div v-else-if="comments.length === 0" class="empty-comments">
        <el-empty description="暂无评论，快来说点什么吧！" />
      </div>
      <div v-else class="comment-list">
        <div 
          v-for="comment in comments" 
          :key="comment.id" 
          class="comment-item"
        >
          <!-- 主评论 -->
          <div class="comment-content">
            <div class="user-avatar">
              <img 
                :src="getImageUrl(comment.user?.avatar) || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'" 
                alt="用户头像"
              >
            </div>
            <div class="comment-main">
              <div class="comment-header">
                <span class="comment-author">{{ comment.user?.username || '匿名用户' }}</span>
                <span class="comment-time">{{ formatDate(comment.createdAt) }}</span>
              </div>
              <div class="comment-text" v-html="formatComment(comment.content)"></div>
              <div class="comment-actions">
                <el-button 
                  type="text" 
                  size="small"
                  @click="toggleReplyForm(comment.id)"
                >
                  回复
                </el-button>
                <el-button 
                  v-if="canDeleteComment(comment)" 
                  type="text" 
                  size="small"
                  @click="deleteComment(comment.id)"
                >
                  删除
                </el-button>
                <el-button 
                  type="text" 
                  size="small"
                  @click="likeComment(comment.id)"
                >
                  <i class="el-icon-star-off"></i>
                  {{ comment.likes || 0 }}
                </el-button>
              </div>
              
              <!-- 回复表单 -->
              <div v-if="replyToId === comment.id" class="reply-form">
                <el-input
                  v-model="replyContent"
                  type="textarea"
                  :rows="2"
                  placeholder="回复评论..."
                  :maxlength="300"
                  show-word-limit
                />
                <div class="comment-toolbar">
                  <div class="emoji-picker">
                    <el-popover
                      placement="top-start"
                      :width="300"
                      trigger="click"
                      popper-class="emoji-popover"
                    >
                      <template #reference>
                        <el-button 
                          type="text" 
                          size="small"
                          class="emoji-btn"
                        >
                          <i class="el-icon-sunny"></i> 表情
                        </el-button>
                      </template>
                      <div class="emoji-picker-container">
                        <div class="emoji-categories">
                          <div 
                            v-for="(category, index) in emojiCategories" 
                            :key="index"
                            :class="['category-tab', { active: currentEmojiCategory === index }]"
                            @click="changeEmojiCategory(index)"
                          >
                            {{ category.name }}
                          </div>
                        </div>
                        <div class="emoji-container">
                          <div 
                            v-for="emoji in currentEmojis" 
                            :key="emoji" 
                            class="emoji-item"
                            @click="insertEmojiToReply(emoji)"
                          >
                            {{ emoji }}
                          </div>
                        </div>
                      </div>
                    </el-popover>
                  </div>
                  <div class="reply-actions">
                    <el-button 
                      size="small"
                      @click="toggleReplyForm(null)"
                    >
                      取消
                    </el-button>
                    <el-button 
                      type="primary" 
                      size="small" 
                      :disabled="!replyContent.trim()" 
                      :loading="submitting"
                      @click="submitReply(comment.id)"
                    >
                      回复
                    </el-button>
                  </div>
                </div>
              </div>
              
              <!-- 子评论/回复列表 -->
              <div v-if="comment.replies && comment.replies.length > 0" class="reply-list">
                <div 
                  v-for="reply in comment.replies" 
                  :key="reply.id" 
                  class="reply-item"
                >
                  <div class="user-avatar">
                    <img 
                      :src="getImageUrl(reply.user?.avatar) || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'" 
                      alt="用户头像"
                    >
                  </div>
                  <div class="reply-main">
                    <div class="comment-header">
                      <span class="comment-author">{{ reply.user?.username || '匿名用户' }}</span>
                      <span v-if="reply.replyToUser">
                        回复 
                        <span class="reply-to-user">{{ reply.replyToUser.username }}</span>
                      </span>
                      <span class="comment-time">{{ formatDate(reply.createdAt) }}</span>
                    </div>
                    <div class="comment-text" v-html="formatComment(reply.content)"></div>
                    <div class="comment-actions">
                      <el-button 
                        type="text" 
                        size="small"
                        @click="toggleReplyForm(comment.id, reply.user)"
                      >
                        回复
                      </el-button>
                      <el-button 
                        v-if="canDeleteComment(reply)" 
                        type="text" 
                        size="small"
                        @click="deleteReply(comment.id, reply.id)"
                      >
                        删除
                      </el-button>
                      <el-button 
                        type="text" 
                        size="small"
                        @click="likeReply(comment.id, reply.id)"
                      >
                        <i class="el-icon-star-off"></i>
                        {{ reply.likes || 0 }}
                      </el-button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 分页 -->
      <div class="comment-pagination" v-if="totalComments > pageSize">
        <el-pagination
          :current-page="currentPage"
          :page-size="pageSize"
          layout="prev, pager, next"
          :total="totalComments"
          @current-change="handlePageChange"
        />
      </div>
    </div>
  </template>
  
  <script>
  import { ref, reactive, computed, onMounted, watch } from 'vue'
  import { useUserStore } from '@/stores/user'
  import axios from 'axios'
  import { ElMessage, ElMessageBox } from 'element-plus'
  
  export default {
    name: 'CommentSection',
    props: {
      targetId: {
        type: [String, Number],
        required: true
      },
      targetType: {
        type: String,
        default: 'blog'
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
      const commentContent = ref('')
      const replyContent = ref('')
      const replyToId = ref(null)
      const replyToUser = ref(null)
      
      const commentForm = reactive({
        content: '',
        username: '',
        email: ''
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
      
      // 检查用户是否登录
      const isLoggedIn = computed(() => userStore.isLoggedIn)
      
      // 获取用户头像
      const userAvatar = computed(() => {
        if (isLoggedIn.value) {
          return userStore.user?.avatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
        }
        return null
      })
      
      // 表情符号列表
      const emojis = ref([
        '😀', '😃', '😄', '😁', '😆', '😅', '😂', '🤣', '😊', '😇',
        '🙂', '🙃', '😉', '😌', '😍', '🥰', '😘', '😗', '😙', '😚',
        '😋', '😛', '😜', '😝', '🤑', '🤗', '🤔', '🤭', '🤫', '🤥',
        '😏', '😒', '😞', '😔', '😟', '😕', '🙁', '☹️', '😣', '😖',
        '😫', '😩', '🥺', '😢', '😭', '😤', '😠', '😡', '🤬', '🤯',
        '😳', '🥵', '🥶', '😱', '😨', '😰', '😥', '😓', '🤗', '🤔',
        '🤭', '🤫', '🤥', '😶', '😐', '😑', '😬', '🙄', '👋', '🤚',
        '✋', '👌', '👍', '👎', '❤️', '🔥', '😈'
      ])
      
      // 表情符号分类
      const emojiCategories = ref([
        {
          name: '表情',
          emojis: ['😀', '😃', '😄', '😁', '😆', '😅', '😂', '🤣', '😊', '😇', 
                  '🙂', '🙃', '😉', '😌', '😍', '🥰', '😘', '😗', '😙', '😚']
        },
        {
          name: '手势',
          emojis: ['👋', '🤚', '✋', '👌', '👍', '👎', '✊', '👊', '🤛', '🤜', 
                  '👏', '🙌', '👐', '🤲', '🤝', '🙏', '✍️', '💪', '🦾', '🖐️']
        },
        {
          name: '符号',
          emojis: ['❤️', '🧡', '💛', '💚', '💙', '💜', '🖤', '🤍', '🤎', '💔', 
                  '❣️', '💕', '💞', '💓', '💗', '💖', '💘', '💝', '💟', '☮️']
        }
      ])
      
      // 当前选中的表情分类
      const currentEmojiCategory = ref(0)
      
      // 获取当前分类的表情
      const currentEmojis = computed(() => {
        return emojiCategories.value[currentEmojiCategory.value].emojis
      })
      
      // 切换表情分类
      const changeEmojiCategory = (index) => {
        currentEmojiCategory.value = index
      }
      
      // 获取当前用户
      const currentUser = computed(() => userStore.user)
      
      // 监听目标ID变化，重新加载评论
      watch(() => props.targetId, () => {
        if (props.targetId) {
          currentPage.value = 1
          fetchComments()
        }
      })
      
      // 初始化时加载评论
      onMounted(() => {
        if (props.targetId) {
          fetchComments()
        }
      })
      
      // 获取评论列表
      const fetchComments = async () => {
        if (!props.targetId) return
        
        loading.value = true
        
        try {
          const response = await axios.get(`/api/comments/blog/${props.targetId}`, {
            params: {
              page: currentPage.value - 1,
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
        if (!commentContent.value.trim()) {
          ElMessage.warning('评论内容不能为空')
          return
        }
        
        submitting.value = true
        
        try {
          // 准备评论数据
          const commentData = {
            content: commentContent.value,
            targetId: props.targetId,
            targetType: props.targetType
          }
          
          // 提交评论
          const headers = isLoggedIn.value ? {
            Authorization: `Bearer ${localStorage.getItem('token') || sessionStorage.getItem('token')}`
          } : {}
          
          // 提交评论api
          const response = await axios.post(`/api/auth/comments/blog/${props.targetId}`, commentData, {
            params: {
              content: commentContent.value,
            },
            headers: headers
          })
          
          // 模拟新评论
          const newComment = {
            id: Date.now(),
            content: commentContent.value,
            createdAt: new Date().toISOString(),
            user: currentUser.value || {
              id: 999,
              username: '访客',
              avatar: 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
            },
            likes: 0,
            replies: []
          }
          
          // 添加到评论列表开头
          comments.value.unshift(newComment)
          totalComments.value++
          
          // 清空评论内容
          commentContent.value = ''
          
          ElMessage.success('评论发表成功')
        } catch (error) {
          console.error('提交评论失败:', error)
          ElMessage.error('评论发表失败，请稍后重试')
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
      
      // 页面加载时获取评论
      onMounted(() => {
        fetchComments()
      })
      
      // 格式化评论内容，支持表情符号和链接
      const formatComment = (content) => {
        if (!content) return ''
        
        // 转义HTML特殊字符
        let formattedContent = content
          .replace(/&/g, '&amp;')
          .replace(/</g, '&lt;')
          .replace(/>/g, '&gt;')
        
        // 将URL转为链接
        const urlRegex = /(https?:\/\/[^\s]+)/g
        formattedContent = formattedContent.replace(
          urlRegex, 
          '<a href="$1" target="_blank" rel="noopener noreferrer">$1</a>'
        )
        
        // 将@用户名高亮显示
        const atRegex = /@(\w+)/g
        formattedContent = formattedContent.replace(
          atRegex,
          '<span class="at-user">@$1</span>'
        )
        
        return formattedContent
      }
      
      // 判断当前用户是否可以删除评论
      const canDeleteComment = (comment) => {
        if (!currentUser.value) return false
        
        // 是评论作者或管理员可以删除
        return currentUser.value.id === comment.user?.id || 
               currentUser.value.role === 'admin'
      }
      
      // 插入表情符号到主评论
      const insertEmoji = (emoji) => {
        commentContent.value += emoji
      }
      
      // 插入表情符号到回复
      const insertEmojiToReply = (emoji) => {
        replyContent.value += emoji
      }
      
      // 切换回复表单
      const toggleReplyForm = (commentId, user = null) => {
        if (replyToId.value === commentId && !user) {
          // 关闭回复表单
          replyToId.value = null
          replyToUser.value = null
          replyContent.value = ''
        } else {
          // 打开回复表单
          replyToId.value = commentId
          replyToUser.value = user
          replyContent.value = user ? `@${user.username} ` : ''
        }
      }
      
      // 提交回复
      const submitReply = async (commentId) => {
        if (!replyContent.value.trim()) {
          ElMessage.warning('回复内容不能为空')
          return
        }
        
        submitting.value = true
        
        try {
          // 准备评论数据
          const replyData = {
            content: replyContent.value,
            parentCommentId: commentId
          }
          
          if (replyToUser.value) {
            replyData.replyToUserId = replyToUser.value.id
          }
          
          // 提交回复
          await axios.post(`/api/comments/blog/${props.targetId}`, replyData, {
            headers: {
              Authorization: `Bearer ${localStorage.getItem('token') || sessionStorage.getItem('token')}`
            }
          })
          
          // 回复成功
          ElMessage.success('回复成功')
          
          // 清空回复内容并关闭回复表单
          replyContent.value = ''
          replyToId.value = null
          replyToUser.value = null
          
          // 刷新评论列表
          fetchComments()
        } catch (error) {
          console.error('提交回复失败:', error)
          ElMessage.error('回复失败，请稍后重试')
        } finally {
          submitting.value = false
        }
      }
      
      // 删除回复
      const deleteReply = async (commentId, replyId) => {
        try {
          await ElMessageBox.confirm('确定要删除这条回复吗？', '删除确认', {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          })
          
          await axios.delete(`/api/comments/${replyId}`, {
            headers: {
              Authorization: `Bearer ${localStorage.getItem('token') || sessionStorage.getItem('token')}`
            }
          })
          
          ElMessage.success('回复已删除')
          
          // 刷新评论列表
          fetchComments()
        } catch (error) {
          if (error !== 'cancel') {
            console.error('删除回复失败:', error)
            ElMessage.error('删除回复失败，请稍后重试')
          }
        }
      }
      
      // 点赞评论
      const likeComment = async (commentId) => {
        try {
          await axios.post(`/api/comments/${commentId}/like`, {}, {
            headers: {
              Authorization: `Bearer ${localStorage.getItem('token') || sessionStorage.getItem('token')}`
            }
          })
          
          // 刷新评论列表
          fetchComments()
        } catch (error) {
          console.error('点赞评论失败:', error)
          ElMessage.error('点赞失败，请稍后重试')
        }
      }
      
      // 点赞回复
      const likeReply = async (commentId, replyId) => {
        try {
          await axios.post(`/api/comments/${replyId}/like`, {}, {
            headers: {
              Authorization: `Bearer ${localStorage.getItem('token') || sessionStorage.getItem('token')}`
            }
          })
          
          // 刷新评论列表
          fetchComments()
        } catch (error) {
          console.error('点赞回复失败:', error)
          ElMessage.error('点赞失败，请稍后重试')
        }
      }
      
      // 处理图片URL
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
        comments,
        commentContent,
        replyContent,
        replyToId,
        replyToUser,
        loading,
        submitting,
        currentPage,
        pageSize,
        totalComments,
        emojis,
        emojiCategories,
        currentEmojiCategory,
        currentEmojis,
        changeEmojiCategory,
        currentUser,
        submitComment,
        toggleReplyForm,
        submitReply,
        deleteComment,
        deleteReply,
        likeComment,
        likeReply,
        handlePageChange,
        formatComment,
        formatDate,
        canDeleteComment,
        insertEmoji,
        insertEmojiToReply,
        getImageUrl
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
  
  /* 评论工具栏样式 */
  .comment-toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-top: 10px;
  }
  
  .comment-input-container {
    flex: 1;
    display: flex;
    flex-direction: column;
  }
  
  .reply-actions {
    display: flex;
    gap: 10px;
    justify-content: flex-end;
  }
  
  .user-avatar {
    width: 40px;
    height: 40px;
    border-radius: 50%;
    overflow: hidden;
  }
  
  .user-avatar img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
  
  /* 表情选择器样式 */
  .emoji-picker-container {
    display: flex;
    flex-direction: column;
    max-height: 250px;
  }
  
  .emoji-categories {
    display: flex;
    border-bottom: 1px solid rgba(0, 0, 0, 0.1);
    margin-bottom: 8px;
  }
  
  .category-tab {
    padding: 6px 12px;
    font-size: 14px;
    cursor: pointer;
    border-bottom: 2px solid transparent;
    transition: all 0.3s;
    color: #606266;
  }
  
  .category-tab.active {
    color: #409eff;
    border-bottom-color: #409eff;
  }
  
  .category-tab:hover {
    background-color: rgba(64, 158, 255, 0.1);
  }
  
  .emoji-container {
    display: grid;
    grid-template-columns: repeat(7, 1fr);
    gap: 8px;
    padding: 12px;
    max-height: 200px;
    overflow-y: auto;
    border-radius: 8px;
  }
  
  .emoji-item {
    font-size: 22px;
    cursor: pointer;
    text-align: center;
    padding: 8px;
    border-radius: 4px;
    transition: all 0.2s;
    user-select: none;
  }
  
  .emoji-item:hover {
    background-color: rgba(64, 158, 255, 0.1);
    transform: scale(1.2);
  }
  
  .emoji-btn {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 14px;
    color: #606266;
  }
  
  /* 回复表单样式 */
  .reply-form {
    margin-top: 10px;
    background-color: rgba(245, 247, 250, 0.6);
    padding: 10px;
    border-radius: 4px;
  }
  
  /* 修改回复表单的表情选择框样式 */
  .reply-form .emoji-container {
    grid-template-columns: repeat(6, 1fr);
  }
  
  /* 评论内容样式 */
  .comment-text {
    word-break: break-word;
    line-height: 1.6;
  }
  
  .comment-text a {
    color: #409eff;
    text-decoration: none;
  }
  
  .comment-text a:hover {
    text-decoration: underline;
  }
  
  .comment-text .at-user {
    color: #409eff;
    font-weight: 500;
  }
  </style>

<style>
/* 全局样式，为表情弹出层添加毛玻璃效果 */
.emoji-popover.el-popper,
.el-popover.emoji-popover {
  background: rgba(255, 255, 255, 0.8) !important;
  backdrop-filter: blur(10px) !important;
  -webkit-backdrop-filter: blur(10px) !important;
  border: 1px solid rgba(255, 255, 255, 0.2) !important;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1) !important;
  border-radius: 12px !important;
  overflow: hidden !important;
  padding: 0 !important;
}

.emoji-popover .emoji-picker-container {
  background: transparent !important;
}

.emoji-popover .el-popover__title {
  color: #606266;
  margin: 0;
  padding: 10px 15px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
}

/* 自定义滚动条样式 */
.emoji-container::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

.emoji-container::-webkit-scrollbar-thumb {
  background: rgba(144, 147, 153, 0.3);
  border-radius: 6px;
}

.emoji-container::-webkit-scrollbar-track {
  background: transparent;
}
</style>