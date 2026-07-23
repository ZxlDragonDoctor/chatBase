import { api } from './client'

export interface FaqItem {
  id: number
  knowledgeBaseId: number
  categoryId: number
  question: string
  answer: string
  keywords: string
  hitCount: number
  satisfaction: number
  similarQuestions: string
  status: boolean
  priority: number
  createTime: string
  updateTime: string
}

export interface FaqStats {
  total: number
  active: number
  inactive: number
  topFaqs: FaqItem[]
}

export interface HotQuestion {
  question: string
  count: number
}

export async function getFaqPage(knowledgeBaseId?: number, question?: string, pageNum: number = 1, pageSize: number = 10): Promise<{ records: FaqItem[]; total: number }> {
  const resp = await api.get('/kb/conversation/faq/page', { params: { knowledgeBaseId, question, pageNum, pageSize } })
  return resp.data
}

export async function createFaq(faq: Partial<FaqItem>): Promise<boolean> {
  const resp = await api.post('/kb/conversation/faq', faq)
  return resp.data
}

export async function updateFaq(faq: Partial<FaqItem>): Promise<boolean> {
  const resp = await api.put('/kb/conversation/faq', faq)
  return resp.data
}

export async function deleteFaq(id: number): Promise<boolean> {
  const resp = await api.delete(`/kb/conversation/faq/${id}`)
  return resp.data
}

export async function extractFaqFromConversations(knowledgeBaseId: number = 1, minCount: number = 3, days: number = 30): Promise<{ success: boolean; count: number; message: string }> {
  const resp = await api.post('/kb/conversation/faq/extract', null, { params: { knowledgeBaseId, minCount, days } })
  return resp.data
}

export async function getHotQuestions(days: number = 30, limit: number = 20): Promise<HotQuestion[]> {
  const resp = await api.get('/kb/conversation/faq/hot-questions', { params: { days, limit } })
  return resp.data
}

export async function getFaqStats(): Promise<FaqStats> {
  const resp = await api.get('/kb/conversation/faq/stats')
  return resp.data
}