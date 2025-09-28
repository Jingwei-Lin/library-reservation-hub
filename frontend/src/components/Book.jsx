import React, { useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { bookService } from "../services/bookService";
import { reservationService } from "../services/reservationService";
import "./Book.css";

const Book = ({ isAuthenticated }) => {
    const { id } = useParams();
    const navigate = useNavigate();
    const [book, setBook] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [reserveLoading, setReserveLoading] = useState(false);
    const [reserveMessage, setReserveMessage] = useState("");

    useEffect(() => {
        const fetchBook = async () => {
            try {
                setLoading(true);
                setError("");
                const response = await bookService.getBookById(id);

                if (response && response.code === 0 && response.data) {
                    setBook(response.data);
                } else {
                    setError(response?.msg || "Invalid response format");
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

    const handleReserve = async () => {
        if (!isAuthenticated) {
            navigate("/login");
            return;
        }

        setReserveLoading(true);
        setReserveMessage("");
        setError("");

        try {
            const response = await reservationService.createReservation(id);
            if (response && response.code === 0) {
                setReserveMessage("Reservation successful! Check your reservations.");
                const updatedBookResponse = await bookService.getBookById(id);
                if (updatedBookResponse && updatedBookResponse.code === 0 && updatedBookResponse.data) {
                    setBook(updatedBookResponse.data);
                }
            } else {
                throw new Error(response?.msg || "Invalid response");
            }
        } catch (err) {
            setError(err.message || "Failed to reserve book");
            if (err.message.includes("not logged in") || err.message.includes("Unauthorized")) {
                navigate("/login");
            }
        } finally {
            setReserveLoading(false);
        }
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
                                    e.target.src = '/placeholder-book-cover.jpg';
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
                            disabled={!book.available || reserveLoading}
                        >
                            {reserveLoading ? 'Reserving...' : (book.available ? 'Reserve Now' : 'Join Waiting List')}
                        </button>
                        {reserveMessage && <p className="success-message">{reserveMessage}</p>}
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