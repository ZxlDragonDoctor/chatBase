const PREFIX = 'chatbase_msgs:';
export function loadLocalMessages(userId, sessionId) {
    const raw = localStorage.getItem(PREFIX + userId + ':' + sessionId);
    if (!raw)
        return [];
    try {
        const v = JSON.parse(raw);
        return Array.isArray(v) ? v : [];
    }
    catch {
        return [];
    }
}
export function saveLocalMessages(userId, sessionId, messages) {
    localStorage.setItem(PREFIX + userId + ':' + sessionId, JSON.stringify(messages));
}
export function activeSessionKey(userId) {
    return `chatbase_active_session:${userId}`;
}
export function loadActiveSessionId(userId) {
    return localStorage.getItem(activeSessionKey(userId));
}
export function saveActiveSessionId(userId, sessionId) {
    localStorage.setItem(activeSessionKey(userId), sessionId);
}
