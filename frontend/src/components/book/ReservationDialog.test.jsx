// @vitest-environment jsdom
// The Reserve dialog's dates are the user's local dates. In Sydney before 11 am, UTC is still on yesterday.
import { afterEach, beforeAll, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import ReservationDialog from './ReservationDialog';

vi.mock('../../services/reservationService', () => ({ reservationService: { createReservation: vi.fn() } }));

describe('ReservationDialog dates', () => {
    beforeAll(() => {
        process.env.TZ = 'Australia/Sydney';
    });

    afterEach(() => {
        cleanup();
        vi.useRealTimers();
    });

    const open = () => render(
        <MemoryRouter>
            <ReservationDialog book={{ title: 'Dune' }} bookId={1} isAuthenticated onClose={() => {}} onReserved={() => {}} />
        </MemoryRouter>
    );

    it('starts the pickup dates at the local today, early in the morning too', () => {
        vi.useFakeTimers({ toFake: ['Date'] });
        // 08:00 on 27 November in Sydney is still 26 November in UTC
        vi.setSystemTime(new Date('2026-11-26T21:00:00Z'));

        open();
        const pickup = document.getElementById('pickupTime');

        expect(pickup.getAttribute('min')).toBe('2026-11-27');
        expect(pickup.getAttribute('max')).toBe('2026-12-04');
    });

    it('gives the latest due date 30 local days after the pickup', () => {
        vi.useFakeTimers({ toFake: ['Date'] });
        vi.setSystemTime(new Date('2026-11-26T21:00:00Z'));

        open();
        fireEvent.change(document.getElementById('pickupTime'), { target: { value: '2026-11-28' } });

        expect(document.getElementById('dueTime').getAttribute('max')).toBe('2026-12-28');
    });
});
