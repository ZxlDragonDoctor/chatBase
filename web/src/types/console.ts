export type BotStatus = {
  qq: {
    enabled: boolean
    selfId: number
    wsPort: number
    httpConfigured: boolean
    httpBaseUrlPreview: string | null
  }
  wecom: {
    callbackPath: string
    note: string
  }
  wx: {
    enabled: boolean
    tokenConfigured: boolean
    online: boolean
    nickname: string
    baseUrlPreview: string | null
  }
}

export type ConsoleOverview = {
  totalMessages: number
  distinctGroups: number
  messageCountByPlatform: Record<string, number>
  groupCountByPlatform: Record<string, number>
  bots: BotStatus
}

export type GroupSummary = {
  id: number
  platform: string
  groupId: string
  groupName?: string | null
  messageCount: number
  lastMessageTime: string | null
  appId?: number | null
  appName?: string | null
  createdBy?: string | null
}

export type GroupMessageItem = {
  id: number
  platform: string
  groupId: string
  userId: string
  messageType: string
  rawMessage: string
  messageTime: string | null
  synced: boolean | null
}

export type GroupMessagePage = {
  records: GroupMessageItem[]
  total: number
  page: number
  size: number
}
