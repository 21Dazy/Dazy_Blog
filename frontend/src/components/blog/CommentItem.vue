<template>
    <div class="comment-item" :class="{ 'child-comment': isChild }">
      <div class="comment-avatar">
        <img :src="getUserAvatar(comment.user)" alt="头像" />
      </div>
      
      <div class="comment-content">
        <div class="comment-header">
          <div class="comment-user">
            <strong>{{ getUserName(comment.user) }}</strong>
            <span v-if="isChild" class="reply-indicator">
              回复 <strong>{{ parentAuthor }}</strong>
            </span>
          </div>
          <div class="comment-date">{{ formatDate(comment.createdAt) }}</div>
        </div>
        
        <div class="comment-text">{{ comment.content }}</div>
        
        <div class="comment-actions">
          <button class="action-btn like-btn" 
            :class="{ 'liked': comment.isLiked }"
            @click="handleLike">
            <el-icon><StarFilled /></el-icon>
            <span>{{ comment.likes || 0 }}</span>
          </button>
          
          <button class="action-btn reply-btn" @click="toggleReply">
            <el-icon><ChatDotRound /></el-icon>
            <span>回复</span>
          </button>
          
          <button v-if="canEdit" class="action-btn edit-btn" @click="handleEdit">
            <el-icon><Edit /></el-icon>
            <span>编辑</span>
          </button>
          
          <button v-if="canDelete" class="action-btn delete-btn" @click="handleDelete">
            <el-icon><Delete /></el-icon>
            <span>删除</span>
          </button>
        </div>
        
        <!-- 回复框 -->
        <div v-if="showReplyBox" class="reply-box">
          <el-input
            v-model="replyContent"
            type="textarea"
            rows="2"
            placeholder="回复评论..."
            maxlength="500"
            show-word-limit
          ></el-input>
          
          <div class="reply-actions">
            <el-button size="small" @click="toggleReply">取消</el-button>
            <el-button type="primary" size="small" @click="submitReply" :loading="loading">
              回复
            </el-button>
          </div>
        </div>
        
        <!-- 编辑框 -->
        <div v-if="showEditBox" class="edit-box">
          <el-input
            v-model="editContent"
            type="textarea"
            rows="3"
            placeholder="编辑评论..."
            maxlength="500"
            show-word-limit
          ></el-input>
          
          <div class="edit-actions">
            <el-button size="small" @click="cancelEdit">取消</el-button>
            <el-button type="primary" size="small" @click="submitEdit" :loading="loading">
              保存
            </el-button>
          </div>
        </div>
        
        <!-- 子评论列表 -->
        <div v-if="childComments && childComments.length > 0" class="child-comments">
          <div class="child-comments-header">
            <span class="child-count">{{ childComments.length }}条回复</span>
            <span v-if="childComments.length > childPageSize" class="child-display-status">
              {{ isChildExpanded ? `正在显示第${childCurrentPage}页，共${Math.ceil(childComments.length / childPageSize)}页` : '显示部分回复' }}
            </span>
          </div>
          
          <!-- 显示当前页的子评论 -->
          <comment-item
            v-for="childComment in displayedChildComments"
            :key="childComment.id"
            :comment="childComment"
            :parent-author="comment.user ? getUserName(comment.user) : '匿名用户'"
            :is-child="true"
            :blog-id="blogId"
            @comment-updated="$emit('comment-updated')"
          ></comment-item>
          
          <!-- 控制区域：分页 + 展开/折叠控制 -->
          <div class="child-comments-control">
            <!-- 展开状态下显示分页 -->
            <div v-if="isChildExpanded && childComments.length > childPageSize" class="child-pagination">
              <el-pagination
                :pager-count="5"
                layout="prev, pager, next"
                :total="childComments.length"
                :page-size="childPageSize"
                :current-page="childCurrentPage"
                @current-change="handleChildPageChange"
                small
                hide-on-single-page
              ></el-pagination>
            </div>
            
            <!-- 展开/折叠开关 - 仅当超过默认显示数量时显示 -->
            <div v-if="childComments.length > childPageSize" class="bilibili-toggle" @click="toggleChildComments">
              <el-button type="text" size="small" class="toggle-button">
                <template v-if="isChildExpanded">
                  <el-icon class="toggle-icon"><ArrowUp /></el-icon>
                  <span>收起回复</span>
                </template>
                <template v-else>
                  <el-icon class="toggle-icon"><ArrowDown /></el-icon>
                  <span>查看全部{{ childComments.length }}条回复</span>
                </template>
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </template>
  
  <script>
  import { ref, computed, onMounted } from 'vue'
  import { useUserStore } from '@/stores/user'
  import { useCommentStore } from '@/stores/comment'
  import { ElMessage, ElMessageBox } from 'element-plus'
  import { ArrowUp, ArrowDown, Edit, Delete, StarFilled, ChatDotRound } from '@element-plus/icons-vue'
  
  // 使用前向引用解决递归组件问题
  const CommentItem = () => import('./CommentItem.vue')
  
  export default {
    name: 'CommentItem',
    components: {
      CommentItem,
      ArrowUp,
      ArrowDown,
      Edit,
      Delete,
      StarFilled,
      ChatDotRound
    },
    props: {
      comment: {
        type: Object,
        required: true
      },
      parentAuthor: {
        type: String,
        default: ''
      },
      isChild: {
        type: Boolean,
        default: false
      },
      blogId: {
        type: Number,
        required: true
      },
      childComments: {
        type: Array,
        default: () => []
      }
    },
    emits: ['comment-updated'],
    setup(props, { emit }) {
      const userStore = useUserStore()
      const commentStore = useCommentStore()
      
      const loading = ref(false)
      const showReplyBox = ref(false)
      const showEditBox = ref(false)
      const replyContent = ref('')
      const editContent = ref('')
      
      // 调试日志 - 在组件挂载时记录信息
      onMounted(() => {
        console.log(`评论组件挂载 - ID: ${props.comment.id} (${typeof props.comment.id})`);
        
        // 确保评论ID是数字类型
        if (props.comment.id !== undefined) {
          props.comment.id = Number(props.comment.id);
        }
        
        // 检查是否有parentId字段
        if (props.comment.parentId !== undefined && props.comment.parentId !== null) {
          props.comment.parentId = Number(props.comment.parentId);
          console.log(`这是子评论! 父评论ID: ${props.comment.parentId} (${typeof props.comment.parentId})`);
        } 
        else if (props.comment.parent_id !== undefined && props.comment.parent_id !== null) {
          props.comment.parentId = Number(props.comment.parent_id);
          console.log(`从parent_id设置父评论ID: ${props.comment.parentId} (${typeof props.comment.parentId})`);
          // 删除parent_id，统一使用parentId
          delete props.comment.parent_id;
        }
        else if (props.comment.parent && props.comment.parent.id) {
          props.comment.parentId = Number(props.comment.parent.id);
          console.log(`从parent对象设置父评论ID: ${props.comment.parentId} (${typeof props.comment.parentId})`);
        }
        
        if (props.isChild) {
          console.log(`通过isChild属性标记为子评论, parentAuthor: ${props.parentAuthor}`);
        }
        
        if (props.childComments && props.childComments.length > 0) {
          console.log(`有 ${props.childComments.length} 个子评论:`);
          props.childComments.forEach(child => {
            console.log(`- 子评论ID: ${child.id}, 内容: ${child.content.substring(0, 20)}...`);
          });
        } else {
          console.log('没有子评论');
        }
      })
      
      // 子评论分页相关
      const childPageSize = ref(5) // 默认显示5条子评论，超过时折叠
      const childCurrentPage = ref(1)
      const isChildExpanded = ref(false) // 子评论是否展开状态
      
      // 计算当前页要显示的子评论
      const displayedChildComments = computed(() => {
        if (!props.childComments || props.childComments.length === 0) {
          return []
        }
        
        // 如果没有展开，只显示前几条
        if (!isChildExpanded.value) {
          return props.childComments.slice(0, childPageSize.value);
        }
        
        // 已展开，根据当前页显示
        const startIndex = (childCurrentPage.value - 1) * childPageSize.value;
        const endIndex = Math.min(startIndex + childPageSize.value, props.childComments.length);
        return props.childComments.slice(startIndex, endIndex);
      })
      
      // 处理子评论分页变化
      const handleChildPageChange = (page) => {
        console.log(`子评论分页：切换到第 ${page} 页`);
        childCurrentPage.value = page;
      }
      
      // 切换子评论展开/折叠状态
      const toggleChildComments = () => {
        isChildExpanded.value = !isChildExpanded.value;
        
        // 展开时重置为第一页
        if (isChildExpanded.value) {
          childCurrentPage.value = 1;
          console.log('展开子评论，重置为第1页');
        } else {
          console.log('折叠子评论');
        }
      }
      
      // 当前用户是否可以编辑/删除评论
      const canEdit = computed(() => {
        return userStore.isAuthenticated && 
               userStore.currentUser && 
               props.comment.user && 
               userStore.currentUser.id === props.comment.user.id
      })
      
      const canDelete = computed(() => canEdit.value)
      
      // 点赞评论
      const handleLike = async () => {
        if (!userStore.isAuthenticated) {
          ElMessage.warning('请先登录再点赞')
          return
        }
        
        try {
          loading.value = true
          // 根据当前状态切换点赞/取消点赞
          await commentStore.toggleLike(props.comment.id, props.comment.isLiked)
          ElMessage.success(props.comment.isLiked ? '取消点赞成功' : '点赞成功')
          emit('comment-updated')
        } catch (error) {
          console.error('点赞操作失败:', error)
          ElMessage.error('点赞操作失败')
        } finally {
          loading.value = false
        }
      }
      
      // 切换回复框显示状态
      const toggleReply = () => {
        if (!userStore.isAuthenticated) {
          ElMessage.warning('请先登录再回复')
          return
        }
        
        showReplyBox.value = !showReplyBox.value
        if (!showReplyBox.value) {
          replyContent.value = ''
        }
      }
      
      // 提交回复
      const submitReply = async () => {
        if (!replyContent.value.trim()) {
          ElMessage.warning('回复内容不能为空')
          return
        }
        
        try {
          loading.value = true
          await commentStore.replyToComment(
            props.blogId, 
            props.comment.id, 
            replyContent.value
          )
          
          replyContent.value = ''
          showReplyBox.value = false
          ElMessage.success('回复成功')
          emit('comment-updated')
        } catch (error) {
          console.error('回复失败:', error)
          ElMessage.error('回复失败，请稍后再试')
        } finally {
          loading.value = false
        }
      }
      
      // 处理编辑评论
      const handleEdit = () => {
        editContent.value = props.comment.content
        showEditBox.value = true
      }
      
      // 取消编辑
      const cancelEdit = () => {
        showEditBox.value = false
        editContent.value = ''
      }
      
      // 提交编辑
      const submitEdit = async () => {
        if (!editContent.value.trim()) {
          ElMessage.warning('评论内容不能为空')
          return
        }
        
        try {
          loading.value = true
          await commentStore.updateComment(props.comment.id, editContent.value)
          
          showEditBox.value = false
          ElMessage.success('评论更新成功')
          emit('comment-updated')
        } catch (error) {
          console.error('更新评论失败:', error)
          ElMessage.error('更新评论失败，请稍后再试')
        } finally {
          loading.value = false
        }
      }
      
      // 处理删除评论
      const handleDelete = async () => {
        try {
          await ElMessageBox.confirm(
            '确定要删除这条评论吗？此操作不可恢复', 
            '提示', 
            {
              confirmButtonText: '确定',
              cancelButtonText: '取消',
              type: 'warning'
            }
          )
          
          loading.value = true
          await commentStore.deleteComment(props.comment.id)
          
          ElMessage.success('评论已删除')
          emit('comment-updated')
        } catch (error) {
          if (error !== 'cancel') {
            console.error('删除评论失败:', error)
            ElMessage.error('删除评论失败，请稍后再试')
          }
        } finally {
          loading.value = false
        }
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
      // 获取用户头像
      const getUserAvatar = (user) => {
        if (!user || !user.avatar) {
          return 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
        }
        
        if (user.avatar.startsWith('http')) {
          return user.avatar
        }
        
        // 添加API前缀
        let avatar = user.avatar
        if (!avatar.startsWith('/')) {
          avatar = '/' + avatar
        }
        
        if (avatar.startsWith('/uploads')) {
          avatar = '/api' + avatar
        }
        
        return `http://localhost:8080${avatar}`
      }
      
      // 获取用户名，处理可能的空用户情况
      const getUserName = (user) => {
        if (!user) return '匿名用户'
        if (typeof user === 'string') return user // 防止直接传入字符串
        return user.username || user.name || '匿名用户'
      }
      
      return {
        loading,
        showReplyBox,
        showEditBox,
        replyContent,
        editContent,
        canEdit,
        canDelete,
        handleLike,
        toggleReply,
        submitReply,
        handleEdit,
        cancelEdit,
        submitEdit,
        handleDelete,
        formatDate,
        getUserAvatar,
        getUserName,
        childPageSize,
        childCurrentPage,
        isChildExpanded,
        displayedChildComments,
        handleChildPageChange,
        toggleChildComments
      }
    }
  }
  </script>
  
  <style scoped>
  .comment-item {
    display: flex;
    margin-bottom: 20px;
    padding-bottom: 15px;
    border-bottom: 1px solid #eee;
    border-radius: 6px;
    transition: background-color 0.2s ease;
  }
  
  .comment-item:hover {
    background-color: #f9f9f9;
  }
  
  .comment-avatar {
    margin-right: 15px;
    flex-shrink: 0;
  }
  
  .comment-avatar img {
    width: 40px;
    height: 40px;
    border-radius: 50%;
    object-fit: cover;
    border: 1px solid #eee;
  }
  
  .comment-content {
    flex-grow: 1;
  }
  
  .comment-header {
    display: flex;
    justify-content: space-between;
    margin-bottom: 5px;
  }
  
  .comment-user {
    font-size: 14px;
    display: flex;
    align-items: center;
  }
  
  .reply-indicator {
    position: relative;
    margin-left: 8px;
    padding-left: 8px;
    color: #909399;
    font-size: 12px;
  }
  
  .comment-date {
    color: #999;
    font-size: 12px;
  }
  
  .comment-text {
    margin-bottom: 8px;
    line-height: 1.5;
    word-break: break-word;
  }
  
  .comment-actions {
    display: flex;
    gap: 15px;
  }
  
  .action-btn {
    background: none;
    border: none;
    color: #606266;
    font-size: 12px;
    cursor: pointer;
    padding: 0;
    display: flex;
    align-items: center;
    gap: 4px;
    transition: all 0.3s;
  }
  
  .action-btn:hover {
    color: #409EFF;
  }
  
  .like-btn.liked {
    color: #fb7299; /* B站的粉红色 */
  }
  
  .delete-btn:hover {
    color: #F56C6C;
  }
  
  .reply-box, .edit-box {
    margin-top: 10px;
  }
  
  .reply-actions, .edit-actions {
    display: flex;
    justify-content: flex-end;
    margin-top: 10px;
    gap: 10px;
  }
  
  .child-comments {
    margin-top: 12px;
    padding-left: 10px;
    border-left: 2px solid #f0f0f0;
  }
  
  .child-comments-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 10px;
    color: #909399;
    font-size: 14px;
  }
  
  .child-count {
    font-weight: 500;
  }
  
  .child-display-status {
    font-size: 12px;
    color: #909399;
  }
  
  .child-comments-control {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-top: 8px;
  }
  
  .bilibili-toggle {
    cursor: pointer;
    display: flex;
    justify-content: center;
    width: 100%;
  }
  
  .toggle-button {
    display: flex;
    align-items: center;
    justify-content: center;
    color: #409EFF;
  }
  
  .toggle-icon {
    margin-right: 4px;
  }
  
  .child-pagination {
    display: flex;
    justify-content: center;
    width: 100%;
    margin-bottom: 8px;
  }
  
  :deep(.el-pagination) {
    justify-content: center;
  }
  
  :deep(.el-pagination .el-pager li) {
    min-width: 28px;
    height: 28px;
    line-height: 28px;
  }
  
  .child-comment {
    margin-left: 0 !important;
    padding-left: 0 !important;
    padding-top: 12px;
    border-top: 1px solid rgba(240, 240, 240, 0.5);
  }
  
  .reply-indicator {
    color: #909399;
    margin-left: 5px;
    font-size: 12px;
  }
  
  .reply-indicator strong {
    color: #409EFF;
  }
  
  @media (max-width: 768px) {
    .comment-item {
      flex-direction: column;
    }
    
    .comment-avatar {
      margin-bottom: 10px;
    }
    
    .comment-header {
      flex-direction: column;
    }
    
    .comment-date {
      margin-top: 5px;
    }
    
    .child-comments {
      margin-left: 10px;
      padding-left: 15px;
    }
  }
  </style>