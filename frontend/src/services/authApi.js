// authApi.js
import axios from 'axios';

const api = axios.create({
    baseURL: '/api',
    headers: {
        'Content-Type': 'application/json',
    },
    withCredentials: true, // important for session cookies
});

// Register user
export const registerUser = async (userData) => {
    const response = await api.post('/user/register', userData);
    const result = response.data;

    if (result.code !== 0) {
        throw new Error(result.message || 'Registration failed');
    }

    return result.data;
};

// Login user
export const loginUser = async (loginData) => {
    const response = await api.post('/user/login', loginData);
    const result = response.data;

    console.log("authApi");
    console.log(result);

    if (result.code !== 0) {
        throw new Error(result.message || 'Login failed');
    }

    return result.data;
};

// Validate session
export const validateSession = async () => {
    try {
        const response = await api.get('/user/get/login');
        const result = response.data;
        console.log(response)
        if (result.code !== 0) {
            throw new Error(result.message || 'Session invalid');
        }
        return result.data; // Return user data if needed
    } catch (error) {
        throw new Error(error.message || 'Session validation failed');
    }
};

// Logout (clear sessionStorage)
export const logoutUser = () => {
    sessionStorage.removeItem('user');
    sessionStorage.removeItem('token');
};