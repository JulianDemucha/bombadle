import axios from 'axios';

axios.defaults.withCredentials = true;
axios.defaults.xsrfCookieName = 'XSRF-TOKEN';
axios.defaults.xsrfHeaderName = 'X-XSRF-TOKEN';

let refreshPromise = null;
let silentRefreshTimer = null;
const REFRESH_ENDPOINT = '/api/auth/refreshToken';

const getCookie = (name) => {
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) return parts.pop().split(';').shift();
};

const shouldIgnoreError = (config) => {
    const url = config?.url || '';
    return !config || 
           url.includes(REFRESH_ENDPOINT) || 
           url.includes('/api/auth/authenticate') || 
           url.includes('/api/auth/register');
};

axios.interceptors.response.use(
    (response) => response,
    async (error) => {
        const originalRequest = error.config;

        if (!error.response || shouldIgnoreError(originalRequest)) {
            return Promise.reject(error);
        }

        if (error.response.status === 401 && !originalRequest._retry) {
            originalRequest._retry = true;

            try {
                if (!refreshPromise) {
                    refreshPromise = axios.post(REFRESH_ENDPOINT)
                        .finally(() => { refreshPromise = null; });
                }
                await refreshPromise;
                return axios(originalRequest);
            } catch (err) {
                return Promise.reject(err);
            }
        }
        return Promise.reject(error);
    }
);

export function setupSilentRefresh() {
    clearTimeout(silentRefreshTimer);

    const expiresAt = getCookie('JWT-EXPIRES-AT');
    if (!expiresAt) return;

    const delay = Math.max(Number(expiresAt) - Date.now() - 30000, 5000);

    silentRefreshTimer = setTimeout(() => {
        axios.post(REFRESH_ENDPOINT)
            .then(setupSilentRefresh)
            .catch(() => {});
    }, delay);
}

export default axios;
