import axios from 'axios';
import { logoutUser } from './authApi';

const api = axios.create({
    baseURL: '/api',
    withCredentials: true,
});

const getAuthHeader = () => {
    const user = JSON.parse(sessionStorage.getItem('user') || '{}');
    if (!user || !user.token) {
        console.warn('No valid user or token found in sessionStorage:', user);
        return {};
    }
    return { Authorization: `Bearer ${user.token}` };
};

export const reservationService = {
    createReservation: async (bookId, formData) => {
        try {
            const response = await api.post(`/reservation/create/${bookId}`, formData, {
                headers: getAuthHeader(),
            });
            console.log('createReservation response:', response.data);
            return response.data;
        } catch (error) {
            console.error('createReservation error:', error.response?.data, error.message);
            if (error.response && [401, 403].includes(error.response.status)) {
                logoutUser();
                throw new Error('Session expired or user banned. Please log in again.');
            }
            throw new Error(error.response?.data?.message || error.message || 'Failed to create reservation');
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
            console.error('getUserReservations error:', error.response?.data, error.message);
            if (error.response && [401, 403].includes(error.response.status)) {
                logoutUser();
                throw new Error('Session expired or user banned. Please log in again.');
            }
            throw new Error(error.response?.data?.message || error.message || 'Failed to load reservations');
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
            console.error('cancelReservation error:', error.response?.data, error.message);
            if (error.response && [401, 403].includes(error.response.status)) {
                logoutUser();
                throw new Error('Session expired or user banned. Please log in again.');
            }
            throw new Error(error.response?.data?.message || error.message || 'Failed to cancel reservation');
        }
    },

    hasActiveReservation: async (bookId) => {
        try {
            const response = await api.get(`/reservation/check/${bookId}`, {
                headers: getAuthHeader(),
            });
            console.log('hasActiveReservation response:', response.data);
            return response.data;
        } catch (error) {
            console.error('hasActiveReservation error:', error.response?.data, error.message);
            if (error.response && [401, 403].includes(error.response.status)) {
                logoutUser();
                throw new Error('Session expired or user banned. Please log in again.');
            }
            throw new Error(error.response?.data?.message || error.message || 'Failed to check reservation status');
        }
    },

    getAllReservations: async (current = 1, pageSize = 5) => {
        try {
            const response = await api.get(`/reservation/all`, {
                headers: getAuthHeader(),
                params: { current, pageSize },
            });
            return response.data;
        } catch (error) {
            if (error.response && [401, 403].includes(error.response.status)) {
                logoutUser();
                throw new Error('Session expired or user banned. Please log in again.');
            }
            throw new Error(error.response?.data?.message || error.message || 'Failed to load all reservations');
        }
    },

    updateReservationStatus: async (reservationId, status) => {
        try {
            const response = await api.put(`/reservation/status/${reservationId}`, null, {
                headers: getAuthHeader(),
                params: { status },
            });
            return response.data;
        } catch (error) {
            if (error.response && [401, 403].includes(error.response.status)) {
                logoutUser();
                throw new Error('Session expired or user banned. Please log in again.');
            }
            throw new Error(error.response?.data?.message || error.message || 'Failed to update reservation status');
        }
    },
};