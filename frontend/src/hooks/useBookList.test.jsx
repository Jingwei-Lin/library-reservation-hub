// @vitest-environment jsdom
import { afterEach, describe, expect, it, vi } from 'vitest';
import { act, cleanup, renderHook } from '@testing-library/react';
import { useBookList } from './useBookList';
import { bookService } from '../services/bookService';

vi.mock('../services/bookService', () => ({
    bookService: { getAllBooks: vi.fn(), searchBooks: vi.fn(), getBooksByGenre: vi.fn(), getTop10Books: vi.fn() },
}));

/** A promise the test resolves when it chooses */
const later = () => {
    let resolve;
    const promise = new Promise(r => { resolve = r; });
    return { promise, resolve };
};

describe('useBookList', () => {
    afterEach(() => {
        cleanup();
        vi.clearAllMocks();
    });

    it('a page from the server keeps the server total', async () => {
        bookService.getAllBooks.mockResolvedValue({ code: 0, data: { records: [{ bookId: 1 }, { bookId: 2 }], total: 102 } });
        const { result } = renderHook(() => useBookList(12));

        await act(() => result.current.fetchBooks('', 3));

        expect(bookService.getAllBooks).toHaveBeenCalledWith(3, 12);
        expect(result.current.books).toHaveLength(2);
        expect(result.current.totalBooks).toBe(102);
        expect(result.current.serverPaged).toBe(true);
        expect(result.current.currentTitle).toBe('All Books');
        expect(result.current.loading).toBe(false);
    });

    it('a genre is a whole list, without books that have no id', async () => {
        bookService.getBooksByGenre.mockResolvedValue({ code: 0, data: [{ bookId: 4 }, { bookId: null }, { bookId: 'x' }] });
        const { result } = renderHook(() => useBookList());

        await act(() => result.current.loadGenreBooks('Fantasy'));

        expect(result.current.books).toEqual([{ bookId: 4 }]);
        expect(result.current.serverPaged).toBe(false);
        expect(result.current.currentTitle).toBe('Fantasy Books');
    });

    it('a slow earlier load cannot replace a newer list', async () => {
        const slowGenre = later();
        bookService.getBooksByGenre.mockReturnValue(slowGenre.promise);
        bookService.getAllBooks.mockResolvedValue({ code: 0, data: { records: [{ bookId: 1 }], total: 1 } });
        const { result } = renderHook(() => useBookList());

        let genreLoad;
        act(() => { genreLoad = result.current.loadGenreBooks('Fantasy'); });
        await act(() => result.current.fetchBooks('', 1));
        await act(async () => {
            slowGenre.resolve({ code: 0, data: [{ bookId: 9 }] });
            await genreLoad;
        });

        expect(result.current.currentTitle).toBe('All Books');
        expect(result.current.books).toEqual([{ bookId: 1 }]);
        expect(result.current.loading).toBe(false);
    });

    it('an empty top 10 says so', async () => {
        bookService.getTop10Books.mockResolvedValue({ code: 0, data: [] });
        const { result } = renderHook(() => useBookList());

        await act(() => result.current.loadTop10Books());

        expect(result.current.error).toBe('No top-rated books available yet.');
        expect(result.current.currentTitle).toBe('Top 10 Rated Books');
    });
});
