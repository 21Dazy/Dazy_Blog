<template>
  <div class="markdown-renderer">
    <div class="markdown-content" v-html="renderedContent"></div>
  </div>
</template>

<script>
import { computed } from 'vue'
import { marked } from 'marked'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'

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
  name: 'MarkdownRenderer',
  props: {
    content: {
      type: String,
      default: ''
    }
  },
  setup(props) {
    // 渲染Markdown内容
    const renderedContent = computed(() => {
      return marked(props.content || '')
    })
    
    return {
      renderedContent
    }
  }
}
</script>

<style scoped>
.markdown-renderer {
  font-size: 16px;
  line-height: 1.6;
  color: #2c3e50;
}

.markdown-content {
  width: 100%;
  overflow-wrap: break-word;
}

.markdown-content h1,
.markdown-content h2,
.markdown-content h3,
.markdown-content h4,
.markdown-content h5,
.markdown-content h6 {
  margin-top: 24px;
  margin-bottom: 16px;
  font-weight: 600;
  line-height: 1.25;
}

.markdown-content h1 {
  font-size: 2em;
  padding-bottom: 0.3em;
  border-bottom: 1px solid #eaecef;
}

.markdown-content h2 {
  font-size: 1.5em;
  padding-bottom: 0.3em;
  border-bottom: 1px solid #eaecef;
}

.markdown-content h3 {
  font-size: 1.25em;
}

.markdown-content p {
  margin-top: 0;
  margin-bottom: 16px;
}

.markdown-content blockquote {
  padding: 0 1em;
  color: #6a737d;
  border-left: 0.25em solid #dfe2e5;
  margin: 0 0 16px 0;
}

.markdown-content ul,
.markdown-content ol {
  padding-left: 2em;
  margin-top: 0;
  margin-bottom: 16px;
}

.markdown-content pre {
  padding: 16px;
  overflow: auto;
  font-size: 85%;
  line-height: 1.45;
  background-color: #f6f8fa;
  border-radius: 3px;
  margin-bottom: 16px;
}

.markdown-content code {
  padding: 0.2em 0.4em;
  margin: 0;
  font-size: 85%;
  background-color: rgba(27, 31, 35, 0.05);
  border-radius: 3px;
}

.markdown-content pre code {
  padding: 0;
  margin: 0;
  border-radius: 0;
  background-color: transparent;
}

.markdown-content img {
  max-width: 100%;
  height: auto;
  display: block;
  margin: 16px 0;
  box-sizing: content-box;
}

.markdown-content table {
  border-collapse: collapse;
  width: 100%;
  margin-bottom: 16px;
}

.markdown-content table th,
.markdown-content table td {
  padding: 6px 13px;
  border: 1px solid #dfe2e5;
}

.markdown-content table tr {
  background-color: #fff;
  border-top: 1px solid #c6cbd1;
}

.markdown-content table tr:nth-child(2n) {
  background-color: #f6f8fa;
}

.markdown-content a {
  color: #0366d6;
  text-decoration: none;
}

.markdown-content a:hover {
  text-decoration: underline;
}

.markdown-content hr {
  height: 0.25em;
  padding: 0;
  margin: 24px 0;
  background-color: #e1e4e8;
  border: 0;
}

/* 适配图片和视频的响应式显示 */
.markdown-content video,
.markdown-content audio {
  max-width: 100%;
  margin: 16px 0;
}
</style> 