import { marked } from 'marked'

export interface ParsedContent {
  thinking: string | null
  content: string
}

/** 转义 HTML 特殊字符，防止用户输入 / 模型输出中的原始 HTML 被浏览器执行 */
export function escapeHtml(text: string): string {
  if (!text) return ''
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

export function parseThinkTag(text: string): ParsedContent {
  const thinkRegex = /<think>([\s\S]*?)<\/think>/g
  const matches = text.match(thinkRegex)

  let thinking: string | null = null
  let content = text

  if (matches) {
    const thinkingParts: string[] = []
    for (const match of matches) {
      const inner = match.replace(/<think>|<\/think>/g, '').trim()
      if (inner) {
        thinkingParts.push(inner)
      }
    }
    if (thinkingParts.length > 0) {
      thinking = thinkingParts.join('\n\n')
    }
    content = text.replace(thinkRegex, '').trim()
  }

  return { thinking, content }
}

export function renderMarkdown(text: string): string {
  if (!text) return ''

  marked.setOptions({
    breaks: true,
    gfm: true
  })

  // 先转义 HTML，再交给 marked：保留 Markdown 语法，禁用内嵌 HTML
  return marked.parse(escapeHtml(text)) as string
}

export function renderMessage(text: string): { thinkingHtml: string | null; contentHtml: string } {
  const { thinking, content } = parseThinkTag(text)

  const thinkingHtml = thinking ? renderMarkdown(thinking) : null
  const contentHtml = renderMarkdown(content)

  return { thinkingHtml, contentHtml }
}
