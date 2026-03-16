import { api } from './client'
import type { DifyChatResponse } from '../types/dify'

export async function webChat(text: string, userId: string): Promise<DifyChatResponse> {
  const resp = await api.post<DifyChatResponse>('/chat/web', { text, userId })
  return resp.data
}

