import axios from 'axios';

axios.defaults.withCredentials = true;
axios.defaults.xsrfCookieName = 'XSRF-TOKEN';
axios.defaults.xsrfHeaderName = 'X-XSRF-TOKEN';
axios.interceptors.response.use((response) => response, async (error) => {
    const originalRequest = error.config;
    if (error.response.status === 401 && !originalRequest._retry) {
        originalRequest._retry = true;

        try {
            await axios.post("/auth/refreshToken");
            return axios(originalRequest);

        } catch (refreshError) {
            console.error("Sesja wygasła");
            window.location.href = '/login';
            return Promise.reject(refreshError);
        }

    }
})


const getCookie = (name) => {
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) return parts.pop().split(';').shift();
};

export function setupSilentRefresh() {
    const expiresAt = getCookie('JWT-EXPIRES-AT');
    if (!expiresAt) return;

    const remainingTime = parseInt(expiresAt) - Date.now();

    // for refreshing x seconds before jwt expiring
    const refreshThreshold = 30 * 1000;
    const delay = remainingTime - refreshThreshold;

    if(delay > 0){
        setTimeout(async ()=>{
            try{
                await axios.post("/api/auth/refreshToken");
                // after success, function has to be called again to set up refresh for new jwt
                setupSilentRefresh();
            } catch(err){
                console.error("Session refresh failed (", err, ")");
            }
        }, delay);
    } else {
        axios.post("/api/auth/refreshToken")
            .then(() => setupSilentRefresh())
            .catch(() => console.error("Immediate refresh failed"));
    }
}
export default axios;
