// @vitest-environment jsdom
// Small page bugs from the code review (P3), each checked through the component that had it.
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import fs from 'node:fs';
import path from 'node:path';

vi.mock('../services/authApi', () => ({
    validateSession: vi.fn(async () => ({ code: 40100 })),
    logoutUser: vi.fn(async () => {}),
    loginUser: vi.fn(),
    registerUser: vi.fn(),
}));
vi.mock('../services/userService', () => ({
    userService: { getUserProfile: vi.fn(), updateUserProfile: vi.fn() },
}));
vi.mock('../services/bookService', () => ({
    bookService: { getBookById: vi.fn(), getRatingByBookId: vi.fn() },
}));
vi.mock('../services/reviewService', () => ({ reviewService: { getReviewsByBookId: vi.fn() } }));
vi.mock('../services/reservationService', () => ({
    reservationService: { hasActiveReservation: vi.fn(), createReservation: vi.fn() },
}));
vi.mock('../services/waitingListService', () => ({ waitingListService: { getMyWaitingList: vi.fn() } }));

import App from '../App';
import Login from './Login';
import Register from './Register';
import Profile from './Profile';
import Book from './Book';
import BookGrid from './dashboard/BookGrid';
import { userService } from '../services/userService';
import { bookService } from '../services/bookService';
import { reviewService } from '../services/reviewService';
import { reservationService } from '../services/reservationService';
import { waitingListService } from '../services/waitingListService';

const inRouter = (ui) => render(<MemoryRouter>{ui}</MemoryRouter>);
const ymd = (d) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
const daysFromNow = (n) => { const d = new Date(); d.setDate(d.getDate() + n); return ymd(d); };

beforeEach(() => {
    Element.prototype.scrollIntoView = () => {};
});

afterEach(() => {
    cleanup();
    vi.clearAllMocks();
    vi.restoreAllMocks();
    localStorage.clear();
    sessionStorage.clear();
});

describe('book covers', () => {
    it('the placeholder cover exists', () => {
        expect(fs.existsSync(path.join(process.cwd(), 'public', 'placeholder-book-cover.svg'))).toBe(true);
    });

    it('a cover that fails shows the placeholder once, without another round if that fails too', () => {
        const log = vi.spyOn(console, 'error').mockImplementation(() => {});
        render(<BookGrid books={[{ bookId: 1, title: 'Dune', path: 'missing.jpg' }]} onSelect={() => {}} />);
        const img = screen.getByAltText('Dune');

        fireEvent.error(img);
        expect(img.getAttribute('src')).toBe('/placeholder-book-cover.svg');

        fireEvent.error(img);
        expect(log).toHaveBeenCalledTimes(1);
    });
});

describe('routes', () => {
    it('an unknown address shows a not-found page with a way back', async () => {
        window.history.pushState({}, '', '/no-such-page');
        render(<App />);

        expect(await screen.findByText(/page not found/i)).toBeTruthy();
        expect(screen.getByRole('link', { name: /books/i }).getAttribute('href')).toBe('/dashboard');
    });
});

describe('dark mode', () => {
    it('reaches the sign-in page', () => {
        localStorage.setItem('theme', 'dark');
        const { container } = inRouter(<Login setIsAuthenticated={() => {}} setUser={() => {}} />);

        expect(container.querySelector('.login-container').classList.contains('dark-mode')).toBe(true);
    });

    it('reaches the registration page', () => {
        localStorage.setItem('theme', 'dark');
        const { container } = inRouter(<Register />);

        expect(container.querySelector('.register-container').classList.contains('dark-mode')).toBe(true);
    });
});

describe('profile', () => {
    beforeEach(() => {
        userService.getUserProfile.mockResolvedValue({
            code: 0, data: { firstName: 'Jing', lastName: 'Lin', userAccount: 'jing@example.com', phone: null },
        });
        userService.updateUserProfile.mockResolvedValue({ code: 0 });
    });

    it('loads a profile without a phone without React warnings', async () => {
        const log = vi.spyOn(console, 'error').mockImplementation(() => {});
        inRouter(<Profile user={{ id: 1 }} setUser={() => {}} />);
        await screen.findByDisplayValue('Jing');

        fireEvent.change(screen.getByDisplayValue('Jing'), { target: { name: 'firstName', value: 'Jingwei' } });

        expect(log.mock.calls.map(c => String(c[0])).filter(m => m.includes('uncontrolled'))).toEqual([]);
    });

    it('a saved name reaches the rest of the app', async () => {
        const setUser = vi.fn();
        inRouter(<Profile user={{ id: 1, firstName: 'Jing' }} setUser={setUser} />);
        const first = await screen.findByDisplayValue('Jing');

        fireEvent.change(first, { target: { name: 'firstName', value: 'Jingwei' } });
        fireEvent.submit(first.closest('form'));

        await waitFor(() => expect(setUser).toHaveBeenCalled());
        expect(setUser.mock.calls[0][0]).toMatchObject({ id: 1, firstName: 'Jingwei', lastName: 'Lin' });
    });
});

describe('book page', () => {
    beforeEach(() => {
        bookService.getBookById.mockResolvedValue({
            code: 0, data: { bookId: 1, title: 'Dune', author: 'Frank Herbert', genre: 'Science Fiction', available: true, quantity: 2 },
        });
        bookService.getRatingByBookId.mockResolvedValue({ code: 0, data: null });
        reviewService.getReviewsByBookId.mockResolvedValue({ code: 0, data: [] });
        reservationService.hasActiveReservation.mockResolvedValue({ code: 0, data: false });
        reservationService.createReservation.mockResolvedValue({ code: 0, data: 7 });
        waitingListService.getMyWaitingList.mockResolvedValue({ code: 0, data: [] });
    });

    it('reserving does not reload the whole page', async () => {
        inRouter(<Book isAuthenticated setIsAuthenticated={() => {}} id={1} onBack={() => {}} />);
        fireEvent.click(await screen.findByRole('button', { name: 'Reserve book' }));
        fireEvent.change(document.getElementById('pickupTime'), { target: { value: daysFromNow(1) } });
        fireEvent.change(document.getElementById('dueTime'), { target: { value: daysFromNow(5) } });
        fireEvent.click(screen.getByRole('button', { name: 'Confirm Reservation' }));

        await screen.findByText('Reservation created successfully!');
        await new Promise(r => setTimeout(r, 50));

        expect(bookService.getBookById).toHaveBeenCalledTimes(1);
        expect(screen.getByText('You have already reserved this book.')).toBeTruthy();
    });
});
