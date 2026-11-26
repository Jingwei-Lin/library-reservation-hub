import { afterEach, describe, expect, it, vi } from 'vitest';
import { filterReservations, includesQuery, isAdminUser, isDueToday, isOverdue, pageCount } from './adminUtils';

describe('admin helpers', () => {
    afterEach(() => {
        vi.useRealTimers();
    });

    it('matches a search case-insensitively, null as empty', () => {
        expect(includesQuery('The Hobbit', 'hob')).toBe(true);
        expect(includesQuery(42, '4')).toBe(true);
        expect(includesQuery(null, 'x')).toBe(false);
    });

    it('counts at least one page', () => {
        expect(pageCount(0, 10)).toBe(1);
        expect(pageCount(undefined, 10)).toBe(1);
        expect(pageCount(21, 10)).toBe(3);
    });

    it('loads only for a signed-in admin', () => {
        expect(isAdminUser({ isAdmin: true }, true)).toBe(true);
        expect(isAdminUser({ userRole: 'admin' }, true)).toBe(true);
        expect(isAdminUser({ isAdmin: true }, false)).toBe(false);
        expect(isAdminUser({ userRole: 'user' }, true)).toBe(false);
    });

    it('filters reservations by status', () => {
        const records = ['reserved', 'borrowed', 'overdue', 'returned', 'cancelled']
            .map((reservationStatus, i) => ({ reservationId: i + 1, reservationStatus }));
        const ids = (filter) => filterReservations(records, filter).map(r => r.reservationId);

        expect(ids('borrowed')).toEqual([2, 3]);
        expect(ids('active')).toEqual([1, 2, 3]);
        expect(ids('all')).toEqual([1, 2, 3, 4, 5]);
    });

    it('marks a reservation due today or overdue, unless it is finished', () => {
        vi.useFakeTimers();
        vi.setSystemTime(new Date(2026, 10, 26, 12, 0));

        expect(isDueToday(new Date(2026, 10, 26, 23, 59).toISOString())).toBe(true);
        expect(isDueToday(new Date(2026, 10, 27, 9, 0).toISOString())).toBe(false);
        expect(isOverdue(new Date(2026, 10, 25).toISOString(), 'borrowed')).toBe(true);
        expect(isOverdue(new Date(2026, 10, 25).toISOString(), 'returned')).toBe(false);
        expect(isOverdue(new Date(2026, 10, 27).toISOString(), 'borrowed')).toBe(false);
        expect(isOverdue(null, 'borrowed')).toBe(false);
    });
});
