import { api } from './client';
export async function webChat(text, userId) {
    const resp = await api.post('/chat/web', { text, userId });
    return resp.data;
}
