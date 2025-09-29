import axios from 'axios';

// const API_BASE_URL = 'http://localhost:8432/api'; // Adjust to your backend URL

const api = axios.create({
    baseURL: '/api',
    headers: {
        'Content-Type': 'application/json',
    },
    withCredentials: true, // important for session cookies
});

export const bookService = {
    // Get all books with pagination
    getAllBooks: async (page = 1, size = 100) => { // Increased size to get all books
        try {
            const response = await api.get(`/book/search?current=${page}&pageSize=${size}`);
            console.log('Get all books response:', response); // Debug log
            return response.data;
        } catch (error) {
            console.error('Error fetching books:', error);
            throw new Error('Failed to fetch books');
        }
    },

    // Search books by keyword
    searchBooks: async (keyword, page = 1, size = 100) => {
        try {
            const url = keyword
                ? `/book/search?keyword=${encodeURIComponent(keyword)}&current=${page}&pageSize=${size}`
                : `/book/search?current=${page}&pageSize=${size}`;

            const response = await api.get(url);
            console.log('Search books response:', response); // Debug log
            return response.data;
        } catch (error) {
            console.error('Error searching books:', error);
            throw new Error('Failed to search books');
        }
    },

    // Get books by genre
    getBooksByGenre: async (genre) => {
        try {
            const response = await api.get(`/book/genre/${encodeURIComponent(genre)}`);
            console.log('Genre books response:', response); // Debug log
            return response.data;
        } catch (error) {
            console.error('Error fetching books by genre:', error);
            throw new Error('Failed to fetch books by genre');
        }
    },

    // Get book by ID
    getBookById: async (id) => {
        try {
            const response = await api.get(`/book/${id}`);
            console.log('Get book by ID response:', response); // Debug log
            return response.data;
        } catch (error) {
            console.error('Error fetching book by ID:', error);
            throw new Error('Failed to fetch book details');
        }
    },
};