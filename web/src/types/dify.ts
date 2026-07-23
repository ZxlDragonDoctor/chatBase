import type { RetrieverResource as RetrieverResourceType } from './internal'

export type RetrieverResource = RetrieverResourceType

export type DifyChatResponse = {
  conversationId?: string
  answer?: string
  retrieverResources?: RetrieverResource[]
}

export type ChatFileInfo = {
  type: 'image' | 'document' | 'audio' | 'video' | string
  transfer_method: 'remote_url' | 'local_file'
  url?: string
  upload_file_id?: string
}

export type DifyFileUploadResponse = {
  id?: string
  name?: string
  size?: number
  extension?: string
  mimeType?: string
  createdAt?: number | string
}

export type FileUploadResult = {
  fileName: string
  success: boolean
  message: string
  difyFileId?: string
}

export type BatchUploadResponse = {
  totalCount: number
  successCount: number
  failedCount: number
  results: FileUploadResult[]
}

