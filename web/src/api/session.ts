import { api } from './client'

export type ChatSession = {
  id: number
  sessionId: string
  userId: string
  channel: string
  title?: string
  difyConversationId?: string
  messageCount: number
  lastMessageTime?: string
  status: boolean
  createTime: string
}

export type ChatMessage = {
  id: number
  sessionId?: string
  conversationId: string
  userId: string
  query: string
  answer?: string
  tokens?: number
  latencyMs?: number
  status: boolean
  createTime: string
}

export type PageResponse<T> = {
  records: T[]
  total: number
  pages: number
  current: number
  size: number
}

export async function createSession(userId: string, channel: string = 'web'): Promise<ChatSession> {
  const resp = await api.post<ChatSession>('/chat/session/create', null, {
    params: { userId, channel }
  })
  return resp.data
}

export async function listSessions(userId: string, channel: string = 'web', pageNum: number = 1, pageSize: number = 20): Promise<PageResponse<ChatSession>> {
  const resp = await api.get<PageResponse<ChatSession>>('/chat/session/list', {
    params: { userId, channel, pageNum, pageSize }
  })
  return resp.data
}

export async function getSession(sessionId: string): Promise<ChatSession> {
  const resp = await api.get<ChatSession>(`/chat/session/${sessionId}`)
  return resp.data
}

export async function getSessionMessages(sessionId: string): Promise<ChatMessage[]> {
  const resp = await api.get<ChatMessage[]>(`/chat/session/${sessionId}/messages`)
  return resp.data
}

export async function deleteSession(sessionId: string): Promise<{ success: boolean; message: string }> {
  const resp = await api.delete<{ success: boolean; message: string }>(`/chat/session/${sessionId}`)
  return resp.data
}