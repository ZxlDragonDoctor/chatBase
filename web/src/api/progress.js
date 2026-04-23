import { api } from './client';
export async function getUploadProgress(taskId) {
    const resp = await api.get(`/upload/progress/${taskId}`);
    return resp.data;
}
export function subscribeUploadProgress(taskId, onProgress, onComplete, onError) {
    const token = localStorage.getItem('chatbase_token');
    let url = `${api.defaults.baseURL}/upload/progress/${taskId}/sse`;
    if (token) {
        url += `?token=${encodeURIComponent(token)}`;
    }
    const eventSource = new EventSource(url);
    eventSource.addEventListener('progress', (event) => {
        try {
            const progress = JSON.parse(event.data);
            onProgress(progress);
        }
        catch (e) {
            onError('解析进度数据失败');
        }
    });
    eventSource.addEventListener('complete', (event) => {
        try {
            const progress = JSON.parse(event.data);
            onComplete(progress);
            eventSource.close();
        }
        catch (e) {
            onError('解析完成数据失败');
        }
    });
    eventSource.addEventListener('error', (event) => {
        const msg = event.data || '连接错误';
        onError(msg);
        eventSource.close();
    });
    eventSource.onerror = () => {
        onError('SSE连接失败');
        eventSource.close();
    };
    return eventSource;
}
