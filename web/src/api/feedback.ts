import { api } from './client'

export async function submitFeedback(sessionId: string, messageIndex: number, rating: number, feedbackType?: string, content?: string): Promise<{ success: boolean; message: string; feedback?: number }> {
  try {
    const resp = await api.post('/kb/conversation/feedback', {
      sessionId,
      messageIndex,
      rating,
      feedbackType,
      feedbackContent: content,
    })
    return resp.data
  } catch {
    return { success: false, message: '请求失败' }
  }
}

export async function getFeedbackStatus(sessionId: string): Promise<Record<number, number>> {
  try {
    const resp = await api.get<Record<number, number>>('/kb/conversation/feedback/status', {
      params: { sessionId },
    })
    return resp.data || {}
  } catch {
    return {}
  }
}