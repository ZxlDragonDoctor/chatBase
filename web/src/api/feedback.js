import { api } from './client';
export async function submitFeedback(sessionId, messageIndex, rating, feedbackType, content) {
    try {
        const resp = await api.post('/kb/conversation/feedback', {
            sessionId,
            messageIndex,
            rating,
            feedbackType,
            feedbackContent: content,
        });
        return resp.data;
    }
    catch {
        return { success: false, message: '请求失败' };
    }
}
export async function getFeedbackStatus(sessionId) {
    try {
        const resp = await api.get('/kb/conversation/feedback/status', {
            params: { sessionId },
        });
        return resp.data || {};
    }
    catch {
        return {};
    }
}
