import axios from './axios.js';

export async function apiFetch(path, opts = {}) {
    try {
        const method = opts.method || 'GET';
        const data = opts.body ? JSON.parse(opts.body) : undefined;
        const res = await axios({ url: path, method, data, headers: opts.headers });
        return { ok: true, status: res.status, data: res.data };
    } catch (error) {
        if (error.response) {
            // The request was made and the server responded with a status code
            // that falls out of the range of 2xx
            return {
                ok: false,
                status: error.response.status,
                data: error.response.data
            };
        } else if (error.request) {
            // The request was made but no response was received
            return {
                ok: false,
                status: -1, // Custom status for network errors
                data: { message: 'Brak odpowiedzi od serwera. Sprawdź połączenie z internetem.' }
            };
        } else {
            // Something happened in setting up the request that triggered an Error
            return {
                ok: false,
                status: -2, // Custom status for client-side errors
                data: { message: error.message }
            };
        }
    }
}
