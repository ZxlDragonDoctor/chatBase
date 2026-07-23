import { marked } from 'marked'

export interface ParsedContent {
  thinking: string | null
  content: string
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
  
  return marked.parse(text) as string
}

export function renderMessage(text: string): { thinkingHtml: string | null; contentHtml: string } {
  const { thinking, content } = parseThinkTag(text)
  
  const thinkingHtml = thinking ? renderMarkdown(thinking) : null
  const contentHtml = renderMarkdown(content)
  
  return { thinkingHtml, contentHtml }
}