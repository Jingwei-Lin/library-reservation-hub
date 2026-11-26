/**
 * The page to go back to after signing in, from the ?redirect= parameter. Only a path inside this app is
 * followed ("/reservations"), never another site ("//evil.example", "https://…"), so the sign-in link can't be
 * used to send someone elsewhere.
 */
export const safeRedirect = (value, fallback) =>
    typeof value === 'string' && value.startsWith('/') && !value.startsWith('//') && !value.startsWith('/\\')
        ? value
        : fallback;
