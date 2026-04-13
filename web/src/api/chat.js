import { api } from './client';
export async function webChat(text, userId, files) {
    const resp = await api.post('/chat/web', { text, userId, files: files || [] });
    return resp.data;
}
