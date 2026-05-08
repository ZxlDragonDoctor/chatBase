import { api } from './client'
import type {
  TokenStatistics,
  GroupActive,
  KeywordHot,
  ConversationStatistics,
  SystemOverview,
} from '../types/statistics'

export async function fetchTokenDaily(days: number = 7, scope: string = 'all'): Promise<TokenStatistics> {
  const resp = await api.get<TokenStatistics>('/statistics/token/daily', { params: { days, scope } })
  return resp.data
}

export async function fetchTokenTotal(scope: string = 'all'): Promise<TokenStatistics> {
  const resp = await api.get<TokenStatistics>('/statistics/token/total', { params: { scope } })
  return resp.data
}

export async function fetchGroupActive(platform: string = 'all', limit: number = 10, scope: string = 'all'): Promise<GroupActive> {
  const resp = await api.get<GroupActive>('/statistics/group/active', { params: { platform, limit, scope } })
  return resp.data
}

export async function fetchHotKeywords(
  platform: string = 'all',
  groupId?: string,
  limit: number = 20,
  scope: string = 'all'
): Promise<KeywordHot> {
  const resp = await api.get<KeywordHot>('/statistics/group/hot-keywords', {
    params: { platform, groupId, limit, scope },
  })
  return resp.data
}

export async function fetchConversationOverview(days: number = 7, scope: string = 'all'): Promise<ConversationStatistics> {
  const resp = await api.get<ConversationStatistics>('/statistics/conversation/overview', { params: { days, scope } })
  return resp.data
}

export async function fetchConversationTrend(days: number = 30, scope: string = 'all'): Promise<ConversationStatistics> {
  const resp = await api.get<ConversationStatistics>('/statistics/conversation/trend', { params: { days, scope } })
  return resp.data
}

export async function fetchSystemOverview(scope: string = 'all'): Promise<SystemOverview> {
  const resp = await api.get<SystemOverview>('/statistics/system/overview', { params: { scope } })
  return resp.data
}

export async function fetchKeywordCloud(
  source: string = 'all',
  days: number = 30,
  limit: number = 50
): Promise<KeywordHot> {
  const resp = await api.get<KeywordHot>('/statistics/keyword/cloud', {
    params: { source, days, limit },
  })
  return resp.data
}

export async function syncKeywordsFromMessages(
  days: number = 30
): Promise<{ success: boolean; messageCount: number; conversationCount: number; totalCount: number; message: string }> {
  const resp = await api.post('/statistics/keyword/batch-extract', null, {
    params: { days },
  })
  return resp.data
}

export interface TokenChartData {
  dates: string[]
  tokens: number[]
  conversations: number[]
  totalTokens: number
  totalConversations: number
  avgTokens: number
}

export interface TokenMonthlyData extends TokenChartData {
  projectedMonthlyTokens: number
  monthProgress: number
}

export async function fetchTokenChartData(days: number = 7, scope: string = 'all'): Promise<TokenChartData> {
  const resp = await api.get<TokenChartData>('/statistics/token/chart', { params: { days, scope } })
  return resp.data
}

export async function fetchTokenMonthlyData(scope: string = 'all'): Promise<TokenMonthlyData> {
  const resp = await api.get<TokenMonthlyData>('/statistics/token/monthly', { params: { scope } })
  return resp.data
}

export async function aggregateStatistics(days: number = 7): Promise<{ success: boolean; message: string }> {
  const resp = await api.post('/statistics/aggregate', null, { params: { days } })
  return resp.data
}

export interface CostChartData {
  dates: string[]
  costs: number[]
  promptTokens: number[]
  completionTokens: number[]
  totalCost: number
  totalPromptTokens: number
  totalCompletionTokens: number
}

export interface CostMonthlyData {
  dates: string[]
  costs: number[]
  totalCost: number
  monthProgress: number
}

export async function fetchCostChartData(days: number = 7, scope: string = 'all'): Promise<CostChartData> {
  const resp = await api.get<CostChartData>('/statistics/cost/chart', { params: { days, scope } })
  return resp.data
}

export async function fetchCostMonthlyData(scope: string = 'all'): Promise<CostMonthlyData> {
  const resp = await api.get<CostMonthlyData>('/statistics/cost/monthly', { params: { scope } })
  return resp.data
}