import React, { useState, useRef, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { bookService } from "../services/bookService";
import "./Dashboard.css";

const Dashboard = ({ user, setIsAuthenticated, isAuthenticated }) => {
    const [isPanelOpen, setIsPanelOpen] = useState(false);
    const [books, setBooks] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [searchTerm, setSearchTerm] = useState("");
    const panelRef = useRef(null);
    const navigate = useNavigate();

    // Fetch books on component mount
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

            console.log('API Response:', response); // Debug log

            // Handle Spring Boot response structure
            if (response && response.data) {
                // For paginated responses (search and getAll)
                if (response.data.records) {
                    setBooks(response.data.records);
                }
                // For non-paginated responses (genre)
                else if (Array.isArray(response.data)) {
                    setBooks(response.data);
                }
                // Direct array response
                else if (Array.isArray(response)) {
                    setBooks(response);
                }
                else {
                    setBooks([]);
                }
            } else {
                setError("Invalid response format");
            }
        } catch (err) {
            setError(err.message || "Failed to load books");
            console.error("Error fetching books:", err);
        } finally {
            setLoading(false);
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

            console.log('Genre API Response:', response); // Debug log

            if (response && response.data) {
                if (Array.isArray(response.data)) {
                    setBooks(response.data);
                } else if (response.data.records) {
                    setBooks(response.data.records);
                } else {
                    setBooks([]);
                }
            } else {
                setError("Invalid response format for genre");
            }
        } catch (err) {
            setError(`Failed to load ${genre} books: ${err.message}`);
        } finally {
            setLoading(false);
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

    // Close panel when clicking outside
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

    return (
        <div className="dashboard">
            {/* Sidebar */}
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

            {/* Main Content */}
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
                    {/* Central content */}
                    <div className="central-content">
                        <div className="welcome">
                            <h1>Welcome to our digital library</h1>
                            <p>Explore our collection of {books.length} books</p>
                        </div>

                        <div className="book-listing">
                            <div className="book-listing-header">
                                <h2>Book Listing</h2>
                                <div className="book-count">Showing {books.length} books</div>
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
                                    {books.map((book) => (
                                        <div key={book.bookId} className="book-card">
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
                                                                timestamp: new Date().toISOString()
                                                            });
                                                            e.target.style.display = 'none';
                                                            e.target.nextSibling.style.display = 'block';
                                                        }}
                                                        onLoad={() => console.log('Image loaded successfully:', `http://localhost:8432/api/images/books/${encodeURIComponent(book.path)}`)}
                                                    />
                                                ) : null}
                                                {/*<div*/}
                                                {/*    className="book-cover-placeholder"*/}
                                                {/*    style={{ display: book.path ? 'none' : 'block' }}*/}
                                                {/*>*/}
                                                {/*    📖*/}
                                                {/*</div>*/}
                                            </div>
                                            <div className="book-info">
                                                <h3>{book.title}</h3>
                                                <p className="book-author">by {book.author}</p>
                                                <p className="book-genre">{book.genre}</p>
                                                <div className="book-status">
                                                    <span className={`availability ${book.available ? 'available' : 'unavailable'}`}>
                                                        {book.available ? 'Available' : 'Unavailable'}
                                                    </span>
                                                    {book.quantity !== undefined && (
                                                        <span className="quantity">Qty: {book.quantity}</span>
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
                        </div>
                    </div>

                    {/* Right panel */}
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