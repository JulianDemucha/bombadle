import { useCallback, useState } from 'react';
import { isInAppBrowser } from './detectInAppBrowser.js';

/**
 * Smart hook shared between LoginPage and RegisterPage (same pattern as
 * useAnonymousMergePrompt / useAccountRecovery). Intercepts the Google-login click: if the UA
 * looks like an in-app/embedded browser (Facebook, Instagram, Messenger, etc.), it opens a
 * warning dialog instead of letting the OAuth redirect proceed.
 *
 * Usage in a page:
 *   const inAppWarning = useInAppBrowserWarning();
 *   const handleGoogleLogin = () => inAppWarning.guardGoogleLogin(() => { window.location.href = ... });
 *   <ConfirmDialog isOpen={inAppWarning.isOpen} onConfirm={inAppWarning.dismiss} ... />
 */
export default function useInAppBrowserWarning() {
    const [isOpen, setIsOpen] = useState(false);

    const guardGoogleLogin = useCallback((proceed) => {
        if (isInAppBrowser()) {
            setIsOpen(true);
            return;
        }
        proceed();
    }, []);

    const dismiss = useCallback(() => setIsOpen(false), []);

    return { isOpen, guardGoogleLogin, dismiss };
}
