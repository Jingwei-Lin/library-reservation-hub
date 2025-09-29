import axios from 'axios';

// const API_BASE_URL = 'http://localhost:8432/api'; // Adjust to your backend URL

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
        // throw error so frontend can catch
        throw new Error(result.message || 'Registration failed');
    }

    return result.data;
};

// Login user
export const loginUser = async (loginData) => {
    const response = await api.post('/user/login', loginData);
    const result = response.data;

    console.log("authApi")
    console.log(result)


    if (result.code !== 0) {
        // throw error so frontend can catch
        throw new Error(result.message || 'Login failed');
    }

    return result.data;
};

// Logout (clear localStorage)
export const logoutUser = () => {
    localStorage.removeItem('user');
};
