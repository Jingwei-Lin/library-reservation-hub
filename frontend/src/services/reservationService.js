// reservationService.js
import axios from 'axios';
import { logoutUser } from './authApi'; // Import logoutUser for auth error handling

const api = axios.create({
    baseURL: '/api',
    withCredentials: true, // important for cookies/session auth
});

// Helper to add Authorization if token exists
const getAuthHeader = () => {
    const user = JSON.parse(sessionStorage.getItem('user') || '{}'); // Use sessionStorage
    return user && user.token ? { Authorization: `Bearer ${user.token}` } : {};
};

export const reservationService = {
    createReservation: async (bookId) => {
        try {
            const response = await api.post(`/reservation/create/${bookId}`, {}, {
                headers: getAuthHeader(),
            });
            console.log('createReservation response:', response.data);
            return response.data;
        } catch (error) {
            if (error.response && [401, 403].includes(error.response.status)) {
                logoutUser(); // Clear sessionStorage on auth error
                throw new Error('Session expired or user banned. Please log in again.');
            }
            console.error('createReservation error:', error.response?.data, error.message);
            throw new Error(error.response?.data?.msg || error.message || 'Failed to create reservation');
        }
    },

    getUserReservations: async () => {
        try {
            const response = await api.get(`/reservation/user`, {
                headers: getAuthHeader(),
            });
            console.log('getUserReservations response:', response.data);
            return response.data;
        } catch (error) {
            if (error.response && [401, 403].includes(error.response.status)) {
                logoutUser();
                throw new Error('Session expired or user banned. Please log in again.');
            }
            console.error('getUserReservations error:', error.response?.data, error.message);
            throw new Error(error.response?.data?.msg || error.message || 'Failed to load reservations');
        }
    },

    cancelReservation: async (reservationId) => {
        try {
            const response = await api.delete(`/reservation/cancel/${reservationId}`, {
                headers: getAuthHeader(),
            });
            console.log('cancelReservation response:', response.data);
            return response.data;
        } catch (error) {
            if (error.response && [401, 403].includes(error.response.status)) {
                logoutUser();
                throw new Error('Session expired or user banned. Please log in again.');
            }
            console.error('cancelReservation error:', error.response?.data, error.message);
            throw new Error(error.response?.data?.msg || error.message || 'Failed to cancel reservation');
        }
    },
};