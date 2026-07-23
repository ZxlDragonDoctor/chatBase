import { api } from './client'

export interface BotInfo {
  platform: 'qq' | 'wecom' | 'wx'
  name: string
  botId: string | null
  online: boolean
  groupCount: number
  todayMessages: number
  totalMessages: number
  lastActiveTime: string | null
}

export async function listBots(): Promise<BotInfo[]> {
  const resp = await api.get<BotInfo[]>('/bot/list')
  return resp.data
}
