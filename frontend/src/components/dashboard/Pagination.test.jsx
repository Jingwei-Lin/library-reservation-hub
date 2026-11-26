// @vitest-environment jsdom
import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import Pagination from './Pagination';

const labels = () => Array.from(document.querySelector('.pagination').children).map((el) => el.textContent.trim());

describe('Pagination', () => {
    afterEach(cleanup);

    it('shows Previous, the page buttons with gaps, and Next', () => {
        render(<Pagination currentPage={5} totalPages={9} onPage={() => {}} />);

        expect(labels()).toEqual(['Previous', '1', '...', '4', '5', '6', '...', '9', 'Next']);
        expect(screen.getByRole('button', { name: '5' }).className).toContain('active');
        expect(screen.getByRole('button', { name: '4' }).className).not.toContain('active');
    });

    it('goes to the page clicked, and to the next and previous ones', () => {
        const onPage = vi.fn();
        render(<Pagination currentPage={5} totalPages={9} onPage={onPage} />);

        fireEvent.click(screen.getByRole('button', { name: '9' }));
        fireEvent.click(screen.getByRole('button', { name: 'Next' }));
        fireEvent.click(screen.getByRole('button', { name: 'Previous' }));

        expect(onPage.mock.calls).toEqual([[9], [6], [4]]);
    });

    it('disables Previous on the first page and Next on the last', () => {
        const { rerender } = render(<Pagination currentPage={1} totalPages={3} onPage={() => {}} />);
        expect(screen.getByRole('button', { name: 'Previous' }).disabled).toBe(true);
        expect(screen.getByRole('button', { name: 'Next' }).disabled).toBe(false);

        rerender(<Pagination currentPage={3} totalPages={3} onPage={() => {}} />);
        expect(screen.getByRole('button', { name: 'Previous' }).disabled).toBe(false);
        expect(screen.getByRole('button', { name: 'Next' }).disabled).toBe(true);
    });

    it('has a single page when the list fits on one', () => {
        render(<Pagination currentPage={1} totalPages={1} onPage={() => {}} />);

        expect(labels()).toEqual(['Previous', '1', 'Next']);
    });
});
