import { marked } from 'marked';
export function parseThinkTag(text) {
    const thinkRegex = /<think>([\s\S]*?)<\/think>/g;
    const matches = text.match(thinkRegex);
    let thinking = null;
    let content = text;
    if (matches) {
        const thinkingParts = [];
        for (const match of matches) {
            const inner = match.replace(/<think>|<\/think>/g, '').trim();
            if (inner) {
                thinkingParts.push(inner);
            }
        }
        if (thinkingParts.length > 0) {
            thinking = thinkingParts.join('\n\n');
        }
        content = text.replace(thinkRegex, '').trim();
    }
    return { thinking, content };
}
export function renderMarkdown(text) {
    if (!text)
        return '';
    marked.setOptions({
        breaks: true,
        gfm: true
    });
    return marked.parse(text);
}
export function renderMessage(text) {
    const { thinking, content } = parseThinkTag(text);
    const thinkingHtml = thinking ? renderMarkdown(thinking) : null;
    const contentHtml = renderMarkdown(content);
    return { thinkingHtml, contentHtml };
}
