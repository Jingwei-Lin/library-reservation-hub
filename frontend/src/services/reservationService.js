import api, { toError } from './api';

export const reservationService = {
    createReservation: async (bookId, formData) => {
        try {
            const response = await api.post(`/reservation/create/${bookId}`, formData);
            return response.data;
        } catch (error) {
            throw toError(error, 'Failed to create reservation');
        }
    },

    getUserReservations: async () => {
        try {
            const response = await api.get(`/reservation/user`);
            return response.data;
        } catch (error) {
            throw toError(error, 'Failed to load reservations');
        }
    },

    cancelReservation: async (reservationId) => {
        try {
            const response = await api.delete(`/reservation/cancel/${reservationId}`);
            return response.data;
        } catch (error) {
            throw toError(error, 'Failed to cancel reservation');
        }
    },

    hasActiveReservation: async (bookId) => {
        try {
            const response = await api.get(`/reservation/check/${bookId}`);
            return response.data;
        } catch (error) {
            throw toError(error, 'Failed to check reservation status');
        }
    },

    getAllReservations: async (current = 1, pageSize = 5) => {
        try {
            const response = await api.get(`/reservation/all`, { params: { current, pageSize } });
            return response.data;
        } catch (error) {
            throw toError(error, 'Failed to load all reservations');
        }
    },

    updateReservationStatus: async (reservationId, status) => {
        try {
            const response = await api.put(`/reservation/status/${reservationId}`, null, { params: { status } });
            return response.data;
        } catch (error) {
            throw toError(error, 'Failed to update reservation status');
        }
    },

    updatePickupTime: async (reservationId, formData) => {
        try {
            const response = await api.put(`/reservation/pickup/${reservationId}`, formData);
            return response.data;
        } catch (error) {
            throw toError(error, 'Failed to update pickup time');
        }
    },
};
