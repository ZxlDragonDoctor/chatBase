import { api } from './client';
export async function listBots() {
    const resp = await api.get('/bot/list');
    return resp.data;
}
