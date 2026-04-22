import { api } from './client'

export interface FeedbackStats {
  thumbsUpDaily: Record<string, number>
  thumbsDownDaily: Record<string, number>
  totalThumbsUp: number
  totalThumbsDown: number
  positiveRate: number
}

export async function fetchFeedbackDailyStats(days: number = 7): Promise<FeedbackStats> {
  const resp = await api.get<FeedbackStats>('/statistics/feedback/daily', { params: { days } })
  return resp.data
}

export async function fetchFeedbackOverview(): Promise<FeedbackStats> {
  const resp = await api.get<FeedbackStats>('/statistics/feedback/overview')
  return resp.data
}