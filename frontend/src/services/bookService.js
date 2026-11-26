// bookService.js
import api, { toError } from './api';

export const bookService = {
    // includeDeleted only has an effect for admins (the admin page)
    getAllBooks: async (page = 1, size = 101, includeDeleted = false) => {
        try {
            const response = await api.get(`/book/search?sortField=title&sortOrder=ascend&current=${page}&pageSize=${size}`
                + (includeDeleted ? '&includeDeleted=true' : ''));
            return response.data;
        } catch (error) {
            throw toError(error, 'Failed to fetch books');
        }
    },

    searchBooks: async (keyword, page = 1, size = 101) => {
        try {
            const url = keyword
                ? `/book/search?sortField=title&sortOrder=ascend&keyword=${encodeURIComponent(keyword)}&current=${page}&pageSize=${size}`
                : `/book/search?sortField=title&sortOrder=ascend&current=${page}&pageSize=${size}`;
            const response = await api.get(url);
            return response.data;
        } catch (error) {
            throw toError(error, 'Failed to search books');
        }
    },

    getBooksByGenre: async (genre) => {
        try {
            const response = await api.get(`/book/genre/${encodeURIComponent(genre)}`);
            return response.data;
        } catch (error) {
            throw toError(error, 'Failed to fetch books by genre');
        }
    },

    getBookById: async (id) => {
        try {
            const response = await api.get(`/book/${id}`);
            return response.data;
        } catch (error) {
            throw toError(error, 'Failed to fetch book details');
        }
    },

    updateBook: async (id, bookPayload) => {
        try {
            const response = await api.put(`/book/${id}`, bookPayload);
            return response.data;
        } catch (error) {
            throw toError(error, 'Failed to update book');
        }
    },

    deleteBook: async (id) => {
        try {
            const response = await api.delete(`/book/${id}`);
            return response.data;
        } catch (error) {
            throw toError(error, 'Failed to delete book');
        }
    },

    restoreBook: async (id) => {
        try {
            const response = await api.put(`/book/${id}/restore`);
            return response.data;
        } catch (error) {
            throw toError(error, 'Failed to restore book');
        }
    },

    getTop10Books: async () => {
        try {
            const response = await api.get('/book-rating/top10-books');
            return response.data;
        } catch (error) {
            throw toError(error, 'Failed to fetch top 10 books');
        }
    },

    getRatingByBookId: async (bookId) => {
        try {
            const response = await api.get(`/book-rating/${bookId}`);
            return response.data;
        } catch (error) {
            throw toError(error, 'Failed to fetch book rating');
        }
    }
};
