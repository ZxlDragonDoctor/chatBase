import type { RetrieverResource as RetrieverResourceType } from './internal'

export type RetrieverResource = RetrieverResourceType

export type DifyChatResponse = {
  conversationId?: string
  answer?: string
  retrieverResources?: RetrieverResource[]
}

export type ChatFileInfo = {
  type: 'image' | 'document' | 'audio' | 'video' | string
  transferMethod: 'remote_url' | 'local_file'
  url?: string
  uploadFileId?: string
}

export type DifyFileUploadResponse = {
  id?: string
  name?: string
  size?: number
  extension?: string
  mimeType?: string
  createdAt?: number | string
}

