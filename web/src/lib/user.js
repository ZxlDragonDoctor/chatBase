export function getOrCreateUserId() {
    const key = 'chatbase_user_id';
    const existing = localStorage.getItem(key);
    if (existing && existing.trim())
        return existing;
    const id = `web-${crypto.randomUUID()}`;
    localStorage.setItem(key, id);
    return id;
}
