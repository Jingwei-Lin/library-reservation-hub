import axios from 'axios';
import { logoutUser } from './authApi';

const api = axios.create({
    baseURL: '/api',
    withCredentials: true,
});

export const reviewService = {
    /**
     * Get all reviews for a specific book
     * @param {number} bookId - The ID of the book
     * @returns {Promise<Array>} List of reviews
     */
    getReviewsByBookId: async (bookId) => {
        const response = await api.get(`/review/book/${bookId}`);
        console.log('getReviewsByBookId response:', response);
        return response.data;
    },

    /**
     * Add a new review for a specific book
     * @param {number} bookId - The ID of the book
     * @param {Object} reviewData - Review data { rating, comment }
     * @returns {Promise<number>} The ID of the created review
     */
    addReview: async (bookId, reviewData) => {
        const response = await api.post(`/review/add/${bookId}`, reviewData);
        console.log('addReview response:', response);
        if (response.data.code !== 0) {
            logoutUser();
            throw new Error(response.data.message || 'Failed to add review');
        }
        return response.data.data;
    },

    /**
     * Delete a specific review
     * @param {number} reviewId - The ID of the review
     * @returns {Promise<boolean>} True if deletion was successful
     */

    deleteReview: async (reviewId) => {
        const response = await api.delete(`/review/delete/${reviewId}`);
        console.log('deleteReview response:', response);
        if (response.data.code !== 0) {
            logoutUser();
            throw new Error(response.data.message || 'Failed to delete review');
        }
        return response.data.data;
    },

    /**
     * Update a specific review
     * @param {Object} reviewData - Review data { reviewId, rating, comment }
     * @returns {Promise<boolean>} True if update was successful
     */
    updateReview: async (reviewData) => {
        const response = await api.put('/review/update', reviewData);
        console.log('updateReview response:', response);
        if (response.data.code !== 0) {
            logoutUser();
            throw new Error(response.data.message || 'Failed to update review');
        }
        return response.data.data;
    }
};