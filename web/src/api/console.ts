import { api } from './client'
import type { ConsoleOverview, ConversationSummary, GroupMessagePage, GroupSummary } from '../types/console'

export async function fetchOverview(): Promise<ConsoleOverview> {
  const resp = await api.get<ConsoleOverview>('/console/overview')
  return resp.data
}

export async function fetchGroups(platform: string = 'all', scope: string = 'all'): Promise<GroupSummary[]> {
  const resp = await api.get<GroupSummary[]>('/console/groups', { params: { platform, scope } })
  return resp.data
}

export async function fetchGroupMessages(params: {
  groupId: string | number | null | undefined
  platform?: string
  page?: number
  size?: number
  keyword?: string
}): Promise<GroupMessagePage> {
  const gid = params.groupId != null && String(params.groupId).trim() !== '' ? String(params.groupId) : ''
  if (!gid) {
    return { records: [], total: 0, page: params.page ?? 0, size: params.size ?? 30 }
  }
  const resp = await api.get<GroupMessagePage>('/console/messages', {
    params: {
      groupId: gid,
      platform: params.platform ?? 'all',
      page: params.page ?? 0,
      size: params.size ?? 30,
      keyword: params.keyword || undefined,
    },
  })
  return resp.data
}

export async function fetchConversations(): Promise<ConversationSummary[]> {
  const resp = await api.get<ConversationSummary[]>('/console/conversations')
  return resp.data
}

export async function fetchPrivateMessages(params: {
  conversationId: string
  page?: number
  size?: number
  keyword?: string
}): Promise<GroupMessagePage> {
  const cid = params.conversationId != null && String(params.conversationId).trim() !== '' ? String(params.conversationId) : ''
  if (!cid) {
    return { records: [], total: 0, page: params.page ?? 0, size: params.size ?? 30 }
  }
  const resp = await api.get<GroupMessagePage>('/console/conversations/messages', {
    params: {
      conversationId: cid,
      page: params.page ?? 0,
      size: params.size ?? 30,
      keyword: params.keyword || undefined,
    },
  })
  return resp.data
}
