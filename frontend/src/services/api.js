// The one HTTP client every service uses.
//
// The server answers every request with HTTP 200 and a body { code, data, message }. A session that has ended
// shows up as code 40100 (signed out: a timeout, or signed out elsewhere after a password change) or 40301
// (banned). The interceptor below spots those once, for every call, tells the app (which signs the user out and
// shows a notice), and rejects the call with a SessionEndedError that pages can recognise and ignore.
import axios from 'axios';

const ENDED = { 40100: 'expired', 40301: 'banned' };

// calls that answer 40100 for a guest by design (or 40301 for a banned sign-in): they never mean a session ended
const NOT_A_SESSION_END = ['/user/get/login', '/user/login', '/user/logout', '/user/register'];

let sessionEndedHandler = () => false;

/**
 * The app registers what to do when a session ends. The handler returns true when the user was signed in (or
 * the session has just ended), so the call is rejected; false for a guest, whose page handles the answer itself.
 */
export const setSessionEndedHandler = (handler) => {
    sessionEndedHandler = handler;
};

export class SessionEndedError extends Error {
    constructor(reason) {
        super(reason === 'banned' ? 'Your account has been suspended.' : 'Your session has ended. Please sign in again.');
        this.name = 'SessionEndedError';
        this.reason = reason;
    }
}

export const isSessionEnded = (error) => error instanceof SessionEndedError;

/** For a session check that is not a call through this client: true when a signed-in session has just ended */
export const reportSessionEnded = (reason = 'expired') => sessionEndedHandler(reason);

/** A session end is passed on as it is; any other failure becomes an Error with the server's message */
export const toError = (error, fallback) =>
    isSessionEnded(error) ? error : new Error(error?.response?.data?.message || error?.message || fallback);

const api = axios.create({
    baseURL: '/api',
    headers: { 'Content-Type': 'application/json' },
    withCredentials: true, // the session cookie
});

api.interceptors.response.use((response) => {
    const reason = ENDED[response.data?.code];
    const url = response.config.url || '';
    if (reason && !NOT_A_SESSION_END.some((path) => url.startsWith(path)) && sessionEndedHandler(reason)) {
        return Promise.reject(new SessionEndedError(reason));
    }
    return response;
});

export default api;
