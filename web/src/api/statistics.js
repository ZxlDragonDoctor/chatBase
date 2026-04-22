import { api } from './client';
export async function fetchTokenDaily(days = 7) {
    const resp = await api.get('/statistics/token/daily', { params: { days } });
    return resp.data;
}
export async function fetchTokenTotal() {
    const resp = await api.get('/statistics/token/total');
    return resp.data;
}
export async function fetchGroupActive(platform = 'all', limit = 10) {
    const resp = await api.get('/statistics/group/active', { params: { platform, limit } });
    return resp.data;
}
export async function fetchHotKeywords(platform = 'all', groupId, limit = 20) {
    const resp = await api.get('/statistics/group/hot-keywords', {
        params: { platform, groupId, limit },
    });
    return resp.data;
}
export async function fetchConversationOverview(days = 7) {
    const resp = await api.get('/statistics/conversation/overview', { params: { days } });
    return resp.data;
}
export async function fetchConversationTrend(days = 30) {
    const resp = await api.get('/statistics/conversation/trend', { params: { days } });
    return resp.data;
}
export async function fetchSystemOverview() {
    const resp = await api.get('/statistics/system/overview');
    return resp.data;
}
export async function fetchKeywordCloud(source = 'all', days = 30, limit = 50) {
    const resp = await api.get('/statistics/keyword/cloud', {
        params: { source, days, limit },
    });
    return resp.data;
}
export async function syncKeywordsFromMessages(days = 30) {
    const resp = await api.post('/statistics/keyword/batch-extract', null, {
        params: { days },
    });
    return resp.data;
}
export async function fetchTokenChartData(days = 7) {
    const resp = await api.get('/statistics/token/chart', { params: { days } });
    return resp.data;
}
export async function fetchTokenMonthlyData() {
    const resp = await api.get('/statistics/token/monthly');
    return resp.data;
}
export async function aggregateStatistics(days = 7) {
    const resp = await api.post('/statistics/aggregate', null, { params: { days } });
    return resp.data;
}
export async function fetchCostChartData(days = 7) {
    const resp = await api.get('/statistics/cost/chart', { params: { days } });
    return resp.data;
}
export async function fetchCostMonthlyData() {
    const resp = await api.get('/statistics/cost/monthly');
    return resp.data;
}
