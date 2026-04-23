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
  conversationId: number | null
  userId: string
  rating: number
  feedbackType: string | null
  feedbackContent: string | null
  adminReply: string | null
  adminId: number | null
  replyTime: string | null
  status: boolean
  createTime: string
  updateTime: string | null
}

export interface FeedbackStats {
  total: number
  pending: number
  processed: number
  avgRating: number
  typeCounts: Record<string, number>
}

export interface PageResponse<T> {
  records: T[]
  total: number
  pages: number
  current: number
  size: number
}

export async function submitFeedbackForm(params: FeedbackSubmitParams): Promise<{ success: boolean; id: number; message: string }> {
  const resp = await api.post('/feedback/submit', null, { params })
  return resp.data
}

export async function getUserFeedbackPage(userId: string, pageNum: number = 1, pageSize: number = 10): Promise<PageResponse<FeedbackItem>> {
  const resp = await api.get<PageResponse<FeedbackItem>>(`/feedback/user/${userId}`, { params: { pageNum, pageSize } })
  return resp.data
}

export async function getFeedbackPage(status?: number, pageNum: number = 1, pageSize: number = 10): Promise<PageResponse<FeedbackItem>> {
  const resp = await api.get<PageResponse<FeedbackItem>>('/feedback/page', { params: { status, pageNum, pageSize } })
  return resp.data
}

export async function getFeedbackById(id: number): Promise<FeedbackItem> {
  const resp = await api.get<FeedbackItem>(`/feedback/${id}`)
  return resp.data
}

export async function replyFeedback(id: number, adminId: number, reply: string): Promise<{ success: boolean; message: string }> {
  const resp = await api.post<{ success: boolean; message: string }>(`/feedback/${id}/reply`, null, { params: { adminId, reply } })
  return resp.data
}

export async function getFeedbackStats(): Promise<FeedbackStats> {
  const resp = await api.get<FeedbackStats>('/feedback/stats')
  return resp.data
}