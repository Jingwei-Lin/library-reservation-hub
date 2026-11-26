import api, { toError } from './api';

export const waitingListService = {
    getMyWaitingList: async () => {
        try {
            const response = await api.get('/waiting-list/my');
            return response.data;
        } catch (error) {
            throw toError(error, 'Failed to load waiting list');
        }
    },

    addToWaitingList: async (bookId) => {
        try {
            const response = await api.post('/waiting-list/my/add', { bookId });
            return response.data;
        } catch (error) {
            throw toError(error, 'Failed to add to waiting list');
        }
    },

    cancelWaiting: async (bookId) => {
        try {
            const response = await api.delete(`/waiting-list/my/${bookId}`);
            return response.data;
        } catch (error) {
            throw toError(error, 'Failed to cancel waiting list entry');
        }
    },

    getEligibility: async () => {
        try {
            const response = await api.get('/waiting-list/my/eligibility');
            return response.data;
        } catch (error) {
            throw toError(error, 'Failed to check eligibility');
        }
    },

    // admin: every waiting-list entry of one user, cancelled ones included
    getByUserAdmin: async (userId) => {
        try {
            const response = await api.get(`/waiting-list/user/${userId}`);
            return response.data;
        } catch (error) {
            throw toError(error, 'Failed to load user waiting list');
        }
    },
};
