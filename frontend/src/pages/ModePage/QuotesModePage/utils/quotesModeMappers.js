export const formatQuoteDialogue = (text) => {
    if (!text) return [];
    return String(text).trim().split(/\s+(?=-\s)/);
};