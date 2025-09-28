import axios from 'axios';

const API_BASE = 'http://localhost:8432/api';

const getAuthHeader = () => {
    const user = JSON.parse(localStorage.getItem('user'));
    return user && user.token ? { Authorization: `Bearer ${user.token}` } : {};
};

export const reservationService = {
    createReservation: async (bookId) => {
        try {
            const response = await axios.post(`${API_BASE}/reservation/create/${bookId}`, {}, {
                headers: getAuthHeader(),
            });
            console.log('createReservation response:', JSON.stringify(response.data, null, 2));
            return response.data;
        } catch (error) {
            console.error('createReservation error:', error.response?.data, error.message);
            throw new Error(error.response?.data?.msg || error.message || 'Failed to create reservation');
        }
    },

    getUserReservations: async () => {
        try {
            const response = await axios.get(`${API_BASE}/reservation/user`, {
                headers: getAuthHeader(),
            });
            console.log('getUserReservations response:', JSON.stringify(response.data, null, 2));
            return response.data;
        } catch (error) {
            console.error('getUserReservations error:', error.response?.data, error.message);
            throw new Error(error.response?.data?.msg || error.message || 'Failed to load reservations');
        }
    },

    cancelReservation: async (reservationId) => {
        try {
            const response = await axios.delete(`${API_BASE}/reservation/cancel/${reservationId}`, {
                headers: getAuthHeader(),
            });
            console.log('cancelReservation response:', JSON.stringify(response.data, null, 2));
            return response.data;
        } catch (error) {
            console.error('cancelReservation error:', error.response?.data, error.message);
            throw new Error(error.response?.data?.msg || error.message || 'Failed to cancel reservation');
        }
    },
};