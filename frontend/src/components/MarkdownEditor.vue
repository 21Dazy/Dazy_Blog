<template>
  <div class="markdown-editor">
    <div class="editor-toolbar">
      <div class="toolbar-left">
        <el-button-group>
          <el-button type="primary" size="small" :plain="mode !== 'edit'" @click="switchMode('edit')">
            <i class="el-icon-edit"></i> 编辑
          </el-button>
          <el-button type="primary" size="small" :plain="mode !== 'split'" @click="switchMode('split')">
            <i class="el-icon-s-operation"></i> 分屏
          </el-button>
          <el-button type="primary" size="small" :plain="mode !== 'preview'" @click="switchMode('preview')">
            <i class="el-icon-view"></i> 预览
          </el-button>
        </el-button-group>
      </div>
      
      <div class="toolbar-right">
        <span class="md-tips">支持 Markdown 语法</span>
      </div>
    </div>
    
    <div class="editor-content" :class="mode">
      <div class="edit-area" v-show="mode !== 'preview'">
        <textarea
          ref="textarea"
          v-model="content"
          @input="handleInput"
          placeholder="使用 Markdown 语法编写你的博客..."
        ></textarea>
      </div>
      
      <div class="preview-area" v-show="mode !== 'edit'">
        <div class="markdown-preview" v-html="previewContent"></div>
      </div>
    </div>
    
    <div class="editor-footer">
      <div class="upload-btn">
        <el-upload
          action=""
          :http-request="customUploadRequest"
          :multiple="true"
          :show-file-list="false"
          accept=".jpg,.jpeg,.png,.gif,.webp,.mp4,.mp3,.pdf"
        >
          <el-button size="small" type="primary">
            <i class="el-icon-upload"></i> 上传文件
          </el-button>
        </el-upload>
      </div>
      
      <div class="editor-status">
        <span>{{ status }}</span>
        <span v-if="mode !== 'preview'">{{ content.length }} 字符</span>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, watchEffect, onMounted, nextTick } from 'vue'
import { marked } from 'marked'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'
import { ElMessage, ElLoading } from 'element-plus'
import axios from 'axios'

// 配置marked解析器
marked.setOptions({
  renderer: new marked.Renderer(),
  highlight: function(code, lang) {
    const language = hljs.getLanguage(lang) ? lang : 'plaintext';
    return hljs.highlight(code, { language }).value;
  },
  langPrefix: 'hljs language-',
  gfm: true,
  breaks: true,
  sanitize: false,
  smartLists: true,
  smartypants: false,
  xhtml: false
})

