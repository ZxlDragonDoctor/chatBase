import { api } from './client';
export async function webChat(text, userId, files) {
    const resp = await api.post('/chat/web', { text, userId, files: files || [] });
    return resp.data;
}
export async function webChatWithSession(sessionId, text, userId, files, appId) {
    const resp = await api.post('/chat/web/session', { sessionId, text, userId, files: files || [], appId });
    return resp.data;
}
