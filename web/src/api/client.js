import axios from 'axios';
export const api = axios.create({
    baseURL: '/api',
    timeout: 120_000,
});
api.interceptors.request.use(config => {
    const token = localStorage.getItem('chatbase_token');
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});
api.interceptors.response.use(response => response, error => {
    if (error.response?.status === 401) {
        localStorage.removeItem('chatbase_token');
        localStorage.removeItem('chatbase_user');
        window.location.href = '/login';
    }
    return Promise.reject(error);
});
