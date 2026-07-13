import {useEffect} from 'react';
import {useLocation, useNavigate, useNavigationType} from 'react-router-dom';

const IN_APP_NAV_KEY = 'bombadle_has_in_app_nav';

// Mounted once near the router root. Flags the session once a real PUSH
// navigation happens, so BackArrowButton knows navigate(-1) has somewhere
// real to land. Deliberately ignores REPLACE: the leaderboard section (and
// returning from it) navigates via replace so it collapses back to a single
// history slot, and those location changes must not be mistaken for "real"
// history depth or navigate(-1) would fall through past the app entirely.
export function NavigationTracker() {
    const navigationType = useNavigationType();

    useEffect(() => {
        if (navigationType === 'PUSH') {
            sessionStorage.setItem(IN_APP_NAV_KEY, '1');
        }
    }, [navigationType]);

    return null;
}

export function useBackNavigation() {
    const navigate = useNavigate();
    const location = useLocation();

    return () => {
        if (location.state?.from) {
            // replace, not push: entering the leaderboard also replaces (see
            // Top3Leaderboard/Top3SuperstreakBoard), so the whole detour
            // collapses back to the one slot the origin page already had —
            // otherwise a second back-click would land back on the detour.
            navigate(location.state.from, {replace: true});
            return;
        }
        if (sessionStorage.getItem(IN_APP_NAV_KEY) === '1') {
            navigate(-1);
            return;
        }
        navigate('/');
    };
}
