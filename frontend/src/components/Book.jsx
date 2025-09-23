import React, { useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { bookService } from "../services/bookService"; // Adjust path if needed
import "./Book.css";

const Book = ({ isAuthenticated }) => {
    const { id } = useParams();
    const navigate = useNavigate();
    const [book, setBook] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        const fetchBook = async () => {
            try {
                setLoading(true);
                setError("");
                const response = await bookService.getBookById(id);

                // Handle Spring Boot BaseResponse structure
                if (response && response.code === 0 && response.data) {
                    setBook(response.data); // Extract BookVO from response.data
                } else {
                    setError("Invalid response format");
                }
            } catch (err) {
                setError(err.message || "Failed to load book details");
                console.error("Error fetching book:", err);
            } finally {
                setLoading(false);
            }
        };

        fetchBook();
    }, [id]);

    const handleBack = () => {
        navigate("/dashboard");
    };

    const handleReserve = () => {
        if (!isAuthenticated) {
            navigate("/login");
            return;
        }
        // Placeholder for reservation logic
        alert("Reservation functionality to be implemented.");
    };

    return (
        <div className="book-detail-container">
            <button className="back-button" onClick={handleBack}>
                ← Back to Dashboard
            </button>

            {loading && <div className="loading">Loading book details...</div>}
            {error && (
                <div className="error">
                    {error}
                    <button onClick={() => window.location.reload()} className="retry-btn">
                        Retry
                    </button>
                </div>
            )}

            {!loading && !error && book && (
                <div className="book-detail-card">
                    <div className="book-detail-cover">
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
                                    e.target.src = '/placeholder-book-cover.jpg'; // Fallback image
                                }}
                            />
                        ) : (
                            <div className="book-cover-placeholder">📖</div>
                        )}
                    </div>
                    <div className="book-detail-info">
                        <h1>{book.title}</h1>
                        <p className="book-author">by {book.author}</p>
                        <p className="book-genre">Genre: {book.genre}</p>
                        <div className="book-status">
                            <span className={`availability ${book.available ? 'available' : 'unavailable'}`}>
                                {book.available ? 'Available' : 'Unavailable'}
                            </span>
                            {book.quantity !== undefined && (
                                <span className="quantity">Quantity: {book.quantity}</span>
                            )}
                        </div>
                        <button
                            className="reserve-button"
                            onClick={handleReserve}
                            disabled={!book.available}
                        >
                            {book.available ? 'Reserve Now' : 'Join Waiting List'}
                        </button>
                    </div>
                </div>
            )}

            {!loading && !error && !book && (
                <div className="no-book">
                    Book not found.
                    <button onClick={handleBack}>Back to Dashboard</button>
                </div>
            )}
        </div>
    );
};

export default Book;