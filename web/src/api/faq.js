import { api } from './client';
export async function getFaqPage(knowledgeBaseId, question, pageNum = 1, pageSize = 10) {
    const resp = await api.get('/kb/conversation/faq/page', { params: { knowledgeBaseId, question, pageNum, pageSize } });
    return resp.data;
}
export async function createFaq(faq) {
    const resp = await api.post('/kb/conversation/faq', faq);
    return resp.data;
}
export async function updateFaq(faq) {
    const resp = await api.put('/kb/conversation/faq', faq);
    return resp.data;
}
export async function deleteFaq(id) {
    const resp = await api.delete(`/kb/conversation/faq/${id}`);
    return resp.data;
}
export async function extractFaqFromConversations(knowledgeBaseId = 1, minCount = 3, days = 30) {
    const resp = await api.post('/kb/conversation/faq/extract', null, { params: { knowledgeBaseId, minCount, days } });
    return resp.data;
}
export async function getHotQuestions(days = 30, limit = 20) {
    const resp = await api.get('/kb/conversation/faq/hot-questions', { params: { days, limit } });
    return resp.data;
}
export async function getFaqStats() {
    const resp = await api.get('/kb/conversation/faq/stats');
    return resp.data;
}
