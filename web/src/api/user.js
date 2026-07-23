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
export async function updateUserProfile(username, data) {
    const resp = await api.put('/user/info', null, {
        params: {
            username,
            nickname: data.nickname || undefined,
            email: data.email || undefined,
            phone: data.phone || undefined
        }
    });
    return resp.data;
}
export async function changePassword(username, oldPassword, newPassword) {
    const resp = await api.post('/user/change-password', null, {
        params: {
            username,
            oldPassword,
            newPassword
        }
    });
    return resp.data;
}
export async function uploadAvatar(username, file) {
    const formData = new FormData();
    formData.append('username', username);
    formData.append('file', file);
    const resp = await api.post('/user/avatar/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
    });
    return resp.data;
}
export async function logout() {
    await api.post('/user/logout');
}
