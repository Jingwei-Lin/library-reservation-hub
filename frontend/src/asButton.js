/**
 * Props that make an element that isn't a <button> (a menu <li>, a card <div>) work like one: announced as a
 * button, reachable with Tab, and activated by Enter or Space as well as by a click. Used where a real <button>
 * would change the layout the styles expect.
 */
export const asButton = (onActivate) => ({
    role: 'button',
    tabIndex: 0,
    onClick: onActivate,
    onKeyDown: (e) => {
        if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault();   // Space would otherwise scroll the page
            onActivate(e);
        }
    },
});
