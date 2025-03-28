<template>
  <div class="comment-section">
    <h3 class="section-title">
      评论 ({{ total }})
    </h3>
    
    <!-- 评论输入框 -->
    <div v-if="userStore.isAuthenticated" class="comment-form">
      <div class="user-avatar">
        <img :src="getUserAvatar(userStore.currentUser)" alt="头像" />
      </div>
      
      <div class="comment-input">
        <el-input
          v-model="commentContent"
          type="textarea"
          :rows="3"
          placeholder="发表你的看法..."
          maxlength="1000"
          show-word-limit
        ></el-input>
        
        <div class="comment-btns">
          <el-button type="primary" @click="submitComment" :loading="submitting">
            发表评论
          </el-button>
        </div>
      </div>
    </div>
    
    <div v-else class="login-prompt">
      <el-alert
        title="请先登录后再评论"
        type="info"
        :closable="false"
        show-icon
      >
        <template #default>
          <div class="login-links">
            <router-link to="/login" class="login-link">登录</router-link>
            <span class="divider">或</span>
            <router-link to="/register" class="login-link">注册</router-link>
          </div>
        </template>
      </el-alert>
    </div>
    
    <!-- 评论列表 -->
    <div class="comment-list">
      <el-empty v-if="comments.length === 0" description="暂无评论，快来发表第一条评论吧！"></el-empty>
      
      <template v-else>
        <!-- 按照层级展示评论，只显示顶级评论，子评论在CommentItem组件内显示 -->
        <div v-for="comment in rootComments" :key="comment.id" class="comment-thread">
          <comment-item 
            :comment="comment" 
            :blog-id="blogId"
            :child-comments="getChildComments(comment.id)"
            @comment-updated="fetchComments"
          ></comment-item>
        </div>
        
        <!-- 分页器 -->
        <el-pagination
          v-if="total > pageSize"
          layout="prev, pager, next"
          :total="total"
          :page-size="pageSize"
          :current-page="currentPage"
          @current-change="handlePageChange"
          hide-on-single-page
          background
        ></el-pagination>
      </template>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted, watch } from 'vue'
import { useUserStore } from '@/stores/user'
import { useCommentStore } from '@/stores/comment'
import { ElMessage } from 'element-plus'
import CommentItem from './CommentItem.vue'

