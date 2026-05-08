import { api } from './client'
import type { BatchUploadResponse } from '../types/dify'

export type KbKnowledgeBase = {
  id: number
  name: string
  description?: string
  categoryId?: number
  difyDatasetId?: string
  sourceType: string
  docCount: number
  chunkCount: number
  status: number
  isPublic?: boolean
  createBy?: string
  createTime: string
}

export type KbDocument = {
  id: number
  knowledgeBaseId: number
  title: string
  content?: string
  fileUrl?: string
  fileName?: string
  difyDocumentId?: string
  difyStatus?: string
  source: string
  syncStatus: number
  status?: number
  createTime: string
}

export type KbCategory = {
  id: number
  parentId: number
  name: string
  icon?: string
  sortOrder: number
  description?: string
  status: number
  createBy?: string
  createTime: string
}

export type PageResponse<T> = {
  records: T[]
  total: number
  pages: number
  current: number
  size: number
}

export async function fetchKbPage(params: {
  categoryId?: number
  name?: string
  pageNum?: number
  pageSize?: number
}): Promise<PageResponse<KbKnowledgeBase>> {
  const resp = await api.get<PageResponse<KbKnowledgeBase>>('/kb/page', { params })
  return resp.data
}

export async function createKb(data: Partial<KbKnowledgeBase>): Promise<boolean> {
  const resp = await api.post<boolean>('/kb', data)
  return resp.data
}

export async function updateKb(data: Partial<KbKnowledgeBase>): Promise<boolean> {
  const resp = await api.put<boolean>('/kb', data)
  return resp.data
}

export async function deleteKb(id: number): Promise<boolean> {
  const resp = await api.delete<boolean>(`/kb/${id}`)
  return resp.data
}

export async function syncKb(id: number): Promise<{ success: boolean; message: string }> {
  const resp = await api.post<{ success: boolean; message: string }>(`/kb/${id}/sync`)
  return resp.data
}

export async function fetchDocumentPage(kbId: number, params: {
  title?: string
  pageNum?: number
  pageSize?: number
}): Promise<PageResponse<KbDocument>> {
  const resp = await api.get<PageResponse<KbDocument>>(`/kb/${kbId}/document/page`, { params })
  return resp.data
}

export async function createDocument(data: Partial<KbDocument>): Promise<boolean> {
  const resp = await api.post<boolean>('/kb/document', data)
  return resp.data
}

export async function deleteDocument(id: number): Promise<boolean> {
  const resp = await api.delete<boolean>(`/kb/document/${id}`)
  return resp.data
}

export async function syncDocument(id: number): Promise<{ success: boolean; message: string }> {
  const resp = await api.post<{ success: boolean; message: string }>(`/kb/document/${id}/sync`)
  return resp.data
}

export async function fetchCategoryTree(): Promise<KbCategory[]> {
  const resp = await api.get<KbCategory[]>('/kb/category/tree')
  return resp.data
}

export async function batchUploadToKb(kbId: number, files: File[], user?: string): Promise<BatchUploadResponse> {
  const form = new FormData()
  files.forEach(file => form.append('files', file))
  if (user) form.append('user', user)
  const resp = await api.post<BatchUploadResponse>(`/kb/${kbId}/batch-upload`, form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return resp.data
}

export async function syncFromDify(): Promise<{ success: boolean; count: number; message: string }> {
  const resp = await api.post<{ success: boolean; count: number; message: string }>('/kb/sync-from-dify')
  return resp.data
}

export async function listDifyDatasets(): Promise<{ id: string; name: string; description: string; documentCount: number }[]> {
  const resp = await api.get<{ id: string; name: string; description: string; documentCount: number }[]>('/kb/dify/list')
  return resp.data
}

export async function linkCategoryToKb(kbId: number, categoryId: number): Promise<void> {
  await api.post(`/kb/${kbId}/link-category`, { categoryId })
}

export async function unlinkCategoryFromKb(kbId: number, mappingId: number): Promise<void> {
  await api.delete(`/kb/${kbId}/link-category/${mappingId}`)
}