import { api } from './client';
export async function fetchOverview() {
    const resp = await api.get('/console/overview');
    return resp.data;
}
export async function fetchGroups(platform = 'all') {
    const resp = await api.get('/console/groups', { params: { platform } });
    return resp.data;
}
export async function fetchGroupMessages(params) {
    const gid = params.groupId != null && String(params.groupId).trim() !== '' ? String(params.groupId) : '';
    if (!gid) {
        return { records: [], total: 0, page: params.page ?? 0, size: params.size ?? 30 };
    }
    const resp = await api.get('/console/messages', {
        params: {
            groupId: gid,
            platform: params.platform ?? 'all',
            page: params.page ?? 0,
            size: params.size ?? 30,
        },
    });
    return resp.data;
}
