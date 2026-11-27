// @vitest-environment jsdom
// The menus, Quick Actions, book cards and header controls work from the keyboard: each is announced as a
// button, can be reached with Tab, and Enter or Space activates it.
import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import Sidebar from './dashboard/Sidebar';
import StatsPanel from './dashboard/StatsPanel';
import BookGrid from './dashboard/BookGrid';
import Header from './Header';

const press = (el, key) => fireEvent.keyDown(el, { key });

const expectButton = (el) => {
    expect(el.getAttribute('role')).toBe('button');
    expect(el.tabIndex).toBe(0);
};

afterEach(() => {
    cleanup();
    localStorage.clear();
});

describe('keyboard access', () => {
    it('the sidebar items', () => {
        const onAllBooks = vi.fn();
        const onGenre = vi.fn();
        const onLeave = vi.fn();
        render(<Sidebar hasNotification={false} onAllBooks={onAllBooks} onGenre={onGenre} onLeave={onLeave} />);

        const all = screen.getByText('All Books');
        expectButton(all);
        press(all, 'Enter');
        press(screen.getByText('My Reservations'), ' ');
        press(screen.getByText('Poetry/Classic'), 'Enter');
        press(screen.getByText('Fantasy'), 'Tab');

        expect(onAllBooks).toHaveBeenCalledTimes(1);
        expect(onLeave).toHaveBeenCalledWith('/reservations');
        expect(onGenre.mock.calls).toEqual([['Poetry Classic']]);
    });

    it('the Top 10 quick action', () => {
        const onTop10 = vi.fn();
        render(<StatsPanel totalStats={{ totalBooks: 1, totalAvailable: 1, totalCategories: 1 }} onTop10={onTop10} />);

        const top10 = screen.getByText('Top 10 Rated Books');
        expectButton(top10);
        press(top10, 'Enter');

        expect(onTop10).toHaveBeenCalledTimes(1);
    });

    it('a book card', () => {
        const onSelect = vi.fn();
        const book = { bookId: 3, title: 'Dune' };
        render(<BookGrid books={[book]} onSelect={onSelect} />);

        const card = screen.getByText('Dune').closest('.book-card');
        expectButton(card);
        press(card, ' ');

        expect(onSelect).toHaveBeenCalledWith(book);
    });

    it('the theme toggle and the user menu', () => {
        render(
            <MemoryRouter>
                <Header user={{ id: 1, firstName: 'Jing' }} onSignOut={() => {}} isAuthenticated
                        searchTerm="" setSearchTerm={() => {}} handleSearch={() => {}} />
            </MemoryRouter>
        );

        const toggle = screen.getByLabelText('Switch to dark mode');
        expectButton(toggle);
        press(toggle, 'Enter');
        expect(localStorage.getItem('theme')).toBe('dark');

        const userIcon = screen.getByLabelText('User menu');
        expectButton(userIcon);
        expect(userIcon.getAttribute('aria-expanded')).toBe('false');
        press(userIcon, 'Enter');
        expect(userIcon.getAttribute('aria-expanded')).toBe('true');
    });
});
