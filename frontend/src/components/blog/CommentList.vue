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
        <div v-for="comment in displayedRootComments" :key="comment.id" class="comment-thread">
          <comment-item 
            :comment="comment" 
            :blog-id="blogId"
            :child-comments="getChildComments(comment.id)"
            @comment-updated="fetchComments"
          ></comment-item>
        </div>
        
        <!-- 根评论分页器 -->
        <el-pagination
          v-if="rootComments.length > rootCommentsPageSize"
          layout="total, prev, pager, next"
          :total="rootComments.length"
          :page-size="rootCommentsPageSize"
          :current-page="rootCommentsCurrentPage"
          @current-change="handleRootCommentsPageChange"
          background
          class="pagination-container"
          hide-on-single-page
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
    const pageSize = ref(50)
    const commentContent = ref('')
    const submitting = ref(false)
    
    // 根评论分页相关
    const rootCommentsPageSize = ref(5); // 每页显示5条根评论
    const rootCommentsCurrentPage = ref(1);

    // 当前页显示的根评论
    const rootComments = computed(() => {
      // 打印出所有评论，便于调试
      console.log('计算根评论列表，当前评论总数:', comments.value.length);
      
      // 更宽松的过滤条件 - 一个评论是根评论，如果它没有parentId或parentId为null/0/undefined/false
      const roots = comments.value.filter(comment => {
        const isRoot = !comment.parentId || comment.parentId === 0 || comment.parentId === null;
        if (isRoot) {
          console.log(`识别到根评论: ID=${comment.id}, 内容=${comment.content.substring(0, 15)}...`);
        }
        return isRoot;
      });
      
      console.log(`找到 ${roots.length} 个根评论`);
      return roots;
    })
    
    // 当前页显示的根评论
    const displayedRootComments = computed(() => {
      if (rootComments.value.length <= rootCommentsPageSize.value) {
        return rootComments.value;
      }
      
      const startIndex = (rootCommentsCurrentPage.value - 1) * rootCommentsPageSize.value;
      const endIndex = startIndex + rootCommentsPageSize.value;
      
      return rootComments.value.slice(startIndex, endIndex);
    });
    
    // 获取指定评论的子评论
    const getChildComments = (parentId) => {
      // 确保parentId是数字类型
      const numericParentId = Number(parentId);
      
      console.log(`查找评论ID ${parentId} (${typeof parentId}) 的子评论`);
      console.log('当前所有评论数量:', comments.value.length);
      
      // 检查评论数据结构，同时支持parent_id和parentId两种属性名
      const children = comments.value.filter(comment => {
        // 提取所有可能的父ID形式并转换为数字
        let commentParentId = null;
        
        if (comment.parentId !== undefined && comment.parentId !== null) {
          commentParentId = Number(comment.parentId);
          console.log(`评论ID ${comment.id} 有parentId属性: ${commentParentId}`);
        } 
        else if (comment.parent_id !== undefined && comment.parent_id !== null) {
          commentParentId = Number(comment.parent_id);
          console.log(`评论ID ${comment.id} 有parent_id属性: ${commentParentId}`);
        }
        else if (comment.parent && comment.parent.id) {
          commentParentId = Number(comment.parent.id);
          console.log(`评论ID ${comment.id} 有parent对象, id: ${commentParentId}`);
        }
        
        // 类型安全的比较
        const isChild = commentParentId === numericParentId;
        
        if (isChild) {
          console.log(`匹配成功! 评论ID ${comment.id} 是评论ID ${numericParentId} 的子评论`);
        }
        return isChild;
      });
      
      console.log(`评论ID ${parentId} 的子评论数量:`, children.length);
      if (children.length > 0) {
        console.log('子评论列表:', children);
      }
      return children;
    }
    
    // 监听博客ID变化，重新获取评论
    watch(() => props.blogId, () => {
      currentPage.value = 1
      fetchComments()
    })
    
    // 获取评论列表
    const fetchComments = async () => {
      try {
        console.log('开始获取评论，博客ID:', props.blogId);
        const result = await commentStore.fetchComments(props.blogId, {
          page: currentPage.value,
          size: pageSize.value,
          rootOnly: false // 获取所有评论，然后在前端整理父子关系
        });
        
        let allComments = result.comments;
        total.value = result.total;
        
        // 添加调试信息，详细查看评论数据结构
        console.log('评论加载成功，总数:', total.value);
        
        // 确保每个评论都有正确的parentId属性，并处理类型统一问题
        allComments = allComments.map(comment => {
          // 处理评论ID为数字类型
          if (comment.id !== undefined) {
            comment.id = Number(comment.id);
          }
          
          // 处理可能的不同字段命名，确保parentId统一为数字类型
          if (comment.parent && comment.parent.id) {
            comment.parentId = Number(comment.parent.id);
            console.log(`从parent对象设置评论ID ${comment.id} 的parentId=${comment.parentId}`);
          } else if (comment.parent_id !== undefined && comment.parent_id !== null) {
            comment.parentId = Number(comment.parent_id);
            console.log(`从parent_id设置评论ID ${comment.id} 的parentId=${comment.parentId}`);
          } else if (comment.parentId !== undefined && comment.parentId !== null) {
            comment.parentId = Number(comment.parentId);
            console.log(`确认评论ID ${comment.id} 的parentId=${comment.parentId}`);
          }
          
          return comment;
        });
        
        // 为了便于排错，打印所有评论的parentId状态
        console.log('处理后评论的parentId状态:');
        allComments.forEach(comment => {
          console.log(`评论ID: ${comment.id}, parentId: ${comment.parentId || 'null'}, 内容: ${comment.content.substring(0, 15)}...`);
        });
        
        // 更新评论列表
        comments.value = allComments;
        
        // 检查并记录父子关系
        console.log('更新后的评论列表长度:', comments.value.length);
        console.log('根评论数量:', rootComments.value.length);
        
        // 子评论的调试信息
        const childCommentsCount = comments.value.filter(c => c.parentId !== undefined && c.parentId !== null && c.parentId !== 0).length;
        console.log('子评论总数:', childCommentsCount);
        
        // 如果根评论少于总评论数，但又没找到子评论，可能是数据结构问题
        if (childCommentsCount === 0 && rootComments.value.length < comments.value.length) {
          console.warn('警告: 存在不是根评论也不是子评论的评论，可能是数据结构问题');
        }
        
        // 检查每个根评论的子评论
        rootComments.value.forEach(rootComment => {
          const children = getChildComments(rootComment.id);
          if (children.length > 0) {
            console.log(`根评论ID ${rootComment.id} 有 ${children.length} 个子评论`);
          }
        });
        
        // 如果还是没有找到子评论，尝试手动加载
        if (childCommentsCount === 0 && rootComments.value.length > 0) {
          console.log('没有发现子评论，尝试手动获取子评论');
          
          // 为每个根评论获取子评论
          const rootIds = rootComments.value.map(comment => comment.id);
          for (const rootId of rootIds) {
            try {
              console.log(`获取评论ID ${rootId} 的子评论`);
              const childResult = await commentStore.fetchChildComments(rootId);
              console.log(`获取到评论ID ${rootId} 的子评论:`, childResult);
              
              // 如果获取到了新的子评论，手动设置parentId
              if (childResult && childResult.length > 0) {
                childResult.forEach(child => {
                  child.parentId = Number(rootId);
                  // 检查comments中是否已有该评论
                  const existingIndex = comments.value.findIndex(c => c.id === child.id);
                  if (existingIndex >= 0) {
                    // 更新现有评论
                    comments.value[existingIndex].parentId = Number(rootId);
                  } else {
                    // 添加新评论
                    comments.value.push(child);
                  }
                });
              }
            } catch (error) {
              console.error(`获取评论ID ${rootId} 的子评论失败:`, error);
            }
          }
          
          // 再次检查子评论
          const newChildCount = comments.value.filter(c => c.parentId !== undefined && c.parentId !== null && c.parentId !== 0).length;
          console.log('获取子评论后，子评论数量:', newChildCount);
        }
      } catch (error) {
        console.error('获取评论失败:', error);
        ElMessage.error('获取评论失败，请稍后再试');
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
    
    // 处理根评论页码变化
    const handleRootCommentsPageChange = (page) => {
      console.log(`根评论分页：切换到第 ${page} 页`);
      rootCommentsCurrentPage.value = page;
      // 滚动到评论区顶部
      const commentSection = document.querySelector('.comment-section');
      if (commentSection) {
        commentSection.scrollIntoView({ behavior: 'smooth' });
      }
    };
    
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
    
    // 处理API分页变化（从后端获取更多评论）
    const handlePageChange = (page) => {
      currentPage.value = page;
      fetchComments();
    };
    
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
      getUserAvatar,
      rootCommentsPageSize,
      rootCommentsCurrentPage,
      displayedRootComments,
      handleRootCommentsPageChange
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