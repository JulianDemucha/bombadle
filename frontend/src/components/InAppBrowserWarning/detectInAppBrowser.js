// UA sniffing is heuristic/best-effort (strings are not a reliable signal) — treat this purely
// as a UX improvement, never as a security control.
const IN_APP_BROWSER_SIGNATURES = [
    /FBAN|FBAV/i,       // Facebook
    /Instagram/i,
    /Messenger/i,
    /Line\//i,
    /MicroMessenger/i,  // WeChat
    /musical_ly|BytedanceWebview|TikTok/i,
    /Snapchat/i,
    /LinkedInApp/i,
];

export function isInAppBrowser(userAgent = navigator.userAgent) {
    return IN_APP_BROWSER_SIGNATURES.some((pattern) => pattern.test(userAgent));
}
