import { describe, expect, it } from 'vitest';
import { safeRedirect } from './safeRedirect';

describe('safeRedirect', () => {
    it('follows a path inside the app', () => {
        expect(safeRedirect('/reservations', '/dashboard')).toBe('/reservations');
        expect(safeRedirect('/dashboard?book=3&page=2', '/x')).toBe('/dashboard?book=3&page=2');
    });

    it.each([
        ['another site, scheme-relative', '//evil.example'],
        ['another site, with a backslash', '/\\evil.example'],
        ['an absolute URL', 'https://evil.example/'],
        ['a javascript: URL', 'javascript:alert(1)'],
        ['a relative path', 'reservations'],
        ['nothing', null],
        ['not a string', 42],
    ])('falls back for %s', (_, value) => {
        expect(safeRedirect(value, '/dashboard')).toBe('/dashboard');
    });
});
