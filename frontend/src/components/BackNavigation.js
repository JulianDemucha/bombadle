import {useEffect, useRef} from 'react';
import {useLocation, useNavigate} from 'react-router-dom';

const IN_APP_NAV_KEY = 'bombadle_has_in_app_nav';

// Mounted once near the router root. Flags every route change after the
// initial page load, so BackArrowButton can tell "landed here fresh" apart
// from "navigated here within the app" even after a reload.
export function NavigationTracker() {
    const location = useLocation();
    // Compared against location.key (not a mount-order flag) because
    // StrictMode double-invokes this effect once on initial mount, which
    // would otherwise flag the very first page load as an in-app navigation.
    const lastKeyRef = useRef(location.key);

    useEffect(() => {
        if (location.key !== lastKeyRef.current) {
            sessionStorage.setItem(IN_APP_NAV_KEY, '1');
            lastKeyRef.current = location.key;
        }
    }, [location]);

    return null;
}

export function useBackNavigation() {
    const navigate = useNavigate();
    const location = useLocation();

    return () => {
        if (location.state?.from) {
            navigate(location.state.from);
            return;
        }
        if (sessionStorage.getItem(IN_APP_NAV_KEY) === '1') {
            navigate(-1);
            return;
        }
        navigate('/');
    };
}
