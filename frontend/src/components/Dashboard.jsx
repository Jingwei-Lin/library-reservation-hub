// Dashboard.jsx
import React, { useState, useEffect, useRef } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import { bookService } from "../services/bookService";
import { waitingListService } from "../services/waitingListService";
import { validateSession } from "../services/authApi";
import { isSessionEnded, reportSessionEnded } from "../services/api";
import Header from "./Header";

import ChatPanel from "./ChatPanel";

import Book from './Book';
import { getTheme } from "../themeUtils";

import "./Dashboard.css";
import { BOOKS_PER_PAGE, pageButtons, pageOf } from '../pagination';

const Dashboard = ({ user, setUser, setIsAuthenticated, isAuthenticated, onSignOut }) => {
    const navigate = useNavigate();
    const location = useLocation();

    // Get initial state from URL parameters
    const urlParams = new URLSearchParams(location.search);
    const initialSearch = urlParams.get('search') || '';
    const initialGenre = urlParams.get('genre') || '';
    const initialView = urlParams.get('view') || '';
    const initialPage = parseInt(urlParams.get('page')) || 1;

    const [books, setBooks] = useState([]);
    const [currentTitle, setCurrentTitle] = useState("All Books");
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    // what is typed in the search box, and the search the list is showing (they differ until Search is pressed);
    // both start from ?search=, so a reload keeps the search
    const [searchTerm, setSearchTerm] = useState(initialSearch);
    const [activeSearch, setActiveSearch] = useState(initialSearch);
    // All Books and search are paged by the server (books holds one page, totalBooks the server's total);
    // a genre and the top 10 are small lists, paged here
    const [serverPaged, setServerPaged] = useState(true);
    const [totalBooks, setTotalBooks] = useState(0);
    // each list load gets a number; only the latest one may change the list, so a slow earlier load can't
    // replace a newer one
    const latestLoad = useRef(0);
    const [currentPage, setCurrentPage] = useState(initialPage);
    const [selectedBookId, setSelectedBookId] = useState(() => {
        const params = new URLSearchParams(location.search);
        const bookIdFromUrl = params.get('book');
        const id = Number(bookIdFromUrl);
        return (!isNaN(id) && id > 0) ? id : null;
    });
    const [theme, setTheme] = useState(getTheme());
    const [hasNotification, setHasNotification] = useState(false);
    const [currentGenre, setCurrentGenre] = useState(initialGenre);
    const [currentView, setCurrentView] = useState(initialView);

    const [totalStats, setTotalStats] = useState({
        totalBooks: 0,
        totalAvailable: 0,
        totalCategories: 0
    });

    const booksPerPage = BOOKS_PER_PAGE;

    // Update URL when search/filter state changes
    const updateURL = (updates = {}) => {
        const params = new URLSearchParams();

        // Only include book if explicitly provided in updates or if we're on a book page
        if (updates.book !== undefined) {
            // If updates explicitly sets book, use that value
            if (updates.book) {
                params.set('book', updates.book);
            }
            // If updates sets book to null/undefined, don't include it
        } else if (selectedBookId) {
            // If no book in updates but we have selectedBookId, include it
            params.set('book', selectedBookId.toString());
        }

        if (activeSearch) params.set('search', activeSearch);
        if (currentGenre) params.set('genre', currentGenre);
        if (currentView) params.set('view', currentView);

        if (updates.page !== undefined) {
            if (updates.page > 1) {
                params.set('page', updates.page.toString());
            } else {
                params.delete('page');
            }
        } else if (currentPage > 1) {
            params.set('page', currentPage.toString());
        }

        // Apply updates
        Object.keys(updates).forEach(key => {
            if (updates[key]) {
                params.set(key, updates[key]);
            } else {
                params.delete(key);
            }
        });

        navigate(`/dashboard?${params.toString()}`, { replace: true });
    };

    // Initial data fetch
    useEffect(() => {
        const initializeDashboard = async () => {
            try {
                // If we're on a book page, only load book-specific data
                if (selectedBookId) {
                    // the book page loads its own data
                }
                // If we're on Top 10 view, load top 10 books directly
                else if (initialView === 'top10') {
                    await loadTop10Books();
                }
                // If we have a search term, load search results
                else if (initialSearch) {
                    await fetchBooks(initialSearch, initialPage);
                }
                // If we have a genre filter, load genre books
                else if (initialGenre) {
                    await loadGenreBooks(initialGenre);
                }
                // Otherwise load all books
                else {
                    await fetchBooks('', initialPage);
                }

                // Parallelize non-critical fetches (waiting list, user, statistics)
                const [waitingListRes, userRes] = await Promise.all([
                    waitingListService.getMyWaitingList().catch(err => ({ code: -1, message: err.message })),
                    validateSession().catch(err => ({ code: -1, message: err.message })),
                    fetchTotalStats()
                ]);

                // Process responses (handle errors individually to not block everything)
                if (waitingListRes.code === 0 && waitingListRes.data) {
                    const hasNotified = waitingListRes.data.some(item => item.status === "notified");
                    setHasNotification(hasNotified);
                } else {
                    console.warn('Waiting list fetch failed:', waitingListRes.message);
                    setHasNotification(false);
                }
                if (userRes.code === 0 && userRes.data) {
                    setUser(userRes.data);
                    setIsAuthenticated(true);
                } else {
                    // a signed-in session that has ended gets the app's notice; a guest simply stays a guest
                    if (!reportSessionEnded('expired')) {
                        setIsAuthenticated(false);
                    }
                    return; // Early exit if logged out
                }
                updateURL();
            } catch (error) {
                console.error('Error during initialization:', error);
                setError('Failed to initialize dashboard. Please try again.');
            } finally {
                setLoading(false); // Always stop loading, success or fail
            }
        };

        initializeDashboard();
    }, []);

    // Apply theme from localStorage
    useEffect(() => {
        document.body.classList.remove("light", "dark");
        document.body.classList.add(theme);
    }, [theme]);

    // Listen for custom themeChange event (in-page sync)
    useEffect(() => {
        const handleThemeChange = (event) => {
            setTheme(event.detail.theme);
        };
        window.addEventListener("themeChange", handleThemeChange);
        return () => {
            window.removeEventListener("themeChange", handleThemeChange);
        };
    }, []);

    // Poll localStorage for theme changes (cross-tab sync)
    useEffect(() => {
        const handleStorageChange = () => {
            setTheme(getTheme());
        };
        window.addEventListener("storage", handleStorageChange);
        return () => {
            window.removeEventListener("storage", handleStorageChange);
        };
    }, []);


    // Sync URL with selected book state
    useEffect(() => {
        const params = new URLSearchParams(location.search);
        const bookIdFromUrl = params.get('book');

        if (bookIdFromUrl) {
            const id = Number(bookIdFromUrl);
            if (!isNaN(id) && id > 0) {
                setSelectedBookId(id);
            } else {
                updateURL({ book: null });
            }
        } else {
            setSelectedBookId(null);
        }
    }, [location.search, navigate]);


    // All Books, or a search: one page from the server, with the server's total
    const fetchBooks = async (keyword = "", page = 1) => {
        const load = ++latestLoad.current;
        try {
            setLoading(true);
            setError("");
            const response = keyword
                ? await bookService.searchBooks(keyword, page, booksPerPage)
                : await bookService.getAllBooks(page, booksPerPage);
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

    // counted by the server over every listed book, not from the page on screen
    const fetchTotalStats = async () => {
        try {
            const response = await bookService.getStats();
            if (response && response.code === 0 && response.data) {
                setTotalStats({
                    totalBooks: response.data.totalBooks,
                    totalAvailable: response.data.availableBooks,
                    totalCategories: response.data.categories
                });
            }
        } catch (err) {
            console.error("Error fetching total statistics:", err);
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

            const bookList = response && response.code === 0 && Array.isArray(response.data) ? response.data : [];
            const validBooks = bookList.filter(book => {
                const bookId = Number(book.bookId);
                return book.bookId != null && !isNaN(bookId) && bookId > 0;
            });
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

    // Helper function to load genre books without setting URL states
    const loadGenreBooks = async (genre) => {
        const load = ++latestLoad.current;
        try {
            setLoading(true);
            setError("");
            const response = await bookService.getBooksByGenre(genre);
            if (load !== latestLoad.current) return;

            if (response && response.code === 0 && response.data) {
                const bookList = Array.isArray(response.data) ? response.data : response.data.records || [];
                const validBooks = bookList.filter(book => {
                    const bookId = Number(book.bookId);
                    return book.bookId != null && !isNaN(bookId) && bookId > 0;
                });
                showWholeList(validBooks, `${genre} Books`);
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

    // a new search always starts at its first page
    const handleSearch = (e) => {
        e.preventDefault();
        const keyword = searchTerm.trim();
        setSelectedBookId(null);
        setCurrentGenre('');
        setCurrentView('');
        setActiveSearch(keyword);
        setCurrentPage(1);
        fetchBooks(keyword, 1);
        updateURL({ book: null, search: keyword, genre: '', view: '', page: 1 });
    };

    const handleGenreClick = async (genre) => {
        try {
            setSelectedBookId(null);
            setCurrentGenre(genre);
            setCurrentView('');
            setSearchTerm('');
            setActiveSearch('');
            setCurrentPage(1); // Explicitly set page to 1
            // the URL changes now, not when the load ends: a later click must not be overwritten by this one
            updateURL({ book: null, genre: genre, view: '', search: '', page: 1 });
            await loadGenreBooks(genre);
        } catch (err) {
            console.error("Error in handleGenreClick:", err);
            setError(`Failed to load ${genre} books`);
        }
    };

    const handleBookClick = (bookId) => {
        const id = Number(bookId);
        if (!bookId || isNaN(id) || id <= 0) {
            console.error('Invalid bookId:', bookId);
            setError('Cannot navigate to book with invalid ID');
            return;
        }
        setSelectedBookId(id);
        // the list keeps its page, so Back returns to it
        updateURL({ book: id.toString() });
    };

    const handleBackFromBook = () => {
        setSelectedBookId(null);
        updateURL({ book: null });
    };

    const handleTop10Click = async () => {
        try {
            setSelectedBookId(null);
            setCurrentView('top10');
            setCurrentGenre('');
            setSearchTerm('');
            setActiveSearch('');
            setCurrentPage(1);
            updateURL({ book: null, view: 'top10', genre: '', search: '', page: 1 });
            await loadTop10Books();
        } catch (err) {
            console.error("Error in handleTop10Click:", err);
            setError("Failed to load top 10 books");
        }
    };

    const handleAllBooks = () => {
        setSelectedBookId(null);
        setSearchTerm("");
        setActiveSearch("");
        setCurrentGenre("");
        setCurrentView("");
        setCurrentPage(1);
        fetchBooks('', 1);
        updateURL({ book: null, search: '', genre: '', view: '', page: 1 });
    };


    // Pagination: a server-paged list already holds just this page; a small list is sliced here
    const { totalCount, totalPages, currentBooks } = pageOf({ books, serverPaged, totalBooks, currentPage, perPage: booksPerPage });

    const paginate = (pageNumber) => {
        setCurrentPage(pageNumber);
        updateURL({ page: pageNumber });
        if (serverPaged) {
            fetchBooks(activeSearch, pageNumber);
        }
    };

    const nextPage = () => paginate(Math.min(currentPage + 1, totalPages));

    const prevPage = () => paginate(Math.max(currentPage - 1, 1));

    const getPaginationButtons = () => pageButtons(currentPage, totalPages);

    return (
        <div className={`dashboard ${theme === "dark" ? "dark-mode" : ""}`}>
            <nav className="sidebar">
                <div className="logo">
                    <span className="logo-icon">📚</span>
                    <span className="logo-text">Library Hub</span>
                </div>
                <div className="nav-box">
                    <h3>Navigation</h3>
                    <ul>
                        <li onClick={handleAllBooks}>
                            All Books
                        </li>
                        <li onClick={() => {
                            setSelectedBookId(null);
                            updateURL({ book: null });
                            navigate("/reservations");
                        }}>
                            My Reservations
                        </li>
                        <li
                            onClick={() => {
                                setSelectedBookId(null);
                                updateURL({ book: null });
                                navigate("/waitinglist");
                            }}
                            className={hasNotification ? "shiny" : ""}
                        >
                            My Waiting List
                            {hasNotification && <span className="notification-dot"></span>}
                        </li>
                    </ul>
                </div>
                <div className="category-box">
                    <h3>Categories</h3>
                    <ul>
                        <li onClick={() => {
                            setSelectedBookId(null);
                            handleGenreClick("Fantasy");
                        }}>Fantasy</li>
                        <li onClick={() => {
                            setSelectedBookId(null);
                            handleGenreClick("Science Fiction");
                        }}>Science Fiction</li>
                        <li onClick={() => {
                            setSelectedBookId(null);
                            handleGenreClick("Mystery");
                        }}>Mystery</li>
                        <li onClick={() => {
                            setSelectedBookId(null);
                            handleGenreClick("Romance");
                        }}>Romance</li>
                        <li onClick={() => {
                            setSelectedBookId(null);
                            handleGenreClick("Historical Fiction");
                        }}>Historical Fiction</li>
                        <li onClick={() => {
                            setSelectedBookId(null);
                            handleGenreClick("Horror");
                        }}>Horror</li>
                        <li onClick={() => {
                            setSelectedBookId(null);
                            handleGenreClick("Thriller");
                        }}>Thriller</li>
                        <li onClick={() => {
                            setSelectedBookId(null);
                            handleGenreClick("Young Adult");
                        }}>Young Adult</li>
                        <li onClick={() => {
                            setSelectedBookId(null);
                            handleGenreClick("Non-Fiction");
                        }}>Non-Fiction</li>
                        <li onClick={() => {
                            setSelectedBookId(null);
                            handleGenreClick("Poetry Classic");
                        }}>Poetry/Classic</li>
                    </ul>
                </div>
            </nav>
            <div className="main-content">
                <Header
                    user={user}
                    onSignOut={onSignOut}
                    isAuthenticated={isAuthenticated}
                    searchTerm={searchTerm}
                    setSearchTerm={setSearchTerm}
                    handleSearch={handleSearch}
                />
                <div className="content-wrapper">
                    <div className="central-content">
                        {selectedBookId ? (
                            <Book
                                isAuthenticated={isAuthenticated}
                                setIsAuthenticated={setIsAuthenticated}
                                id={selectedBookId}
                                onBack={handleBackFromBook} // Use the new handler
                            />
                        ) : (
                            <>
                                <div className="welcome">
                                    <h1>Welcome to our Digital Library</h1>
                                    <p>Explore our collection of {totalStats.totalBooks} books</p>
                                </div>
                                <div className="book-listing">
                                    <div className="book-listing-header">
                                        <h2>{currentTitle}</h2>
                                        <div className="book-count">Showing {currentBooks.length} of {totalCount} books</div>
                                    </div>
                                    {loading && <div className="loading">Loading books...</div>}
                                    {error && (
                                        <div className="error">
                                            {error}
                                            <button onClick={handleAllBooks} className="retry-btn">
                                                Retry
                                            </button>
                                        </div>
                                    )}
                                    {!loading && !error && (
                                        <div className="books-grid">
                                            {currentBooks.map((book) => (
                                                <div
                                                    key={book.bookId || book.title + book.author}
                                                    className="book-card"
                                                    onClick={() => book.bookId ? handleBookClick(book.bookId) : setError('Cannot navigate: Book ID is missing')}
                                                    style={{ cursor: book.bookId ? 'pointer' : 'not-allowed' }}
                                                >
                                                    <div className="book-cover">
                                                        {book.path ? (
                                                            <img
                                                                src={`/api/images/books/${encodeURIComponent(book.path)}`}
                                                                alt={book.title}
                                                                onError={(e) => {
                                                                    console.error('Image failed to load:', {
                                                                        bookTitle: book.title,
                                                                        databasePath: book.path,
                                                                        imageSrc: `/api/images/books/${encodeURIComponent(book.path)}`,
                                                                        timestamp: new Date().toISOString(),
                                                                    });
                                                                    e.target.src = '/placeholder-book-cover.jpg';
                                                                }}
                                                            />
                                                        ) : (
                                                            <div className="book-cover-placeholder">📖</div>
                                                        )}
                                                    </div>
                                                    <div className="book-info">
                                                        <h3>{book.title || 'Untitled'}</h3>
                                                        <p className="books-author">by {book.author || 'Unknown'}</p>
                                                        <p className="books-genre">{book.genre || 'Unknown'}</p>
                                                        {book.avgRating != null && (
                                                            <div className="book-rating">
                                                                {book.avgRating.toFixed(1)} ★
                                                            </div>
                                                        )}
                                                        <div className="book-status">
                                                            <span className={`availability ${book.available ? 'available' : 'unavailable'}`}>
                                                                {book.available ? 'Available' : 'Unavailable'}
                                                            </span>
                                                            {book.quantity !== undefined && (
                                                                <span className="quantity">Qty: {book.quantity}</span>
                                                            )}
                                                            {!book.bookId && (
                                                                <span className="error-text">Missing Book ID</span>
                                                            )}
                                                        </div>
                                                    </div>
                                                </div>
                                            ))}
                                        </div>
                                    )}
                                    {!loading && !error && currentBooks.length === 0 && (
                                        <div className="no-books">
                                            No books found. <button onClick={handleAllBooks}>Load all books</button>
                                        </div>
                                    )}
                                    {!loading && !error && totalCount > 0 && (
                                        <div className="pagination">
                                            <button onClick={prevPage} disabled={currentPage === 1}>
                                                Previous
                                            </button>
                                            {getPaginationButtons().map((page, index) => (
                                                <React.Fragment key={index}>
                                                    {typeof page === "number" ? (
                                                        <button
                                                            onClick={() => paginate(page)}
                                                            className={`pagination-number ${currentPage === page ? 'active' : ''}`}
                                                        >
                                                            {page}
                                                        </button>
                                                    ) : (
                                                        <span className="pagination-ellipsis">...</span>
                                                    )}
                                                </React.Fragment>
                                            ))}
                                            <button onClick={nextPage} disabled={currentPage === totalPages}>
                                                Next
                                            </button>
                                        </div>
                                    )}
                                </div>
                            </>
                        )}
                    </div>
                    <div className="right-panel">
                        <div className="stats-box">
                            <h3>Library Statistics</h3>
                            <p>Total books: {totalStats.totalBooks}</p>
                            <p>Available books: {totalStats.totalAvailable}</p>
                            <p>Categories: {totalStats.totalCategories}</p>
                        </div>
                        <div className="recommend-box">
                            <h3>Quick Actions</h3>
                            <ul>
                                <li onClick={handleTop10Click}>Top 10 Rated Books</li>
                            </ul>
                        </div>
                    </div>
                </div>
            </div>
            {/* AI Assistant Chat Panel */}
            {/* keyed by the user: signing in or out starts a new, empty chat with a new chat id */}
            <ChatPanel key={user?.id ?? 'guest'} />
        </div>
    );
};

export default Dashboard;
