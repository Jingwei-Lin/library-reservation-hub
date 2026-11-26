// @vitest-environment jsdom
import { afterEach, beforeAll, describe, expect, it, vi } from 'vitest';
import { act, cleanup, fireEvent, render, screen } from '@testing-library/react';
import ChatPanel from './ChatPanel';

// A stand-in for the server's event stream: each test sends chunks and ends it by hand. (vi.mock is hoisted
// above the imports, so ChatPanel gets it.)
const streams = [];
vi.mock('event-source-polyfill', () => ({
    EventSourcePolyfill: class {
        constructor(url) {
            this.url = url;
            this.closed = false;
            this.listeners = {};
            streams.push(this);
        }
        addEventListener(name, fn) { this.listeners[name] = fn; }
        close() { this.closed = true; }
    },
}));


const ask = (text) => {
    fireEvent.change(screen.getByPlaceholderText('Type your message...'), { target: { value: text } });
    fireEvent.click(screen.getByRole('button', { name: 'Send message' }));
    return streams[streams.length - 1];
};
const send = (stream, chunk) => act(() => stream.onmessage({ data: chunk }));
// the server closing the stream reaches the page as an error event, after a reply as after a failure
const end = (stream) => act(() => stream.onerror());
const aiBubbles = () => Array.from(document.querySelectorAll('.ai-message .message-content'));

describe('ChatPanel', () => {
    beforeAll(() => {
        Element.prototype.scrollIntoView = () => {};
    });

    afterEach(() => {
        cleanup();
        streams.length = 0;
    });

    it('shows an image in a reply as a link, without loading it', () => {
        render(<ChatPanel />);
        const stream = ask('Show me a chart');

        send(stream, '![a chart](https://images.example/chart.png?d=secret)');
        end(stream);

        const bubble = aiBubbles()[0];
        expect(bubble.querySelector('img')).toBeNull();
        const link = bubble.querySelector('a');
        expect(link.textContent).toBe('a chart');
        expect(link.getAttribute('href')).toBe('https://images.example/chart.png?d=secret');
        expect(link.getAttribute('rel')).toContain('noopener');
    });

    it('says so when the stream ends without a reply', () => {
        render(<ChatPanel />);
        const stream = ask('Hello?');

        end(stream);

        expect(aiBubbles()[0].textContent).toMatch(/couldn't answer/i);
        expect(stream.closed).toBe(true);
    });

    it('keeps a reply that ended normally as it is', () => {
        render(<ChatPanel />);
        const stream = ask('Recommend a book');

        send(stream, 'Try ');
        send(stream, 'The Hobbit.');
        end(stream);

        expect(aiBubbles()[0].textContent.trim()).toBe('Try The Hobbit.');
        expect(stream.closed).toBe(true);
        // the server never sends a "complete" event, so nothing listens for one
        expect(stream.listeners.complete).toBeUndefined();
    });
});
