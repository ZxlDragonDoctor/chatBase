import { api } from './client'
import type { ChatFileInfo, DifyChatResponse } from '../types/dify'

export async function webChat(text: string, userId: string, files?: ChatFileInfo[]): Promise<DifyChatResponse> {
  const resp = await api.post<DifyChatResponse>('/chat/web', { text, userId, files: files || [] })
  return resp.data
}

