import { api } from './client';
export async function fetchFeedbackDailyStats(days = 7) {
    const resp = await api.get('/statistics/feedback/daily', { params: { days } });
    return resp.data;
}
export async function fetchFeedbackOverview() {
    const resp = await api.get('/statistics/feedback/overview');
    return resp.data;
}
