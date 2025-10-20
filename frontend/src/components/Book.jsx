import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { bookService } from "../services/bookService";
import { reservationService } from "../services/reservationService";
import { reviewService } from "../services/reviewService";
import { waitingListService } from "../services/waitingListService";
import { logoutUser } from "../services/authApi";
import Portal from './Portal';
import "./Book.css";

const Book = ({ isAuthenticated, setIsAuthenticated, id, onBack }) => {
    // const { id } = useParams();
    const navigate = useNavigate();
    const [book, setBook] = useState(null);
    const [reviews, setReviews] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [newReview, setNewReview] = useState({ rating: 0, comment: "" });
    const [isOnWaitingList, setIsOnWaitingList] = useState(false);
    const [isNotified, setIsNotified] = useState(false);
    const [waitingStatus, setWaitingStatus] = useState(null);
    const [hasActiveReservation, setHasActiveReservation] = useState(false);

    //State for reservation popup
    const [showReservationPopup, setShowReservationPopup] = useState(false);
    const [reservationForm, setReservationForm] = useState({
        pickupTime: "",
        dueTime: ""
    });
    const [reservationLoading, setReservationLoading] = useState(false);
    const [reservationError, setReservationError] = useState("");
    const [reservationSuccess, setReservationSuccess] = useState("");

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
                    setError(bookResponse.message || "Invalid response format");
                    return;
                }

                // Fetch all reviews (accessible to all users)
                const reviewResponse = await reviewService.getReviewsByBookId(id);
                if (reviewResponse && reviewResponse.code === 0 && reviewResponse.data) {
                    setReviews(Array.isArray(reviewResponse.data) ? reviewResponse.data : []);
                } else {
                    console.warn(reviewResponse.message || "No reviews found");
                    setReviews([]);
                }

                // Fetch waiting list and reservation status if authenticated
                if (isAuthenticated) {
                    // Check reservation status
                    const reservationResponse = await reservationService.hasActiveReservation(id);
                    if (reservationResponse && reservationResponse.code === 0) {
                        setHasActiveReservation(reservationResponse.data.hasActive);
                    }

                    // Fetch waiting list status
                    await waitingListService.getEligibility();
                    const waitResponse = await waitingListService.getMyWaitingList();
                    if (waitResponse && waitResponse.code === 0 && waitResponse.data) {
                        const waitItem = waitResponse.data.find((item) => item.bookId === parseInt(id));
                        if (waitItem) {
                            setIsOnWaitingList(true);
                            setIsNotified(waitItem.status === "notified");
                            setWaitingStatus(waitItem.status);
                        }
                    }
                }
            } catch (err) {
                if (err.message.includes("Session expired or user banned") && isAuthenticated) {
                    localStorage.removeItem('user');
                    sessionStorage.removeItem('user');
                    logoutUser();
                    setIsAuthenticated(false);
                    navigate("/login");
                    return;
                }
                setError(err.message || "Failed to load book details or reviews");
                console.error("Error fetching book or reviews:", err);
                setReviews([]);
            } finally {
                setLoading(false);
            }
        };

        fetchBookAndReviews();
    }, [id, setIsAuthenticated, isAuthenticated]);

    useEffect(() => {
        if (showReservationPopup) {
            document.body.classList.add('popup-open');
        } else {
            document.body.classList.remove('popup-open');
        }

        return () => {
            document.body.classList.remove('popup-open');
        };
    }, [showReservationPopup]);

    const handleBack = () => {
        if (onBack) {
            onBack(); // Call onBack if provided
        } else {
            navigate('/dashboard'); // Fallback to dashboard
        }
    };

    const handleReserve = () => {
        if (!isAuthenticated) {
            navigate("/login");
            return;
        }
        setShowReservationPopup(true);
        // navigate(`/reservation/form/${id}?fromBook=${id}`);
    };

    const handleReservationSubmit = async (e) => {
        e.preventDefault();
        if (!isAuthenticated) {
            navigate("/login");
            return;
        }

        setReservationLoading(true);
        setReservationError("");
        setReservationSuccess("");

        try {
            const pickupDate = new Date(reservationForm.pickupTime);
            const dueDate = new Date(reservationForm.dueTime);
            const today = new Date();
            today.setHours(0, 0, 0, 0);

            if (pickupDate < today) {
                throw new Error("Pickup date cannot be in the past");
            }
            if (dueDate <= pickupDate) {
                throw new Error("Due date must be after pickup date");
            }

            // Calculate maximum allowed due date (7 days from pickup)
            const maxDueDate = new Date(pickupDate);
            maxDueDate.setDate(pickupDate.getDate() + 30);

            if (dueDate > maxDueDate) {
                throw new Error("Reservation period cannot exceed 30 days");
            }

            const response = await reservationService.createReservation(id, {
                pickupTime: reservationForm.pickupTime,
                dueTime: reservationForm.dueTime
            });

            if (response && response.code === 0) {
                setReservationSuccess("Reservation created successfully!");
                setHasActiveReservation(true);
                setTimeout(() => {
                    setShowReservationPopup(false);
                    setReservationForm({ pickupTime: "", dueTime: "" });
                }, 2000);
            } else if (response && response.code === 50001) {
                setReservationError(response.message);
            } else {
                setReservationError(response?.message || "Failed to create reservation");
            }
        } catch (err) {
            console.error("createReservation error:", err);
            if (err.message.includes("Session expired") || err.message.includes("Please log in")) {
                localStorage.removeItem('user');
                setIsAuthenticated(false);
                navigate("/login");
            } else {
                setReservationError(err.message || "Failed to create reservation");
            }
        } finally {
            setReservationLoading(false);
        }
    };

    const handleReservationFormChange = (e) => {
        setReservationForm({ ...reservationForm, [e.target.name]: e.target.value });
    };

    const handleCloseReservationPopup = () => {
        setShowReservationPopup(false);
        setReservationForm({ pickupTime: "", dueTime: "" });
        setReservationError("");
        setReservationSuccess("");
    };

    const handleJoinWaiting = async () => {
        if (!isAuthenticated) {
            navigate("/login");
            return;
        }
        try {
            const response = await waitingListService.addToWaitingList(parseInt(id));
            if (response && response.code === 0) {
                window.alert("Successfully added to waiting list!");
                setIsOnWaitingList(true);
                setIsNotified(false);
                setWaitingStatus("waiting");
            } else {
                window.alert(response.message || "Failed to add to waiting list");
            }
        } catch (err) {
            window.alert(err.message || "Failed to add to waiting list");
        }
    };

    const handleCancelWaiting = async () => {
        if (!isAuthenticated) {
            navigate("/login");
            return;
        }
        if (!window.confirm("Are you sure you want to cancel your waiting list entry?")) return;
        try {
            const response = await waitingListService.cancelWaiting(parseInt(id));
            if (response && response.code === 0) {
                window.alert("Waiting list entry cancelled successfully!");
                setIsOnWaitingList(false);
                setIsNotified(false);
                setWaitingStatus(null);
            } else {
                window.alert(response.message || "Failed to cancel waiting list entry");
            }
        } catch (err) {
            window.alert(err.message || "Failed to cancel waiting list entry");
        }
    };

    const handleAddReview = async (e) => {
        e.preventDefault();
        if (!isAuthenticated) {
            navigate("/login");
            return;
        }
        if (newReview.rating < 1 || newReview.rating > 5) {
            window.alert("Rating must be between 1 and 5");
            return;
        }
        if (!newReview.comment.trim()) {
            window.alert("Comment cannot be empty");
            return;
        }

        try {
            setError("");
            const reviewData = { rating: newReview.rating, comment: newReview.comment };
            await reviewService.addReview(parseInt(id), reviewData);
            window.alert("Review added successfully!");
            setNewReview({ rating: 0, comment: "" });
            window.location.reload();
        } catch (err) {
            window.alert(err.message || "Failed to add review");
            console.error("Error adding review:", err);
        }
    };

    const handleDeleteReview = async (reviewId) => {
        if (!isAuthenticated) {
            navigate("/login");
            return;
        }
        if (!window.confirm("Are you sure you want to delete this review?")) {
            return;
        }

        try {
            setError("");
            await reviewService.deleteReview(reviewId);
            window.alert("Review deleted successfully!");
            const reviewResponse = await reviewService.getReviewsByBookId(id);
            if (reviewResponse && reviewResponse.code === 0 && reviewResponse.data) {
                setReviews(Array.isArray(reviewResponse.data) ? reviewResponse.data : []);
            } else {
                console.warn(reviewResponse?.msg || "No reviews found after deletion");
                setReviews([]);
            }
        } catch (err) {
            window.alert(err.message || "Failed to delete review");
            console.error("Error deleting review:", err);
        }
    };

    const handleStarClick = (rating) => {
        setNewReview({ ...newReview, rating });
    };

    return (
        <div className="book-detail-container">
            {/* Reservation Popup */}
            {showReservationPopup && (
                <Portal>
                    <div className="popup-overlay">
                        <div className="reservation-popup">
                            <div className="popup-header">
                                <h3>Reserve "{book?.title}"</h3>
                                <button
                                    className="close-popup"
                                    onClick={handleCloseReservationPopup}
                                    aria-label="Close reservation popup"
                                >
                                    ×
                                </button>
                            </div>
                            <form onSubmit={handleReservationSubmit} className="reservation-form">
                                <div className="form-group">
                                    <label htmlFor="pickupTime">Pickup Date:</label>
                                    <input
                                        type="date"
                                        id="pickupTime"
                                        name="pickupTime"
                                        value={reservationForm.pickupTime}
                                        onChange={handleReservationFormChange}
                                        required
                                        min={new Date().toISOString().split("T")[0]}
                                        max={(() => {
                                            const maxDate = new Date();
                                            maxDate.setDate(maxDate.getDate() + 7);
                                            return maxDate.toISOString().split("T")[0];
                                        })()}
                                    />
                                    <small className="date-hint">Select a date within the next 7 days</small>
                                </div>
                                <div className="form-group">
                                    <label htmlFor="dueTime">Due Date:</label>
                                    <input
                                        type="date"
                                        id="dueTime"
                                        name="dueTime"
                                        value={reservationForm.dueTime}
                                        onChange={handleReservationFormChange}
                                        required
                                        min={reservationForm.pickupTime || new Date().toISOString().split("T")[0]}
                                        max={(() => {
                                            if (reservationForm.pickupTime) {
                                                const maxDueDate = new Date(reservationForm.pickupTime);
                                                maxDueDate.setDate(maxDueDate.getDate() + 30);
                                                return maxDueDate.toISOString().split("T")[0];
                                            }
                                            const maxDate = new Date();
                                            maxDate.setDate(maxDate.getDate() + 30);
                                            return maxDate.toISOString().split("T")[0];
                                        })()}
                                    />
                                    <small className="date-hint">
                                        {reservationForm.pickupTime
                                            ? `Due date must be within 30 days of pickup (max: ${(() => {
                                                const maxDueDate = new Date(reservationForm.pickupTime);
                                                maxDueDate.setDate(maxDueDate.getDate() + 30);
                                                return maxDueDate.toISOString().split("T")[0];
                                            })()})`
                                            : "Select pickup date first"
                                        }
                                    </small>
                                </div>
                                {reservationError && <p className="error">{reservationError}</p>}
                                {reservationSuccess && <p className="success">{reservationSuccess}</p>}
                                <div className="popup-actions">
                                    <button
                                        type="button"
                                        onClick={handleCloseReservationPopup}
                                        className="cancel-btn"
                                    >
                                        Cancel
                                    </button>
                                    <button
                                        type="submit"
                                        disabled={reservationLoading}
                                        className="confirm-btn"
                                    >
                                        {reservationLoading ? "Reserving..." : "Confirm Reservation"}
                                    </button>
                                </div>
                            </form>
                        </div>
                    </div>
                </Portal>
            )}
            <button className="book-back-button" onClick={handleBack} aria-label="Back to dashboard">
                ← Back to Dashboard
            </button>

            {loading && <div className="loading">Loading book details...</div>}
            {error && (
                <div className="error">
                    {error}
                    <button onClick={() => window.location.reload()} className="retry-btn" aria-label="Retry loading">
                        Retry
                    </button>
                </div>
            )}

            {!loading && !error && book && (
                <div className="book-detail-card">
                    <div className="book-detail-cover">
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
                        {hasActiveReservation ? (
                            <p className="reservation-status">You have already reserved this book.</p>
                        ) : book.available && book.quantity > 0 ? (
                            <button
                                className="reserve-button"
                                onClick={handleReserve}
                                aria-label="Reserve book"
                                disabled={hasActiveReservation}
                            >
                                Reserve Now
                            </button>
                        ) : (
                            <>
                                {isOnWaitingList ? (
                                    <>
                                        <p className="waiting-status">
                                            You are on the waiting list. Status: {waitingStatus?.charAt(0).toUpperCase() + waitingStatus?.slice(1)}
                                        </p>
                                        {isNotified && (
                                            <button
                                                className="reserve-button"
                                                onClick={handleReserve}
                                                aria-label="Reserve book after notification"
                                            >
                                                Reserve Now
                                            </button>
                                        )}
                                        <button
                                            className="cancel-waiting-button"
                                            onClick={handleCancelWaiting}
                                            aria-label="Cancel waiting list entry"
                                        >
                                            Cancel Waiting
                                        </button>
                                    </>
                                ) : (
                                    <button
                                        className="join-waiting-button"
                                        onClick={handleJoinWaiting}
                                        aria-label="Join waiting list"
                                    >
                                        Join Waiting List
                                    </button>
                                )}
                            </>
                        )}
                    </div>
                </div>
            )}

            {book && (
                <div className="reviews-section">
                    <h2>Reviews ({reviews.length})</h2>

                    {isAuthenticated && (
                        <form className="review-form" onSubmit={handleAddReview}>
                            <h3>Write a Review</h3>
                            <div className="form-group">
                                <label htmlFor="rating">Rating:</label>
                                <div className="star-rating" role="radiogroup" aria-label="Select review rating">
                                    {[1, 2, 3, 4, 5].map((num) => (
                                        <span
                                            key={num}
                                            className={`star ${newReview.rating >= num ? 'filled' : ''}`}
                                            onClick={() => handleStarClick(num)}
                                            role="radio"
                                            aria-checked={newReview.rating === num}
                                            tabIndex={0}
                                            onKeyDown={(e) => {
                                                if (e.key === 'Enter' || e.key === ' ') {
                                                    handleStarClick(num);
                                                    e.preventDefault();
                                                }
                                            }}
                                            aria-label={`${num} star${num > 1 ? 's' : ''}`}
                                        >
                                            ★
                                        </span>
                                    ))}
                                </div>
                            </div>
                            <div className="form-group">
                                <label htmlFor="comment">Comment:</label>
                                <textarea
                                    id="comment"
                                    value={newReview.comment}
                                    onChange={(e) => setNewReview({ ...newReview, comment: e.target.value })}
                                    required
                                    placeholder="Write your review here..."
                                    aria-label="Write your review"
                                />
                            </div>
                            <button
                                type="submit"
                                className="submit-review-button"
                                aria-label="Submit review"
                            >
                                Submit Review
                            </button>
                        </form>
                    )}

                    {reviews.length > 0 ? (
                        <div className="reviews-list">
                            {reviews.map((review) => (
                                <div key={review.reviewId} className="review-card">
                                    <div className="review-header">
                                        <span className="review-user">User ID: {review.userId}</span>
                                        <div className="review-rating" aria-label={`Rating ${review.rating} out of 5 stars`}>
                                            {[1, 2, 3, 4, 5].map((num) => (
                                                <span
                                                    key={num}
                                                    className={`star ${review.rating >= num ? 'filled' : ''}`}
                                                    aria-hidden="true"
                                                >
                                                    ★
                                                </span>
                                            ))}
                                        </div>
                                    </div>
                                    {review.comment && <p className="review-comment">{review.comment}</p>}
                                    <p className="review-date">
                                        Posted on: {new Date(review.reviewDate).toLocaleDateString()}
                                    </p>
                                    {isAuthenticated &&
                                        (review.userId === JSON.parse(sessionStorage.getItem("user"))?.id ||
                                            JSON.parse(sessionStorage.getItem("user"))?.userRole === "admin") && (
                                            <div className="review-actions">
                                                <button
                                                    className="delete-review-button"
                                                    onClick={() => handleDeleteReview(review.reviewId)}
                                                    aria-label={`Delete review by user ${review.userId}`}
                                                >
                                                    Delete
                                                </button>
                                            </div>
                                        )}
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

            {!loading && !error && !book && (
                <div className="no-book">
                    Book not found.
                    <button
                        onClick={handleBack}
                        aria-label="Back to dashboard"
                    >
                        Back to Dashboard
                    </button>
                </div>
            )}
        </div>
    );
};

export default Book;