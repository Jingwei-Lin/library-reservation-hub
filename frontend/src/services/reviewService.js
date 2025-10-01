import axios from 'axios';
import { logoutUser } from './authApi';

const api = axios.create({
    baseURL: '/api',
    withCredentials: true,
});

export const reviewService = {
    getReviewsByBookId: async (bookId) => {
        try {
            const response = await api.get(`/review/book/${bookId}`);
            console.log('getReviewsByBookId response:', response);
            // if (response.code !== 0) {
            //     logoutUser();
            //     throw new Error(response.message || 'Session invalid');
            // }
            return response.data;

        } catch (error) {
            if (error.response && [401, 403].includes(error.response.status)) {
                logoutUser();
                throw new Error('Session expired or user banned. Please log in again.');
            }
            console.error('Error fetching reviews by book:', error);
            throw new Error('Failed to fetch reviews');
        }
    },
};