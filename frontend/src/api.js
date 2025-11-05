export async function apiFetch(path, opts = {}) {
    const res = await fetch(path, {
        credentials: 'include',
        headers: {'Accept': 'application/json', ...(opts.headers || {})},
        ...opts
    });
    if (res.status === 401) {
        return {ok: false, status: 401, data: null};
    }
    const data = await res.json().catch(() => null);
    return {ok: res.ok, status: res.status, data};
}

export async function getUser() {
    return await apiFetch('/api/players/me', {
        method: "GET"
    });
}

export async function login(credentials) {
    return await apiFetch("/api/auth/authenticate", {
        method: "POST",
        body: JSON.stringify(credentials),
    });
}

export async function isLoggedIn() {
    const r = await apiFetch('/api/players/me', {method: 'GET'});
    return r.ok ? r.data : null;
}
