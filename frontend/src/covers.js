// Book cover images, served by the back end from static/books.

export const PLACEHOLDER_COVER = '/placeholder-book-cover.svg';

export const coverUrl = (path) => `/api/images/books/${encodeURIComponent(path)}`;

/**
 * onError for a cover <img>: logs the failure once and shows the placeholder. If the placeholder fails as well,
 * nothing more happens (no second log, no loop).
 */
export const showPlaceholderCover = (book) => (e) => {
    const img = e.currentTarget;
    if (img.dataset.placeholder) return;
    img.dataset.placeholder = 'true';
    console.error('Image failed to load:', {
        bookTitle: book.title,
        databasePath: book.path,
        imageSrc: coverUrl(book.path),
        timestamp: new Date().toISOString(),
    });
    img.src = PLACEHOLDER_COVER;
};
