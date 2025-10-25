// Dashboard.jsx
import React, { useState, useEffect } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import { bookService } from "../services/bookService";
import { waitingListService } from "../services/waitingListService";
import { logoutUser } from "../services/authApi";
import Header from "./Header";

import ChatPanel from "./ChatPanel";

import Book from './Book';
import { getTheme } from "../themeUtils";

import "./Dashboard.css";

const Dashboard = ({ user, setIsAuthenticated, isAuthenticated }) => {
    const [books, setBooks] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [searchTerm, setSearchTerm] = useState("");
    const [currentPage, setCurrentPage] = useState(1);
    const [selectedBookId, setSelectedBookId] = useState(null);
    const [initialLoadComplete, setInitialLoadComplete] = useState(false);
    const [theme, setTheme] = useState(getTheme());
    const [hasNotification, setHasNotification] = useState(false);
    const booksPerPage = 12;
    const navigate = useNavigate();
    const location = useLocation();

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

    // Initial data fetch
    useEffect(() => {
        const initializeDashboard = async () => {
            console.log(sessionStorage);
            await fetchBooks();
            await fetchWaitingList();
            setInitialLoadComplete(true);
        };

        initializeDashboard();
    }, []);

    // Sync URL with selected book state
    useEffect(() => {
        if (!initialLoadComplete) return; // Wait for initial data load
        const params = new URLSearchParams(location.search);
        const bookIdFromUrl = params.get('book');
        if (bookIdFromUrl) {
            const id = Number(bookIdFromUrl);
            if (!isNaN(id) && id > 0) {
                setSelectedBookId(id);
            } else {
                navigate('/dashboard', { replace: true });
            }
        } else {
            setSelectedBookId(null);
        }
    }, [location.search], initialLoadComplete, navigate);

    useEffect(() => {
        if (initialLoadComplete) {
            fetchWaitingList(); // Refetch when navigating back to dashboard
        }
    }, [location.pathname, initialLoadComplete]);

    const fetchBooks = async (keyword = "") => {
        try {
            setLoading(true);
            setError("");
            const response = keyword
                ? await bookService.searchBooks(keyword)
                : await bookService.getAllBooks();

            if (response && response.code === 0 && response.data) {
                const bookList = response.data.records
                    ? response.data.records
                    : Array.isArray(response.data)
                        ? response.data
                        : [];
                const validBooks = bookList.filter(book => {
                    const bookId = Number(book.bookId);
                    const isValid = book.bookId != null && !isNaN(bookId) && bookId > 0;
                    if (!isValid) {
                        console.warn('Invalid book detected:', JSON.stringify(book, null, 2));
                    }
                    return true;
                });
                setBooks(validBooks);
                if (bookList.length > 0 && validBooks.every(book => !book.bookId)) {
                    setError("All books have missing or invalid IDs. Please contact support.");
                }
            } else {
                setError("Invalid response format");
                setBooks([]);
            }
        } catch (err) {
            if (err.message.includes("Session expired or user banned")) {
                logoutUser();
                setIsAuthenticated(false);
                navigate("/login");
            } else {
                setError(err.message || "Failed to load books");
                console.error("Error fetching books:", err);
                setBooks([]);
            }
        } finally {
            setLoading(false);
            setCurrentPage(1);
        }
    };

    const handleSearch = (e) => {
        e.preventDefault();
        fetchBooks(searchTerm);
        setSelectedBookId(null);
        navigate('/dashboard', { replace: true });
    };

    const handleGenreClick = async (genre) => {
        try {
            setLoading(true);
            setError("");
            const response = await bookService.getBooksByGenre(genre);

            console.log('API Response (handleGenreClick):', JSON.stringify(response, null, 2));

            if (response && response.code === 0 && response.data) {
                const bookList = Array.isArray(response.data) ? response.data : response.data.records || [];
                const validBooks = bookList.filter(book => {
                    const bookId = Number(book.bookId);
                    const isValid = book.bookId != null && !isNaN(bookId) && bookId > 0;
                    if (!isValid) {
                        console.warn('Invalid book detected:', JSON.stringify(book, null, 2));
                    }
                    return true;
                });
                setBooks(validBooks);
                if (bookList.length > 0 && validBooks.every(book => !book.bookId)) {
                    setError(`All ${genre} books have missing or invalid IDs. Please contact support.`);
                }
            } else {
                setError("Invalid response format for genre");
                setBooks([]);
            }
        } catch (err) {
            if (err.message.includes("Session expired or user banned")) {
                logoutUser();
                setIsAuthenticated(false);
                navigate("/login");
            } else {
                setError(`Failed to load ${genre} books: ${err.message}`);
                console.error("Error fetching books by genre:", err);
                setBooks([]);
            }
        } finally {
            setLoading(false);
            setCurrentPage(1);
            setSelectedBookId(null);
            navigate('/dashboard', { replace: true });
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
        navigate(`/dashboard?book=${id}`, { replace: true });
    };

    const handleBackFromBook = () => {
        setSelectedBookId(null);
        navigate('/dashboard', { replace: true });
    };

    // Fetch waiting list and check for notified status
    const fetchWaitingList = async () => {
        try {
            const response = await waitingListService.getMyWaitingList();
            if (response && response.code === 0 && response.data) {
                // Check if any waiting list item has status "notified"
                const hasNotified = response.data.some(item => item.status === "notified");
                setHasNotification(hasNotified);
            } else {
                setHasNotification(false); // No notifications if response is invalid
            }
        } catch (err) {
            console.error("fetchWaitingList error:", err);
            if (err.message.includes("Session expired or user banned")) {
                logoutUser();
                setIsAuthenticated(false);
                navigate("/login");
            } else {
                console.warn("Failed to check waiting list notifications");
                setHasNotification(false); // Safe default on error
            }
        }
    };

    // Pagination logic
    const indexOfLastBook = currentPage * booksPerPage;
    const indexOfFirstBook = indexOfLastBook - booksPerPage;
    const currentBooks = books.slice(indexOfFirstBook, indexOfLastBook);
    const totalPages = Math.ceil(books.length / booksPerPage);

    const paginate = (pageNumber) => setCurrentPage(pageNumber);
    const nextPage = () => setCurrentPage(prev => Math.min(prev + 1, totalPages));
    const prevPage = () => setCurrentPage(prev => Math.max(prev - 1, 1));

    const getPaginationButtons = () => {
        const buttons = [];
        buttons.push(1);
        if (totalPages <= 5) {
            for (let i = 2; i <= totalPages; i++) {
                buttons.push(i);
            }
        } else {
            if (currentPage <= 3) {
                buttons.push(2, 3, 4);
                buttons.push("...");
                buttons.push(totalPages);
            } else if (currentPage >= totalPages - 2) {
                buttons.push("...");
                for (let i = totalPages - 3; i <= totalPages; i++) {
                    buttons.push(i);
                }
            } else {
                buttons.push("...");
                buttons.push(currentPage - 1);
                buttons.push(currentPage);
                buttons.push(currentPage + 1);
                buttons.push("...");
                buttons.push(totalPages);
            }
        }
        return buttons;
    };

    return (
        <div className={`dashboard ${theme === "dark" ? "dark-mode" : ""}`}>
            <nav className="sidebar">
                <div className="logo">
                    <span className="logo-icon">📚</span>
                    <span className="logo-text">LibraSys</span>
                </div>
                <div className="nav-box">
                    <h3>Navigation</h3>
                    <ul>
                        <li onClick={() => fetchBooks()}>All Books</li>
                        <li onClick={() => navigate("/reservations")}>My Reservations</li>
                        <li
                            onClick={() => navigate("/waitinglist")}
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
                        <li onClick={() => handleGenreClick("Fantasy")}>Fantasy</li>
                        <li onClick={() => handleGenreClick("Science Fiction")}>Science Fiction</li>
                        <li onClick={() => handleGenreClick("Mystery")}>Mystery</li>
                        <li onClick={() => handleGenreClick("Romance")}>Romance</li>
                        <li onClick={() => handleGenreClick("Historical Fiction")}>Historical Fiction</li>
                        <li onClick={() => handleGenreClick("Horror")}>Horror</li>
                        <li onClick={() => handleGenreClick("Thriller")}>Thriller</li>
                        <li onClick={() => handleGenreClick("Young Adult")}>Young Adult</li>
                        <li onClick={() => handleGenreClick("Non-Fiction")}>Non-Fiction</li>
                        <li onClick={() => handleGenreClick("Poetry Classic")}>Poetry/Classic</li>
                    </ul>
                </div>
            </nav>
            <div className="main-content">
                <Header
                    user={user}
                    setIsAuthenticated={setIsAuthenticated}
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
                                    <h1>Welcome to our digital library</h1>
                                    <p>Explore our collection of {books.length} books</p>
                                </div>
                                <div className="book-listing">
                                    <div className="book-listing-header">
                                        <h2>Book Listing</h2>
                                        <div className="book-count">Showing {currentBooks.length} of {books.length} books</div>
                                    </div>
                                    {loading && <div className="loading">Loading books...</div>}
                                    {error && (
                                        <div className="error">
                                            {error}
                                            <button onClick={() => fetchBooks()} className="retry-btn">
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
                                                                src={`http://localhost:8432/api/images/books/${encodeURIComponent(book.path)}`}
                                                                alt={book.title}
                                                                onError={(e) => {
                                                                    console.error('Image failed to load:', {
                                                                        bookTitle: book.title,
                                                                        databasePath: book.path,
                                                                        imageSrc: `http://localhost:8432/api/images/books/${encodeURIComponent(book.path)}`,
                                                                        timestamp: new Date().toISOString(),
                                                                    });
                                                                    e.target.src = '/placeholder-book-cover.jpg';
                                                                }}
                                                                onLoad={() => console.log('Image loaded successfully:', `http://localhost:8432/api/images/books/${encodeURIComponent(book.path)}`)}
                                                            />
                                                        ) : (
                                                            <div className="book-cover-placeholder">📖</div>
                                                        )}
                                                    </div>
                                                    <div className="book-info">
                                                        <h3>{book.title || 'Untitled'}</h3>
                                                        <p className="books-author">by {book.author || 'Unknown'}</p>
                                                        <p className="books-genre">{book.genre || 'Unknown'}</p>
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
                                    {!loading && !error && books.length === 0 && (
                                        <div className="no-books">
                                            No books found. <button onClick={() => fetchBooks()}>Load all books</button>
                                        </div>
                                    )}
                                    {!loading && !error && books.length > 0 && (
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
                            <p>Total books: {books.length}</p>
                            <p>Available books: {books.filter(book => book.available).length}</p>
                            <p>Categories: 10</p>
                        </div>
                        <div className="recommend-box">
                            <h3>Quick Actions</h3>
                            <ul>
                                <li onClick={() => fetchBooks()}>View All Books</li>
                                <li onClick={() => handleGenreClick("Fantasy")}>Fantasy Books</li>
                                <li onClick={() => handleGenreClick("Science Fiction")}>Sci-Fi Books</li>
                            </ul>
                        </div>
                    </div>
                </div>
            </div>
            {/* AI Assistant Chat Panel */}
            <ChatPanel />
        </div>
    );
};

export default Dashboard;
