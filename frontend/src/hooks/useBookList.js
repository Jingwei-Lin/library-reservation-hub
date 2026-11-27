import { useRef, useState } from "react";
import { bookService } from "../services/bookService";
import { isSessionEnded } from "../services/api";
import { BOOKS_PER_PAGE } from "../pagination";

/** Books with a usable id (the lists link to each book by id) */
const withValidIds = (list) => list.filter(book => {
    const bookId = Number(book.bookId);
    return book.bookId != null && !isNaN(bookId) && bookId > 0;
});

/**
 * The Dashboard's book list: what it shows, and the loads that fill it.
 * - All Books and search are paged by the server (`books` holds one page, `totalBooks` the server's total);
 *   a genre and the top 10 are small lists, paged by the page (`serverPaged` false).
 * - Each load gets a number; only the latest one may change the list, so a slow earlier load can't replace a
 *   newer one.
 */
export const useBookList = (perPage = BOOKS_PER_PAGE) => {
    const [books, setBooks] = useState([]);
    const [currentTitle, setCurrentTitle] = useState("All Books");
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [serverPaged, setServerPaged] = useState(true);
    const [totalBooks, setTotalBooks] = useState(0);
    const latestLoad = useRef(0);

    // All Books, or a search: one page from the server, with the server's total
    const fetchBooks = async (keyword = "", page = 1) => {
        const load = ++latestLoad.current;
        try {
            setLoading(true);
            setError("");
            const response = keyword
                ? await bookService.searchBooks(keyword, page, perPage)
                : await bookService.getAllBooks(page, perPage);
            if (load !== latestLoad.current) return;   // a newer list was asked for meanwhile
            if (response && response.code === 0 && response.data) {
                setBooks(response.data.records || []);
                setTotalBooks(response.data.total ?? 0);
                setServerPaged(true);
                setCurrentTitle(keyword ? `Search Results for "${keyword}"` : "All Books");
            } else {
                setError("Invalid response format");
                setBooks([]);
                setTotalBooks(0);
            }
        } catch (err) {
            if (load !== latestLoad.current) return;
            if (!isSessionEnded(err)) {
                setError(err.message || "Failed to load books");
                console.error("Error fetching books:", err);
                setBooks([]);
                setTotalBooks(0);
            }
        } finally {
            if (load === latestLoad.current) setLoading(false);
        }
    };

    // a small list, paged here
    const showWholeList = (list, title) => {
        setBooks(list);
        setTotalBooks(list.length);
        setServerPaged(false);
        setCurrentTitle(title);
    };

    const loadTop10Books = async () => {
        const load = ++latestLoad.current;
        try {
            setLoading(true);
            setError("");
            const response = await bookService.getTop10Books();
            if (load !== latestLoad.current) return;

            const validBooks = withValidIds(response && response.code === 0 && Array.isArray(response.data) ? response.data : []);
            if (validBooks.length === 0) {
                setError("No top-rated books available yet.");
                showWholeList([], "Top 10 Rated Books");
            } else {
                showWholeList(validBooks, "Top 10 Rated Books");
            }
        } catch (err) {
            if (load !== latestLoad.current) return;
            console.error("Error loading top 10 books:", err);
            setError("Failed to load top 10 books");
            showWholeList([], "Top 10 Rated Books");
        } finally {
            if (load === latestLoad.current) setLoading(false);
        }
    };

    // one genre's books; the caller updates the URL
    const loadGenreBooks = async (genre) => {
        const load = ++latestLoad.current;
        try {
            setLoading(true);
            setError("");
            const response = await bookService.getBooksByGenre(genre);
            if (load !== latestLoad.current) return;

            if (response && response.code === 0 && response.data) {
                const bookList = Array.isArray(response.data) ? response.data : response.data.records || [];
                showWholeList(withValidIds(bookList), `${genre} Books`);
            } else {
                setError("Invalid response format for genre");
                showWholeList([], `${genre} Books`);
            }
        } catch (err) {
            if (load !== latestLoad.current) return;
            console.error("Error loading genre books:", err);
            setError(`Failed to load ${genre} books: ${err.message}`);
            showWholeList([], `${genre} Books`);
        } finally {
            if (load === latestLoad.current) setLoading(false);
        }
    };

    return {
        books, currentTitle, loading, setLoading, error, setError, serverPaged, totalBooks,
        fetchBooks, loadTop10Books, loadGenreBooks,
    };
};
