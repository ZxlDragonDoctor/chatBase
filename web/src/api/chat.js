import { api } from './client';
export async function webChat(text, userId, sessionId, files) {
    const resp = await api.post('/chat/web', {
        text,
        userId,
        sessionId,
        files: files || [],
    });
    return resp.data;
}
export async function listWebSessions(userId) {
    const resp = await api.get('/chat/web/sessions', { params: { userId } });
    return resp.data;
}
export async function registerWebSession(userId, sessionId, title = '新对话') {
    await api.post('/chat/web/sessions', { userId, sessionId, title });
}
