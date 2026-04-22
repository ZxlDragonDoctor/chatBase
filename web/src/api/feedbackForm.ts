import { api } from './client'

export interface FeedbackSubmitParams {
  userId: string
  conversationId?: number
  rating: number
  feedbackType?: string
  content?: string
  contact?: string
}

export interface FeedbackItem {
  id: number
  conversationId: number
  userId: string
  rating: number
  feedbackType: string
  feedbackContent: string
  adminReply: string
  adminId: number
  replyTime: string
  status: boolean
  createTime: string
  updateTime: string
}

export interface FeedbackStats {
  total: number
  pending: number
  processed: number
  avgRating: number
  typeCounts: Record<string, number>
}

export async function submitFeedbackForm(params: FeedbackSubmitParams): Promise<{ success: boolean; id: number; message: string }> {
  const resp = await api.post('/feedback/submit', null, { params })
  return resp.data
}

export async function getFeedbackPage(status?: number, pageNum: number = 1, pageSize: number = 10): Promise<{ records: FeedbackItem[]; total: number }> {
  const resp = await api.get('/feedback/page', { params: { status, pageNum, pageSize } })
  return resp.data
}

export async function getFeedbackById(id: number): Promise<FeedbackItem> {
  const resp = await api.get(`/feedback/${id}`)
  return resp.data
}

export async function replyFeedback(id: number, adminId: number, reply: string): Promise<{ success: boolean; message: string }> {
  const resp = await api.post(`/feedback/${id}/reply`, null, { params: { adminId, reply } })
  return resp.data
}

export async function getFeedbackStats(): Promise<FeedbackStats> {
  const resp = await api.get('/feedback/stats')
  return resp.data
}