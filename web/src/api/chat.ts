import { api } from './client'
import type { ChatFileInfo, DifyChatResponse } from '../types/dify'

export async function webChat(text: string, userId: string, files?: ChatFileInfo[]): Promise<DifyChatResponse> {
  const resp = await api.post<DifyChatResponse>('/chat/web', { text, userId, files: files || [] })
  return resp.data
}

export async function webChatWithSession(sessionId: string, text: string, userId: string, files?: ChatFileInfo[], appId?: number): Promise<DifyChatResponse> {
  const resp = await api.post<DifyChatResponse>('/chat/web/session', { sessionId, text, userId, files: files || [], appId })
  return resp.data
}

