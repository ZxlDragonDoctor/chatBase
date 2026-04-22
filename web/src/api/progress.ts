import { api } from './client'

export type UploadProgress = {
  taskId: string
  totalCount: number
  completedCount: number
  successCount: number
  failedCount: number
  currentFile: string | null
  status: string
  fileProgresses: FileProgress[]
  progressPercent: number
  createTime: string
  updateTime: string
}

export type FileProgress = {
  fileName: string
  status: string
  message: string
  difyFileId: string | null
}

export async function getUploadProgress(taskId: string): Promise<UploadProgress> {
  const resp = await api.get<UploadProgress>(`/upload/progress/${taskId}`)
  return resp.data
}

export function subscribeUploadProgress(
  taskId: string,
  onProgress: (progress: UploadProgress) => void,
  onComplete: (progress: UploadProgress) => void,
  onError: (error: string) => void
): EventSource {
  const url = `${api.defaults.baseURL}/upload/progress/${taskId}/sse`
  const eventSource = new EventSource(url)
  
  eventSource.addEventListener('progress', (event) => {
    try {
      const progress = JSON.parse(event.data) as UploadProgress
      onProgress(progress)
    } catch (e) {
      onError('解析进度数据失败')
    }
  })
  
  eventSource.addEventListener('complete', (event) => {
    try {
      const progress = JSON.parse(event.data) as UploadProgress
      onComplete(progress)
      eventSource.close()
    } catch (e) {
      onError('解析完成数据失败')
    }
  })
  
  eventSource.addEventListener('error', (event: Event) => {
    const msg = (event as MessageEvent).data || '连接错误'
    onError(msg)
    eventSource.close()
  })
  
  eventSource.onerror = () => {
    onError('SSE连接失败')
    eventSource.close()
  }
  
  return eventSource
}