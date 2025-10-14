// bookService.js
import axios from 'axios';
import { logoutUser } from './authApi'; // Import logoutUser for handling auth errors

const api = axios.create({
    baseURL: '/api',
    headers: {
        'Content-Type': 'application/json',
    },
    withCredentials: true,
});

export const bookService = {
    getAllBooks: async (page = 1, size = 101) => {
        try {
            const response = await api.get(`/book/search?current=${page}&pageSize=${size}`);
            console.log('Get all books response:', response);
            return response.data;
        } catch (error) {
            if (error.response && [401, 403].includes(error.response.status)) {
                logoutUser(); // Clear sessionStorage on auth error
                throw new Error('Session expired or user banned. Please log in again.');
            }
            console.error('Error fetching books:', error);
            throw new Error('Failed to fetch books');
        }
    },

    searchBooks: async (keyword, page = 1, size = 101) => {
        try {
            const url = keyword
                ? `/book/search?keyword=${encodeURIComponent(keyword)}&current=${page}&pageSize=${size}`
                : `/book/search?current=${page}&pageSize=${size}`;
            const response = await api.get(url);
            console.log('Search books response:', response);
            return response.data;
        } catch (error) {
            if (error.response && [401, 403].includes(error.response.status)) {
                logoutUser();
                throw new Error('Session expired or user banned. Please log in again.');
            }
            console.error('Error searching books:', error);
            throw new Error('Failed to search books');
        }
    },

    getBooksByGenre: async (genre) => {
        try {
            const response = await api.get(`/book/genre/${encodeURIComponent(genre)}`);
            console.log('Genre books response:', response);
            return response.data;
        } catch (error) {
            if (error.response && [401, 403].includes(error.response.status)) {
                logoutUser();
                throw new Error('Session expired or user banned. Please log in again.');
            }
            console.error('Error fetching books by genre:', error);
            throw new Error('Failed to fetch books by genre');
        }
    },

    getBookById: async (id) => {
        try {
            const response = await api.get(`/book/${id}`);
            console.log('Get book by ID response:', response);
            return response.data;
        } catch (error) {
            if (error.response && [401, 403].includes(error.response.status)) {
                logoutUser();
                throw new Error('Session expired or user banned. Please log in again.');
            }
            console.error('Error fetching book by ID:', error);
            throw new Error('Failed to fetch book details');
        }
    },
};