// @vitest-environment jsdom
// What the admin page sends when a book is saved: a quantity only when the admin changed it, together with the
// count it was changed from, so the server can refuse an edit made on a stale shelf count.
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import BooksSection from './BooksSection';
import { bookService } from '../../services/bookService';

vi.mock('../../services/bookService', () => ({
    bookService: { getAllBooks: vi.fn(), updateBook: vi.fn(), deleteBook: vi.fn(), restoreBook: vi.fn() },
}));

const hobbit = { bookId: 1, title: 'The Hobbit', author: 'J.R.R. Tolkien', genre: 'Fantasy', quantity: 5, heldQuantity: 0, available: true };

describe('BooksSection save', () => {
    beforeEach(() => {
        bookService.getAllBooks.mockResolvedValue({ code: 0, data: { records: [{ ...hobbit }], total: 1 } });
        bookService.updateBook.mockResolvedValue({ code: 0, data: true });
        vi.spyOn(window, 'confirm').mockReturnValue(true);
    });

    afterEach(() => {
        cleanup();
        vi.clearAllMocks();
        vi.restoreAllMocks();
    });

    it('a title edit sends no quantity', async () => {
        render(<BooksSection canLoad />);
        const title = await screen.findByDisplayValue('The Hobbit');

        fireEvent.change(title, { target: { value: 'The Hobbit (fixed)' } });
        fireEvent.click(screen.getByRole('button', { name: 'Save' }));

        await vi.waitFor(() => expect(bookService.updateBook).toHaveBeenCalled());
        const [id, payload] = bookService.updateBook.mock.calls[0];
        expect(id).toBe(1);
        expect(payload.title).toBe('The Hobbit (fixed)');
        expect(payload).not.toHaveProperty('quantity');
        expect(payload).not.toHaveProperty('expectedQuantity');
    });

    it('a quantity change sends the new quantity and the one it was loaded with', async () => {
        render(<BooksSection canLoad />);
        await screen.findByDisplayValue('The Hobbit');

        fireEvent.click(document.querySelector('.step-up'));
        fireEvent.click(screen.getByRole('button', { name: 'Save' }));

        await vi.waitFor(() => expect(bookService.updateBook).toHaveBeenCalled());
        const [, payload] = bookService.updateBook.mock.calls[0];
        expect(payload.quantity).toBe(6);
        expect(payload.expectedQuantity).toBe(5);
    });
});