export default {
  name: 'MarkdownEditor',
  props: {
    value: {
      type: String,
      default: ''
    },
    height: {
      type: String,
      default: '400px'
    },
    uploadUrl: {
      type: String,
      required: true
    },
    uploadHeaders: {
      type: Object,
      default: () => ({})
    },
    placeholder: {
      type: String,
      default: '使用 Markdown 语法编写你的博客...'
    }
  },
  emits: ['update:value', 'upload-success', 'upload-error'],
  setup(props, { emit }) {
    // 编辑模式：edit, split, preview
    const mode = ref('edit')
    // 编辑内容
    const content = ref(props.value || '')
    // 状态信息
    const status = ref('准备就绪')
    // textarea引用
    const textarea = ref(null)
    
    // 预览内容
    const previewContent = computed(() => {
      return marked(content.value)
    })
    
    // 切换编辑模式
    const switchMode = (newMode) => {
      mode.value = newMode
    }
    
    // 处理输入事件
    const handleInput = () => {
      emit('update:value', content.value)
    }
    
    // 自定义上传文件逻辑
    const customUploadRequest = async (options) => {
      const { file } = options
      const isImage = file.type.startsWith('image/')
      const isVideo = file.type.startsWith('video/')
      const isAudio = file.type.startsWith('audio/')
      const isPdf = file.type === 'application/pdf'
      
      // 检查文件类型
      if (!isImage && !isVideo && !isAudio && !isPdf) {
        ElMessage.error('仅支持图片、视频、音频和PDF文件！')
        return
      }
      
      // 检查文件大小
      const isLt10M = file.size / 1024 / 1024 < 10
      if (!isLt10M) {
        ElMessage.error('文件大小不能超过10MB！')
        return
      }
      
      // 创建FormData对象
      const formData = new FormData()
      formData.append('file', file)
      
      // 显示加载指示器
      const loadingInstance = ElLoading.service({
        text: '正在上传文件...',
        background: 'rgba(0, 0, 0, 0.7)'
      })
      
      // 上传文件
      try {
        status.value = '上传中...'
        
        const response = await axios.post(props.uploadUrl, formData, {
          headers: {
            ...props.uploadHeaders,
            'Content-Type': 'multipart/form-data'
          }
        })
        
        loadingInstance.close()
        
        if (response.data && response.data.url) {
          status.value = '上传成功'
          
          // 根据文件类型，生成不同的Markdown语法
          let markdownText = ''
          const fileUrl = "/api" + response.data.url
          
          if (isImage) {
            markdownText = `![${file.name}](${fileUrl})`
          } else if (isVideo) {
            markdownText = `<video src="${fileUrl}" controls></video>`
          } else if (isAudio) {
            markdownText = `<audio src="${fileUrl}" controls></audio>`
          } else if (isPdf) {
            markdownText = `[${file.name}](${fileUrl})`
          }
          
          // 在光标位置插入Markdown
          insertTextAtCursor(markdownText)
          
          // 触发上传成功事件
          emit('upload-success', response.data)
          
          ElMessage.success('文件上传成功')
        } else {
          status.value = '上传失败'
          emit('upload-error', new Error('上传失败，未返回有效的文件URL'))
          ElMessage.error('上传失败，未返回有效的文件URL')
        }
      } catch (error) {
        loadingInstance.close()
        status.value = '上传失败'
        emit('upload-error', error)
        ElMessage.error('文件上传失败：' + (error.message || '未知错误'))
      }
    }
    
    // 在光标位置插入文本
    const insertTextAtCursor = (text) => {
      const textArea = textarea.value
      if (!textArea) return
      
      const startPos = textArea.selectionStart
      const endPos = textArea.selectionEnd
      const scrollTop = textArea.scrollTop
      
      content.value = content.value.substring(0, startPos) + text + content.value.substring(endPos)
      
      // 更新组件值
      emit('update:value', content.value)
      
      // 恢复光标位置
      nextTick(() => {
        textArea.focus()
        textArea.selectionStart = startPos + text.length
        textArea.selectionEnd = startPos + text.length
        textArea.scrollTop = scrollTop
      })
    }
    
    // 添加行内格式
    const addInlineFormat = (format, placeholder = '文本') => {
      if (mode.value === 'preview') return
      
      const formats = {
        bold: { prefix: '**', suffix: '**' },
        italic: { prefix: '*', suffix: '*' },
        code: { prefix: '`', suffix: '`' },
        link: { prefix: '[', suffix: '](URL)' },
        image: { prefix: '![', suffix: '](URL)' }
      }
      
      if (!formats[format]) return
      
      const { prefix, suffix } = formats[format]
      
      const textArea = textarea.value
      if (!textArea) return
      
      const startPos = textArea.selectionStart
      const endPos = textArea.selectionEnd
      
      const selectedText = content.value.substring(startPos, endPos)
      const replacement = selectedText.length > 0 ? selectedText : placeholder
      const text = prefix + replacement + suffix
      
      insertTextAtCursor(text)
    }
    
    // 监听props.value的变化
    watchEffect(() => {
      content.value = props.value
    })
    
    // 组件挂载后设置textarea的高度
    onMounted(() => {
      if (textarea.value) {
        textarea.value.style.height = props.height
      }
    })
    
    return {
      mode,
      content,
      status,
      textarea,
      previewContent,
      switchMode,
      handleInput,
      customUploadRequest,
      insertTextAtCursor,
      addInlineFormat
    }
  }
}
</script>

<style>
.markdown-editor {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 500px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.05);
  background-color: #ffffff;
}

