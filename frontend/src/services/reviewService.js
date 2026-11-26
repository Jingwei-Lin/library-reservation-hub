import api, { toError } from './api';

// A refused review (for example "Book is not available") is just an error: it no longer signs the user out.
// A session that has really ended is handled once, by the shared client.
export const reviewService = {
    /**
     * Get all reviews for a specific book
     * @param {number} bookId - The ID of the book
     * @returns {Promise<Object>} The response body
     */
    getReviewsByBookId: async (bookId) => {
        const response = await api.get(`/review/book/${bookId}`);
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
        if (response.data.code !== 0) {
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
        if (response.data.code !== 0) {
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
        if (response.data.code !== 0) {
            throw new Error(response.data.message || 'Failed to update review');
        }
        return response.data.data;
    },

    // admin: every review, paged, with optional filters
    getAllReviews: async ({ current = 1, pageSize = 20, bookId, userId, minRating, maxRating } = {}) => {
        try {
            const params = new URLSearchParams();
            params.set('current', current);
            params.set('pageSize', pageSize);
            if (bookId != null) params.set('bookId', bookId);
            if (userId != null) params.set('userId', userId);
            if (minRating != null) params.set('minRating', minRating);
            if (maxRating != null) params.set('maxRating', maxRating);

            const response = await api.get(`/review/all?${params.toString()}`);
            return response.data;
        } catch (error) {
            throw toError(error, 'Failed to fetch reviews');
        }
    }
};