export default {
  name: 'CommentList',
  components: {
    CommentItem
  },
  props: {
    blogId: {
      type: Number,
      required: true
    }
  },
  setup(props) {
    const userStore = useUserStore()
    const commentStore = useCommentStore()
    
    const comments = ref([])
    const total = ref(0)
    const currentPage = ref(1)
    const pageSize = ref(10)
    const commentContent = ref('')
    const submitting = ref(false)
    
    // 计算出根评论
    const rootComments = computed(() => {
      return comments.value.filter(comment => !comment.parentId);
    })
    
    // 获取指定评论的子评论
    const getChildComments = (parentId) => {
      return comments.value.filter(comment => 
        comment.parentId === parentId
      );
    }
    
    // 监听博客ID变化，重新获取评论
    watch(() => props.blogId, () => {
      currentPage.value = 1
      fetchComments()
    })
    
    // 获取评论列表
    const fetchComments = async () => {
      try {
        console.log('开始获取评论，博客ID:', props.blogId)
        const result = await commentStore.fetchComments(props.blogId, {
          page: currentPage.value,
          size: pageSize.value,
          rootOnly: true // 只获取根评论
        })
        
        comments.value = result.comments
        total.value = result.total
        
        // 添加调试信息，详细查看评论数据结构
        console.log('评论加载成功，总数:', total.value)
        console.log('根评论数量:', rootComments.value.length)
        
        // 加载每个根评论的子评论
        const rootIds = rootComments.value.map(comment => comment.id)
        await Promise.all(rootIds.map(async (rootId) => {
          try {
            // 获取每个根评论的所有子评论（不分页，后端会处理）
            const childResult = await commentStore.fetchChildComments(rootId)
            // 不需要在这里处理，子评论数据会在getChildComments方法中使用
          } catch (error) {
            console.error(`获取评论ID ${rootId} 的子评论失败:`, error)
          }
        }))
      } catch (error) {
        console.error('获取评论失败:', error)
        ElMessage.error('获取评论失败，请稍后再试')
      }
    }
    
    // 提交评论
    const submitComment = async () => {
      if (!commentContent.value.trim()) {
        ElMessage.warning('评论内容不能为空')
        return
      }
      
      try {
        submitting.value = true
        await commentStore.createComment(props.blogId, commentContent.value)
        
        commentContent.value = ''
        ElMessage.success('评论发表成功')
        
        // 刷新评论列表
        fetchComments()
      } catch (error) {
        console.error('发表评论失败:', error)
        ElMessage.error('发表评论失败，请稍后再试')
      } finally {
        submitting.value = false
      }
    }
    
    // 处理页码变化
    const handlePageChange = (page) => {
      currentPage.value = page
      fetchComments()
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
    
    onMounted(() => {
      fetchComments()
    })
    
    return {
      comments,
      total,
      currentPage,
      pageSize,
      commentContent,
      submitting,
      userStore,
      rootComments,
      fetchComments,
      submitComment,
      handlePageChange,
      getChildComments,
      getUserAvatar
    }
  }
}
</script>

<style scoped>
.comment-section {
  margin-top: 30px;
  background-color: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.section-title {
  font-size: 1.5rem;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  align-items: center;
  color: #222;
}

.section-title::before {
  content: '';
  display: inline-block;
  width: 4px;
  height: 18px;
  background-color: #00a1d6; /* B站蓝色 */
  margin-right: 8px;
  border-radius: 2px;
}

.comment-form {
  display: flex;
  margin-bottom: 30px;
}

.user-avatar {
  margin-right: 15px;
}

.user-avatar img {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  border: none;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
  transition: all 0.2s ease;
}

.user-avatar img:hover {
  transform: translateY(-2px);
  box-shadow: 0 3px 8px rgba(0, 0, 0, 0.1);
}

.comment-input {
  flex-grow: 1;
}

.comment-input .el-textarea__inner {
  background-color: #f4f5f7;
  border: 1px solid #e5e9ef;
  border-radius: 4px;
  font-size: 14px;
  color: #222;
  transition: all 0.2s ease;
}

.comment-input .el-textarea__inner:focus {
  background-color: #fff;
  border-color: #00a1d6;
  box-shadow: 0 0 2px rgba(0, 161, 214, 0.2);
}

.comment-input .el-textarea__inner:hover {
  border-color: #ccd0d7;
}

.comment-input .el-input__count {
  background-color: transparent;
  color: #99a2aa;
  font-size: 12px;
}

.comment-btns {
  display: flex;
  justify-content: flex-end;
  margin-top: 10px;
}

.comment-btns .el-button {
  background-color: #00a1d6; /* B站蓝色 */
  border-color: #00a1d6;
  border-radius: 4px;
  transition: all 0.2s ease;
}

.comment-btns .el-button:hover {
  background-color: #00b5e5;
  border-color: #00b5e5;
}

.login-prompt {
  margin-bottom: 20px;
}

.login-links {
  margin-top: 5px;
  display: flex;
  gap: 10px;
  align-items: center;
}

.login-link {
  color: #409EFF;
  text-decoration: none;
  font-weight: 500;
}

.login-link:hover {
  text-decoration: underline;
}

.divider {
  color: #909399;
}

.comment-list {
  margin-top: 20px;
}

.comment-thread {
  margin-bottom: 15px;
}

.el-pagination {
  text-align: center;
  margin-top: 30px;
}

/* B站风格的分页器 */
.el-pagination .el-pager li {
  background-color: #fff;
  color: #505050;
  border: 1px solid #e5e9ef;
  border-radius: 4px;
  margin: 0 4px;
  transition: all 0.2s ease;
}

.el-pagination .el-pager li.active {
  background-color: #00a1d6;
  color: #fff;
  border-color: #00a1d6;
  font-weight: normal;
}

.el-pagination .el-pager li:hover:not(.active) {
  border-color: #00a1d6;
  color: #00a1d6;
}

.el-pagination .btn-prev,
.el-pagination .btn-next {
  background-color: #fff;
  color: #505050;
  border: 1px solid #e5e9ef;
  border-radius: 4px;
  padding: 0 10px;
  transition: all 0.2s ease;
}

.el-pagination .btn-prev:hover,
.el-pagination .btn-next:hover {
  border-color: #00a1d6;
  color: #00a1d6;
}

@media (max-width: 768px) {
  .comment-form {
    flex-direction: column;
  }
  
  .user-avatar {
    margin-bottom: 10px;
  }
}
</style>