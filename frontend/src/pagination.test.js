import { describe, expect, it } from 'vitest';
import { pageButtons, pageOf } from './pagination';

const books = (n) => Array.from({ length: n }, (_, i) => ({ bookId: i + 1 }));

describe('pageOf', () => {
    it('shows a server-paged list as it is, with the server total', () => {
        const page = books(12);
        const result = pageOf({ books: page, serverPaged: true, totalBooks: 102, currentPage: 9 });

        expect(result.totalCount).toBe(102);
        expect(result.totalPages).toBe(9);
        expect(result.currentBooks).toBe(page);
    });

    it('slices a small list here', () => {
        const result = pageOf({ books: books(30), serverPaged: false, totalBooks: 0, currentPage: 3 });

        expect(result.totalCount).toBe(30);
        expect(result.totalPages).toBe(3);
        expect(result.currentBooks.map((b) => b.bookId)).toEqual([25, 26, 27, 28, 29, 30]);
    });

    it('always has at least one page', () => {
        expect(pageOf({ books: [], serverPaged: true, totalBooks: 0, currentPage: 1 }).totalPages).toBe(1);
    });
});

describe('pageButtons', () => {
    it('lists every page up to five', () => {
        expect(pageButtons(1, 1)).toEqual([1]);
        expect(pageButtons(2, 5)).toEqual([1, 2, 3, 4, 5]);
    });

    it('near the start: the first four, then the last', () => {
        expect(pageButtons(3, 9)).toEqual([1, 2, 3, 4, '...', 9]);
    });

    it('near the end: the first, then the last four', () => {
        expect(pageButtons(7, 9)).toEqual([1, '...', 6, 7, 8, 9]);
    });

    it('in the middle: the pages around the current one', () => {
        expect(pageButtons(5, 9)).toEqual([1, '...', 4, 5, 6, '...', 9]);
    });
});