.editor-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 15px;
  border-bottom: 1px solid #ebeef5;
  background-color: #f9fafc;
  border-radius: 4px 4px 0 0;
}

.toolbar-left {
  display: flex;
  align-items: center;
}

.toolbar-right {
  display: flex;
  align-items: center;
}

.md-tips {
  font-size: 13px;
  color: #909399;
  font-style: italic;
}

.editor-content {
  display: flex;
  flex: 1;
  min-height: 400px;
}

.editor-content.edit .edit-area {
  width: 100%;
}

.editor-content.preview .preview-area {
  width: 100%;
}

.editor-content.split .edit-area,
.editor-content.split .preview-area {
  width: 50%;
}

.edit-area {
  height: 100%;
  border-right: 1px solid #ebeef5;
}

.edit-area textarea {
  width: 100%;
  height: 100%;
  min-height: 400px;
  padding: 20px;
  border: none;
  resize: none;
  outline: none;
  font-family: "SFMono-Regular", Consolas, "Liberation Mono", Menlo, monospace;
  font-size: 15px;
  line-height: 1.7;
  color: #303133;
  background-color: #ffffff;
}

.preview-area {
  padding: 20px;
  overflow-y: auto;
  background-color: #ffffff;
}

.markdown-preview {
  font-size: 15px;
  line-height: 1.7;
  color: #303133;
}

.markdown-preview h1,
.markdown-preview h2,
.markdown-preview h3,
.markdown-preview h4,
.markdown-preview h5,
.markdown-preview h6 {
  margin-top: 24px;
  margin-bottom: 16px;
  font-weight: 600;
  line-height: 1.25;
}

.markdown-preview h1 {
  font-size: 2em;
  padding-bottom: 0.3em;
  border-bottom: 1px solid #eaecef;
}

.markdown-preview h2 {
  font-size: 1.5em;
  padding-bottom: 0.3em;
  border-bottom: 1px solid #eaecef;
}

.markdown-preview h3 {
  font-size: 1.25em;
}

.markdown-preview p {
  margin-top: 0;
  margin-bottom: 16px;
}

.markdown-preview blockquote {
  padding: 0 1em;
  color: #6a737d;
  border-left: 0.25em solid #dfe2e5;
  margin: 0 0 16px 0;
}

.markdown-preview pre {
  padding: 16px;
  overflow: auto;
  font-size: 85%;
  line-height: 1.45;
  background-color: #f6f8fa;
  border-radius: 3px;
}

.markdown-preview code {
  padding: 0.2em 0.4em;
  margin: 0;
  font-size: 85%;
  background-color: rgba(27, 31, 35, 0.05);
  border-radius: 3px;
}

.markdown-preview pre code {
  padding: 0;
  margin: 0;
  border-radius: 0;
  background-color: transparent;
}

.markdown-preview img {
  max-width: 100%;
  box-sizing: content-box;
  display: block;
  margin: 15px 0;
  border-radius: 4px;
}

.markdown-preview table {
  border-collapse: collapse;
  width: 100%;
  margin-bottom: 16px;
  border-radius: 4px;
  overflow: hidden;
}

.markdown-preview table th,
.markdown-preview table td {
  padding: 8px 16px;
  border: 1px solid #ebeef5;
}

.markdown-preview table th {
  background-color: #f5f7fa;
  font-weight: 600;
}

.markdown-preview table tr {
  background-color: #fff;
  border-top: 1px solid #ebeef5;
}

.markdown-preview table tr:nth-child(2n) {
  background-color: #fafafa;
}

.editor-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 15px;
  border-top: 1px solid #ebeef5;
  background-color: #f9fafc;
  border-radius: 0 0 4px 4px;
}

.editor-status {
  font-size: 13px;
  color: #909399;
}

@media (max-width: 768px) {
  .editor-content.split .edit-area,
  .editor-content.split .preview-area {
    width: 100%;
  }
  
  .editor-content.split {
    flex-direction: column;
  }
  
  .edit-area {
    border-right: none;
    border-bottom: 1px solid #ebeef5;
  }
}
</style>