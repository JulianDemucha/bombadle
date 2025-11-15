import axios from './axios.js';

export async function apiFetch(path, opts = {}) {
    const method = opts.method || 'GET';
    const data = opts.body ? JSON.parse(opts.body) : undefined;
    const res = await axios({ url: path, method, data, headers: opts.headers });
    return { ok: true, status: res.status, data: res.data };
}