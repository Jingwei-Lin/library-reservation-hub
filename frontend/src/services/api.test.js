import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import api, { SessionEndedError, isSessionEnded, setSessionEndedHandler, toError } from './api';

// Every call answers HTTP 200 with the given body, as the server does; nothing goes over the network.
const answer = (body) => {
    api.defaults.adapter = async (config) => ({ data: body, status: 200, statusText: 'OK', headers: {}, config });
};

describe('the session-ended interceptor', () => {
    let handler;

    beforeEach(() => {
        handler = vi.fn(() => true);   // a signed-in user
        setSessionEndedHandler(handler);
    });

    afterEach(() => {
        setSessionEndedHandler(() => false);
    });

    it('turns 40100 into a SessionEndedError and tells the app once', async () => {
        answer({ code: 40100, message: 'Not logged in' });

        const error = await api.get('/reservation/my').catch((e) => e);

        expect(error).toBeInstanceOf(SessionEndedError);
        expect(error.reason).toBe('expired');
        expect(isSessionEnded(error)).toBe(true);
        expect(handler).toHaveBeenCalledTimes(1);
        expect(handler).toHaveBeenCalledWith('expired');
    });

    it('turns 40301 into a ban', async () => {
        answer({ code: 40301, message: 'Banned' });

        const error = await api.get('/waiting-list/my').catch((e) => e);

        expect(error.reason).toBe('banned');
        expect(error.message).toBe('Your account has been suspended.');
        expect(handler).toHaveBeenCalledWith('banned');
    });

    it("lets a guest's page handle the answer itself", async () => {
        handler.mockReturnValue(false);
        answer({ code: 40100, message: 'Not logged in' });

        const response = await api.get('/review/add/1');

        expect(response.data.code).toBe(40100);
        expect(handler).toHaveBeenCalledTimes(1);
    });

    it.each(['/user/get/login', '/user/login', '/user/logout', '/user/register'])(
        'never treats %s as a session end',
        async (url) => {
            answer({ code: 40100, message: 'Not logged in' });

            const response = await api.get(url);

            expect(response.data.code).toBe(40100);
            expect(handler).not.toHaveBeenCalled();
        },
    );

    it('passes other answers through', async () => {
        answer({ code: 0, data: { bookId: 1 } });

        const response = await api.get('/book/1');

        expect(response.data.data.bookId).toBe(1);
        expect(handler).not.toHaveBeenCalled();
    });
});

describe('toError', () => {
    it('passes a session end on as it is', () => {
        const ended = new SessionEndedError('expired');

        expect(toError(ended, 'Failed')).toBe(ended);
    });

    it("uses the server's message, then the error's, then the fallback", () => {
        expect(toError({ response: { data: { message: 'Book not found' } } }, 'Failed').message).toBe('Book not found');
        expect(toError(new Error('Network Error'), 'Failed').message).toBe('Network Error');
        expect(toError({}, 'Failed').message).toBe('Failed');
    });
});
