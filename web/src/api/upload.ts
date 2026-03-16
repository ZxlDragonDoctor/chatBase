import { api } from './client'
import type { DifyFileUploadResponse } from '../types/dify'

export async function uploadFile(file: File, user: string): Promise<DifyFileUploadResponse> {
  const form = new FormData()
  form.append('file', file)
  form.append('user', user)
  const resp = await api.post<DifyFileUploadResponse>('/chat/v1/files/upload', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return resp.data
}

