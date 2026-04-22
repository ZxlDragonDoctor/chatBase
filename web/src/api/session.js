import { api } from './client';
export async function createSession(userId, channel = 'web') {
    const resp = await api.post('/chat/session/create', null, {
        params: { userId, channel }
    });
    return resp.data;
}
export async function listSessions(userId, channel = 'web', pageNum = 1, pageSize = 20) {
    const resp = await api.get('/chat/session/list', {
        params: { userId, channel, pageNum, pageSize }
    });
    return resp.data;
}
export async function getSession(sessionId) {
    const resp = await api.get(`/chat/session/${sessionId}`);
    return resp.data;
}
export async function getSessionMessages(sessionId) {
    const resp = await api.get(`/chat/session/${sessionId}/messages`);
    return resp.data;
}
export async function deleteSession(sessionId) {
    const resp = await api.delete(`/chat/session/${sessionId}`);
    return resp.data;
}
