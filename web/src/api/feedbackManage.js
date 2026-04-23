import { api } from './client';
export async function fetchFeedbackPage(params) {
    const resp = await api.get('/feedback/page', { params });
    return resp.data;
}
export async function fetchFeedbackById(id) {
    const resp = await api.get(`/feedback/${id}`);
    return resp.data;
}
export async function replyFeedback(id, adminId, reply) {
    const resp = await api.post(`/feedback/${id}/reply`, null, {
        params: { adminId, reply }
    });
    return resp.data;
}
export async function updateFeedbackStatus(id, status) {
    const resp = await api.put(`/feedback/${id}/status`, null, {
        params: { status }
    });
    return resp.data;
}
export async function fetchFeedbackStats() {
    const resp = await api.get('/feedback/stats');
    return resp.data;
}
