export function getCurrentUser() {
    return localStorage.getItem('chatbase_user');
}
export function getOrCreateUserId() {
    const loggedUser = getCurrentUser();
    if (loggedUser && loggedUser.trim())
        return loggedUser;
    const key = 'chatbase_user_id';
    const existing = localStorage.getItem(key);
    if (existing && existing.trim())
        return existing;
    const uuid = typeof crypto !== 'undefined' && crypto.randomUUID
        ? crypto.randomUUID()
        : `${Date.now()}-${Math.random().toString(36).slice(2, 11)}`;
    const id = `web-${uuid}`;
    localStorage.setItem(key, id);
    return id;
}
