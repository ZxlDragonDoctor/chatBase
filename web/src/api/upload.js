import { api } from './client';
export async function uploadFile(file, user) {
    const form = new FormData();
    form.append('file', file);
    form.append('user', user);
    const resp = await api.post('/chat/v1/files/upload', form, {
        headers: { 'Content-Type': 'multipart/form-data' },
    });
    return resp.data;
}
