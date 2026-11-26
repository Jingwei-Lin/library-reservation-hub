// Paging for the book list. All Books and search are paged by the server (the list holds one page and the server
// gives the total); a genre and the top 10 are small lists, paged here.

export const BOOKS_PER_PAGE = 12;

/** The page to show: how many pages there are, and the books on the current one */
export const pageOf = ({ books, serverPaged, totalBooks, currentPage, perPage = BOOKS_PER_PAGE }) => {
    const totalCount = serverPaged ? totalBooks : books.length;
    const totalPages = Math.max(1, Math.ceil(totalCount / perPage));
    const first = (currentPage - 1) * perPage;
    return {
        totalCount,
        totalPages,
        currentBooks: serverPaged ? books : books.slice(first, first + perPage),
    };
};

/**
 * The page buttons: every page up to 5, otherwise the first and last pages, the pages around the current one,
 * and "..." for the gaps.
 */
export const pageButtons = (currentPage, totalPages) => {
    if (totalPages <= 5) {
        return Array.from({ length: totalPages }, (_, i) => i + 1);
    }
    if (currentPage <= 3) {
        return [1, 2, 3, 4, '...', totalPages];
    }
    if (currentPage >= totalPages - 2) {
        return [1, '...', totalPages - 3, totalPages - 2, totalPages - 1, totalPages];
    }
    return [1, '...', currentPage - 1, currentPage, currentPage + 1, '...', totalPages];
};
