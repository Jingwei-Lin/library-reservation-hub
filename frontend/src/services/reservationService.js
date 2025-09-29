import axios from 'axios';

const api = axios.create({
    baseURL: '/api',            // use relative path so vite proxy works
    withCredentials: true,      // important for cookies/session auth
});

// Helper to add Authorization if token exists
const getAuthHeader = () => {
    const user = JSON.parse(localStorage.getItem('user'));
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
            console.error('cancelReservation error:', error.response?.data, error.message);
            throw new Error(error.response?.data?.msg || error.message || 'Failed to cancel reservation');
        }
    },
};
