import { api } from './client';
export async function register(username, password, nickname, email, phone) {
    const resp = await api.post('/user/register', { username, password, nickname, email, phone });
    return resp.data;
}
export async function login(username, password) {
    const resp = await api.post('/user/login', { username, password });
    return resp.data;
}
export async function getCurrentUser(username) {
    try {
        const resp = await api.get('/user/info', { params: { username } });
        return resp.data;
    }
    catch {
        return null;
    }
}
export async function logout() {
    await api.post('/user/logout');
}
