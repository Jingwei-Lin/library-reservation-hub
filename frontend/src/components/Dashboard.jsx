import React, { useState, useRef, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { bookService } from "../services/bookService";
import "./Dashboard.css";

const Dashboard = ({ user, setIsAuthenticated, isAuthenticated, theme, toggleTheme }) => {
    const [isPanelOpen, setIsPanelOpen] = useState(false);
    const [books, setBooks] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [searchTerm, setSearchTerm] = useState("");
    const [currentPage, setCurrentPage] = useState(1);
    // const [isDarkMode, setIsDarkMode] = useState(false); // New state for dark mode
    const booksPerPage = 12;
    const panelRef = useRef(null);
    const navigate = useNavigate();


    useEffect(() => {
        fetchBooks();
    }, []);

    const fetchBooks = async (keyword = "") => {
        try {
            setLoading(true);
            setError("");
            const response = keyword
                ? await bookService.searchBooks(keyword)
                : await bookService.getAllBooks();

            console.log('API Response (fetchBooks):', JSON.stringify(response, null, 2));

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
            setError(err.message || "Failed to load books");
            console.error("Error fetching books:", err);
            setBooks([]);
        } finally {
            setLoading(false);
            setCurrentPage(1); // Reset to first page on new fetch
        }
    };

    const handleSearch = (e) => {
        e.preventDefault();
        fetchBooks(searchTerm);
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
            setError(`Failed to load ${genre} books: ${err.message}`);
            console.error("Error fetching books by genre:", err);
            setBooks([]);
        } finally {
            setLoading(false);
            setCurrentPage(1); // Reset to first page on new fetch
        }
    };

    const handleLogout = () => {
        localStorage.removeItem("user");
        localStorage.removeItem("token");
        setIsAuthenticated(false);
        setIsPanelOpen(false);
    };

    const handleLoginRedirect = () => {
        navigate("/login");
    };

    const handleProtectedClick = () => {
        if (!isAuthenticated) {
            navigate("/login");
        }
    };

    const handleBookClick = (bookId) => {
        const id = Number(bookId);
        if (!bookId || isNaN(id) || id <= 0) {
            console.error('Invalid bookId:', bookId);
            setError('Cannot navigate to book with invalid ID');
            return;
        }
        navigate(`/book/${id}`);
    };

    useEffect(() => {
        const handleClickOutside = (event) => {
            if (panelRef.current && !panelRef.current.contains(event.target)) {
                setIsPanelOpen(false);
            }
        };
        document.addEventListener("mousedown", handleClickOutside);
        return () => {
            document.removeEventListener("mousedown", handleClickOutside);
        };
    }, []);

    // Pagination logic
    const indexOfLastBook = currentPage * booksPerPage;
    const indexOfFirstBook = indexOfLastBook - booksPerPage;
    const currentBooks = books.slice(indexOfFirstBook, indexOfLastBook);
    const totalPages = Math.ceil(books.length / booksPerPage);

    const paginate = (pageNumber) => setCurrentPage(pageNumber);
    const nextPage = () => setCurrentPage(prev => Math.min(prev + 1, totalPages));
    const prevPage = () => setCurrentPage(prev => Math.max(prev - 1, 1));

    // Dynamic pagination buttons
    const getPaginationButtons = () => {
        const buttons = [];

        // Always show first page
        buttons.push(1);

        if (totalPages <= 5) {
            // Show all pages if small number
            for (let i = 2; i <= totalPages; i++) {
                buttons.push(i);
            }
        } else {
            if (currentPage <= 3) {
                // Near the beginning: 1,2,3,4,...,last
                buttons.push(2,3,4);
                buttons.push("...");
                buttons.push(totalPages);
            } else if (currentPage >= totalPages - 2) {
                // Near the end: 1,...,last-3,last-2,last-1,last
                buttons.push("...");
                for (let i = totalPages - 3; i <= totalPages; i++) {
                    buttons.push(i);
                }
            } else {
                // Middle pages: 1,...,current-1,current,current+1,...,last
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
        // <div className={`dashboard ${isDarkMode ? "dark-mode" : ""}`}>
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
                        <li onClick={handleProtectedClick}>My Borrowings</li>
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
                <header className="header">
                    <form className="search-bar" onSubmit={handleSearch}>
                        <input
                            type="text"
                            placeholder="Search books, authors, categories..."
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                        />
                        <button type="submit">Search</button>
                        <button type="button" onClick={() => { setSearchTerm(""); fetchBooks(); }} className="clear-btn">
                            Clear
                        </button>
                    </form>
                    <div className="user-wrapper" ref={panelRef}>
                        {isAuthenticated ? (
                            <>
                                <div
                                    className="dark-mode-toggle"
                                    onClick={toggleTheme}
                                >
                                    {theme === "dark" ? "🌞" : "🌙"}
                                </div>

                                <div
                                    className="user-icon"
                                    onClick={() => setIsPanelOpen(!isPanelOpen)}
                                >
                                    👤
                                </div>
                                <div className={`user-panel ${isPanelOpen ? "open" : ""}`}>
                                    <h3>User Panel</h3>
                                    <p>{user?.firstName} {user?.lastName}</p>
                                    <p>Email: {user?.userAccount}</p>
                                    <button onClick={handleLogout}>Logout</button>
                                </div>
                            </>
                        ) : (
                            <button className="login-button" onClick={handleLoginRedirect}>
                                Login
                            </button>
                        )}
                    </div>
                </header>
                <div className="content-wrapper">
                    <div className="central-content">
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
                                                <p className="book-author">by {book.author || 'Unknown'}</p>
                                                <p className="book-genre">{book.genre || 'Unknown'}</p>
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
                                                    style={{
                                                        fontWeight: currentPage === page ? 'bold' : 'normal',
                                                        backgroundColor: currentPage === page ? '#e0e0e0' : 'white',
                                                    }}
                                                >
                                                    {page}
                                                </button>
                                            ) : (
                                                <span style={{ padding: '8px 12px' }}>...</span>
                                            )}
                                        </React.Fragment>
                                    ))}
                                    <button onClick={nextPage} disabled={currentPage === totalPages}>
                                        Next
                                    </button>
                                </div>
                            )}
                        </div>
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
        </div>
    );
};

export default Dashboard;