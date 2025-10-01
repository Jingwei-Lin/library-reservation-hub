// Book.jsx
import React, { useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { bookService } from "../services/bookService";
import { reservationService } from "../services/reservationService";
import { reviewService } from "../services/reviewService";
import "./Book.css";

const Book = ({ isAuthenticated }) => {
    const { id } = useParams();
    const navigate = useNavigate();
    const [book, setBook] = useState(null);
    const [reviews, setReviews] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [reserveLoading, setReserveLoading] = useState(false);
    const [reserveMessage, setReserveMessage] = useState("");

    useEffect(() => {
        const fetchBookAndReviews = async () => {
            try {
                setLoading(true);
                setError("");

                // Fetch book details
                const bookResponse = await bookService.getBookById(id);
                if (bookResponse && bookResponse.code === 0 && bookResponse.data) {
                    setBook(bookResponse.data);
                } else {
                    setError(bookResponse?.msg || "Invalid response format");
                    return;
                }

                // Fetch reviews
                const reviewResponse = await reviewService.getReviewsByBookId(id);
                if (reviewResponse && reviewResponse.code === 0 && reviewResponse.data) {
                    setReviews(Array.isArray(reviewResponse.data) ? reviewResponse.data : []);
                } else {
                    console.warn(reviewResponse?.msg || "No reviews found");
                    setReviews([]);
                }
            } catch (err) {
                if (err.message.includes("Session expired or user banned")) {
                    logoutUser(); // Assuming logoutUser is imported if needed
                    navigate("/login");
                    return;
                }
                setError(err.message || "Failed to load book details or reviews");
                console.error("Error fetching book or reviews:", err);
                setReviews([]); // Set empty reviews on error
            } finally {
                setLoading(false);
            }
        };

        fetchBookAndReviews();
    }, [id, navigate]);

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
                // Refresh book details after successful reservation
                const updatedBookResponse = await bookService.getBookById(id);
                if (updatedBookResponse && updatedBookResponse.code === 0 && updatedBookResponse.data) {
                    setBook(updatedBookResponse.data);
                }
            } else {
                throw new Error(response?.msg || "Invalid response");
            }
        } catch (err) {
            setError(err.message || "Failed to reserve book");
            if (err.message.includes("Session expired or user banned") || err.message.includes("not logged in") || err.message.includes("Unauthorized")) {
                navigate("/login");
            }
        } finally {
            setReserveLoading(false);
        }
    };

    if (loading) {
        return (
            <div className="book-detail-container">
                <div className="loading">Loading book details...</div>
            </div>
        );
    }

    if (error && !book) {
        return (
            <div className="book-detail-container">
                <div className="error">
                    {error}
                    <button onClick={() => window.location.reload()} className="retry-btn">
                        Retry
                    </button>
                </div>
            </div>
        );
    }

    return (
        <div className="book-detail-container">
            <button className="back-button" onClick={handleBack}>
                ← Back to Dashboard
            </button>

            {error && (
                <div className="error">
                    {error}
                    <button onClick={() => window.location.reload()} className="retry-btn">
                        Retry
                    </button>
                </div>
            )}

            {book && (
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

            {/* Reviews Section */}
            {book && (
                <div className="reviews-section">
                    <h2>Reviews ({reviews.length})</h2>
                    {reviews.length > 0 ? (
                        <div className="reviews-list">
                            {reviews.map((review) => (
                                <div key={review.reviewId} className="review-card">
                                    <div className="review-header">
                                        <span className="review-user">User ID: {review.userId}</span>
                                        <span className="review-rating">⭐ {review.rating}/5</span>
                                    </div>
                                    {review.comment && <p className="review-comment">{review.comment}</p>}
                                    <p className="review-date">
                                        Posted on: {new Date(review.reviewDate).toLocaleDateString()}
                                    </p>
                                </div>
                            ))}
                        </div>
                    ) : (
                        <div className="no-reviews">
                            <p>No reviews yet for this book. Be the first to review!</p>
                        </div>
                    )}
                </div>
            )}

            {!book && (
                <div className="no-book">
                    Book not found.
                    <button onClick={handleBack}>Back to Dashboard</button>
                </div>
            )}
        </div>
    );
};

export default Book;