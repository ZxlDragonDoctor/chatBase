import { api } from './client';
export async function submitFeedbackForm(params) {
    const resp = await api.post('/feedback/submit', null, { params });
    return resp.data;
}
export async function getUserFeedbackPage(userId, pageNum = 1, pageSize = 10) {
    const resp = await api.get(`/feedback/user/${userId}`, { params: { pageNum, pageSize } });
    return resp.data;
}
export async function getFeedbackPage(status, pageNum = 1, pageSize = 10) {
    const resp = await api.get('/feedback/page', { params: { status, pageNum, pageSize } });
    return resp.data;
}
export async function getFeedbackById(id) {
    const resp = await api.get(`/feedback/${id}`);
    return resp.data;
}
export async function replyFeedback(id, adminId, reply) {
    const resp = await api.post(`/feedback/${id}/reply`, null, { params: { adminId, reply } });
    return resp.data;
}
export async function getFeedbackStats() {
    const resp = await api.get('/feedback/stats');
    return resp.data;
}
