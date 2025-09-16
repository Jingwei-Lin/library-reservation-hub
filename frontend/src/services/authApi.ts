import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080'; // Adjust to your backend URL

const api = axios.create({
    baseURL: API_BASE_URL,
    headers: {
        'Content-Type': 'application/json',
    },
});

// Register user
export const registerUser = async (userData: {
    userAccount: string;
    userPassword: string;
    checkPassword: string;
    firstName: string;
    lastName: string;
    phone: string;
}) => {
    const response = await api.post('/user/register', userData);
    return response.data;
};

// Login user
export const loginUser = async (loginData: {
    userAccount: string;
    userPassword: string;
}) => {
    const response = await api.post('/user/login', loginData);
    return response.data;
};

// Logout (clear localStorage)
export const logoutUser = () => {
    localStorage.removeItem('user');
};