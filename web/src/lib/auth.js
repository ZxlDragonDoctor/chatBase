const AUTH_KEY = 'chatbase_auth';
const GUEST_KEY = 'chatbase_guest_id';
/**
 * 未显式登录时使用稳定游客 ID（与 Kimi「默认游客」一致，本地持久化）
 */
export function getAuth() {
    const raw = localStorage.getItem(AUTH_KEY);
    if (raw) {
        try {
            const p = JSON.parse(raw);
            if (p.userId && p.displayName && (p.mode === 'guest' || p.mode === 'user')) {
                return p;
            }
        }
        catch {
            /* ignore */
        }
    }
    let gid = localStorage.getItem(GUEST_KEY);
    if (!gid) {
        gid = `guest-${crypto.randomUUID()}`;
        localStorage.setItem(GUEST_KEY, gid);
    }
    return { mode: 'guest', userId: gid, displayName: '游客' };
}
/** 使用显示名登录（演示用：无密码，仅区分知识库/历史维度下的 userId） */
export function loginAsUser(displayName) {
    const name = displayName.trim() || '用户';
    const slug = name.replace(/[^\w\u4e00-\u9fa5-]/g, '_').slice(0, 48) || 'user';
    const userId = `user-${slug}`;
    const state = { mode: 'user', userId, displayName: name };
    localStorage.setItem(AUTH_KEY, JSON.stringify(state));
}
/** 清除登录态，回到游客（保留同一游客 UUID） */
export function logoutToGuest() {
    localStorage.removeItem(AUTH_KEY);
}
