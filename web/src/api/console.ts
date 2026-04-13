import { api } from './client'
import type { ConsoleOverview, GroupMessagePage, GroupSummary } from '../types/console'

export async function fetchOverview(): Promise<ConsoleOverview> {
  const resp = await api.get<ConsoleOverview>('/console/overview')
  return resp.data
}

export async function fetchGroups(platform: 'all' | 'qq' | 'wecom' | 'wx' = 'all'): Promise<GroupSummary[]> {
  const resp = await api.get<GroupSummary[]>('/console/groups', { params: { platform } })
  return resp.data
}

export async function fetchGroupMessages(params: {
  groupId: string | number | null | undefined
  platform?: string
  page?: number
  size?: number
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
    },
  })
  return resp.data
}
