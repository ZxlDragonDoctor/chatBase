import { api } from './client'

export type KbFeedback = {
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

export type FeedbackStats = {
  total: number
  pending: number
  processed: number
  avgRating: number
  typeStats: Record<string, number>
}

export type PageResponse<T> = {
  records: T[]
  total: number
  pages: number
  current: number
  size: number
}

export async function fetchFeedbackPage(params: {
  status?: number
  pageNum?: number
  pageSize?: number
}): Promise<PageResponse<KbFeedback>> {
  const resp = await api.get<PageResponse<KbFeedback>>('/feedback/page', { params })
  return resp.data
}

export async function fetchFeedbackById(id: number): Promise<KbFeedback> {
  const resp = await api.get<KbFeedback>(`/feedback/${id}`)
  return resp.data
}

export async function replyFeedback(id: number, adminId: number, reply: string): Promise<{ success: boolean; message: string }> {
  const resp = await api.post<{ success: boolean; message: string }>(`/feedback/${id}/reply`, null, {
    params: { adminId, reply }
  })
  return resp.data
}

export async function updateFeedbackStatus(id: number, status: number): Promise<{ success: boolean; message: string }> {
  const resp = await api.put<{ success: boolean; message: string }>(`/feedback/${id}/status`, null, {
    params: { status }
  })
  return resp.data
}

export async function fetchFeedbackStats(): Promise<FeedbackStats> {
  const resp = await api.get<FeedbackStats>('/feedback/stats')
  return resp.data
}