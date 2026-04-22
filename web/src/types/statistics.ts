export type TokenStatistics = {
  totalTokens: number
  avgTokensPerConversation: number
  totalConversations: number
  dailyTokens: DailyToken[]
}

export type DailyToken = {
  date: string
  tokens: number
  conversations: number
}

export type GroupActive = {
  topGroups: GroupRankItem[]
  totalGroups: number
  totalMessages: number
}

export type GroupRankItem = {
  platform: string
  groupId: string
  groupName?: string
  messageCount: number
  lastMessageTime?: string
  rank: number
}

export type KeywordHot = {
  keywords: KeywordItem[]
  platform?: string
  groupId?: string
}

export type KeywordItem = {
  keyword: string
  count: number
  rank: number
}

export type ConversationStatistics = {
  totalConversations: number
  successfulConversations: number
  failedConversations: number
  successRate: number
  avgLatencyMs: number
  dailyTrend: DailyConversation[]
}

export type DailyConversation = {
  date: string
  count: number
  successCount: number
  tokens: number
}

export type SystemOverview = {
  totalMessages: number
  totalConversations: number
  totalTokens: number
  activeGroups: number
  activeUsers: number
  knowledgeBases: number
  documents: number
  avgLatencyMs: number
  successRate: number
  bots: BotStatus
}

export type BotStatus = {
  qqEnabled: boolean
  qqSelfId?: string
  wecomEnabled: boolean
  wecomCallbackPath?: string
}