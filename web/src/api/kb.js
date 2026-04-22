import { api } from './client';
export async function fetchKbPage(params) {
    const resp = await api.get('/kb/page', { params });
    return resp.data;
}
export async function createKb(data) {
    const resp = await api.post('/kb', data);
    return resp.data;
}
export async function updateKb(data) {
    const resp = await api.put('/kb', data);
    return resp.data;
}
export async function deleteKb(id) {
    const resp = await api.delete(`/kb/${id}`);
    return resp.data;
}
export async function syncKb(id) {
    const resp = await api.post(`/kb/${id}/sync`);
    return resp.data;
}
export async function fetchDocumentPage(kbId, params) {
    const resp = await api.get(`/kb/${kbId}/document/page`, { params });
    return resp.data;
}
export async function createDocument(data) {
    const resp = await api.post('/kb/document', data);
    return resp.data;
}
export async function deleteDocument(id) {
    const resp = await api.delete(`/kb/document/${id}`);
    return resp.data;
}
export async function syncDocument(id) {
    const resp = await api.post(`/kb/document/${id}/sync`);
    return resp.data;
}
export async function fetchCategoryTree() {
    const resp = await api.get('/kb/category/tree');
    return resp.data;
}
export async function batchUploadToKb(kbId, files, user) {
    const form = new FormData();
    files.forEach(file => form.append('files', file));
    if (user)
        form.append('user', user);
    const resp = await api.post(`/kb/${kbId}/batch-upload`, form, {
        headers: { 'Content-Type': 'multipart/form-data' },
    });
    return resp.data;
}
export async function syncFromDify() {
    const resp = await api.post('/kb/sync-from-dify');
    return resp.data;
}
export async function listDifyDatasets() {
    const resp = await api.get('/kb/dify/list');
    return resp.data;
}
