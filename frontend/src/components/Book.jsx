import React, { useState, useEffect } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import { bookService } from "../services/bookService";
import { reservationService } from "../services/reservationService";
import { reviewService } from "../services/reviewService";
import { waitingListService } from "../services/waitingListService";
import { isSessionEnded } from "../services/api";
import ReservationDialog from "./book/ReservationDialog";
import DeleteReviewDialog from "./book/DeleteReviewDialog";
import RatingSummary from "./book/RatingSummary";
import AvailabilityActions from "./book/AvailabilityActions";
import ReviewForm from "./book/ReviewForm";
import ReviewList from "./book/ReviewList";
import "./Book.css";

/**
 * A book's page: its details and rating, what the user can do (reserve, or the waiting list), and the reviews.
 * ?reserve=true opens the Reserve dialog once the book has loaded, if the user may reserve it.
 */
const Book = ({ isAuthenticated, setIsAuthenticated, id, onBack }) => {
    const navigate = useNavigate();
    const location = useLocation();
    const [book, setBook] = useState(null);
    const [reviews, setReviews] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [isOnWaitingList, setIsOnWaitingList] = useState(false);
    const [isNotified, setIsNotified] = useState(false);
    const [holdExpiresAt, setHoldExpiresAt] = useState(null);
    const [waitingStatus, setWaitingStatus] = useState(null);
    const [hasActiveReservation, setHasActiveReservation] = useState(false);
    const [autoOpenReserve, setAutoOpenReserve] = useState(false);
    const [bookRating, setBookRating] = useState(null);

    // the delete-review dialog, and the review it is about
    const [showDeletePopup, setShowDeletePopup] = useState(false);
    const [reviewToDelete, setReviewToDelete] = useState(null);

    const [showReservationPopup, setShowReservationPopup] = useState(false);

    useEffect(() => {
        const fetchBookAndReviews = async () => {
            try {
                setLoading(true);
                setError("");

                const promises = [
                    bookService.getBookById(id).catch(err => ({ code: -1, message: err.message })),
                    bookService.getRatingByBookId(id).catch(err => ({ code: -1, message: err.message })),
                    reviewService.getReviewsByBookId(id).catch(err => ({ code: -1, message: err.message }))
                ];

                if (isAuthenticated) {
                    promises.push(reservationService.hasActiveReservation(id).catch(err => ({ code: -1, message: err.message })));
                    promises.push(waitingListService.getMyWaitingList().catch(err => ({ code: -1, message: err.message })));
                }

                const responses = await Promise.all(promises);

                let bookRes, ratingRes, reviewRes, reservationRes, waitRes;
                [bookRes, ratingRes, reviewRes, reservationRes, waitRes] = responses.length === 5 ? responses : [...responses, null, null];

                // Process book
                if (bookRes && bookRes.code === 0 && bookRes.data) {
                    setBook(bookRes.data);
                } else {
                    console.warn("Book fetch failed:", bookRes?.message);
                    setError(bookRes?.message || "Invalid response format for book");
                    return; // Early return if book fails, as it's critical
                }

                // Process rating
                if (ratingRes && ratingRes.code === 0 && ratingRes.data) {
                    setBookRating(ratingRes.data);
                } else {
                    console.warn("No rating data:", ratingRes?.message);
                    setBookRating(null);
                }

                // Process reviews
                if (reviewRes && reviewRes.code === 0 && reviewRes.data) {
                    setReviews(Array.isArray(reviewRes.data) ? reviewRes.data : []);
                } else {
                    console.warn(reviewRes?.message || "No reviews found");
                    setReviews([]);
                }

                // If authenticated, process reservation and waiting list
                if (isAuthenticated) {
                    // Process reservation
                    if (reservationRes && reservationRes.code === 0) {
                        setHasActiveReservation(reservationRes.data || false);
                    } else {
                        console.warn(reservationRes?.message || "Failed to check reservation status");
                        setHasActiveReservation(false);
                    }

                    // Process waiting list
                    if (waitRes && waitRes.code === 0 && waitRes.data) {
                        const waitItem = waitRes.data.find((item) => item.bookId === parseInt(id));
                        if (waitItem) {
                            setIsOnWaitingList(waitItem.status === "waiting" || waitItem.status === "notified");
                            setIsNotified(waitItem.status === "notified");
                            setHoldExpiresAt(waitItem.status === "notified" ? waitItem.holdExpiresAt : null);
                            setWaitingStatus(waitItem.status);
                        } else {
                            setIsOnWaitingList(false);
                            setIsNotified(false);
                            setWaitingStatus(null);
                        }
                    } else {
                        console.warn(waitRes?.message || "Failed to check waiting list status");
                        setIsOnWaitingList(false);
                        setIsNotified(false);
                        setWaitingStatus(null);
                    }
                }
            } catch (err) {
                // an ended session is handled by the app: this page stays, as a guest
                if (isSessionEnded(err)) {
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
    }, [id, setIsAuthenticated, isAuthenticated, hasActiveReservation, setHasActiveReservation]);

    // Check for ?reserve=true in URL and set flag to auto-open popup
    useEffect(() => {
        const params = new URLSearchParams(location.search);
        if (params.get('reserve') === 'true') {
            setAutoOpenReserve(true);
            // Remove the 'reserve' param from URL to clean it up
            params.delete('reserve');
            navigate({ search: params.toString() }, { replace: true });
        }
    }, [location.search, navigate]);

    // Auto-open the reservation popup after data is loaded if flag is set
    useEffect(() => {
        if (autoOpenReserve && !loading && book && !error) {
            // only when the user may reserve: a copy on the shelf, or one held for them
            if (!hasActiveReservation && (book.available || isNotified)) {
                setShowReservationPopup(true);
            }
            setAutoOpenReserve(false); // Reset flag after handling
        }
    }, [autoOpenReserve, loading, book, error, hasActiveReservation, isNotified]);

    const handleBack = () => {
        if (onBack) {
            // onBack(); // Call onBack if provided
            navigate('/dashboard');
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

    const fetchBookRating = async () => {
        try {
            const response = await bookService.getRatingByBookId(id);
            if (response && response.code === 0 && response.data) {
                setBookRating(response.data);
            } else {
                setBookRating(null);
            }
        } catch (err) {
            console.warn("Failed to refresh rating:", err);
            setBookRating(null);
        }
    };

    // the reviews and the rating again, after a review is added or deleted
    const refreshReviews = async () => {
        const reviewResponse = await reviewService.getReviewsByBookId(id);
        if (reviewResponse && reviewResponse.code === 0 && Array.isArray(reviewResponse.data)) {
            setReviews(reviewResponse.data);
            await fetchBookRating();
        }
    };

    const handleDeleteReview = async (reviewId) => {
        if (!isAuthenticated) {
            navigate("/login");
            return;
        }
        // open confirmation popup
        setReviewToDelete(reviewId);
        setShowDeletePopup(true);
    };

    const confirmDeleteReview = async () => {
        try {
            setError("");
            await reviewService.deleteReview(reviewToDelete);
            setShowDeletePopup(false);
            setReviewToDelete(null);
            await refreshReviews();
        } catch (err) {
            console.error("Error deleting review:", err);
            setShowDeletePopup(false);
            setReviewToDelete(null);
        }
    };

    const cancelDeleteReview = () => {
        setShowDeletePopup(false);
        setReviewToDelete(null);
    };

    return (
        <div className="book-detail-container">
            {showDeletePopup && (
                <DeleteReviewDialog onCancel={cancelDeleteReview} onConfirm={confirmDeleteReview} />
            )}

            {showReservationPopup && (
                <ReservationDialog
                    book={book}
                    bookId={id}
                    isAuthenticated={isAuthenticated}
                    onClose={() => setShowReservationPopup(false)}
                    onReserved={() => setHasActiveReservation(true)}
                />
            )}
            <button className="book-back-button" onClick={handleBack} aria-label="Back to dashboard">
                ← Back
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
                        <p className="book-genre">{book.genre}</p>

                        {bookRating && <RatingSummary bookRating={bookRating} />}

                        <p className="book-description-title">Description:</p>
                        <p className="book-description">{book.description}</p>
                        <div className="book-status">
                            <span className={`availability ${book.available ? 'available' : 'unavailable'}`}>
                                {book.available ? 'Available' : 'Unavailable'}
                            </span>
                            {book.quantity !== undefined && (
                                <span className="quantity">Quantity: {book.quantity}</span>
                            )}
                        </div>
                        <AvailabilityActions
                            book={book}
                            hasActiveReservation={hasActiveReservation}
                            isOnWaitingList={isOnWaitingList}
                            isNotified={isNotified}
                            holdExpiresAt={holdExpiresAt}
                            waitingStatus={waitingStatus}
                            onReserve={handleReserve}
                            onJoinWaiting={handleJoinWaiting}
                            onCancelWaiting={handleCancelWaiting}
                        />
                    </div>
                </div>
            )}

            {book && (
                <div className="reviews-section">
                    <h2>Reviews ({reviews.length})</h2>

                    {isAuthenticated && (
                        <ReviewForm
                            bookId={id}
                            isAuthenticated={isAuthenticated}
                            onSubmitStart={() => setError("")}
                            onReviewAdded={refreshReviews}
                        />
                    )}

                    <ReviewList reviews={reviews} isAuthenticated={isAuthenticated} onDelete={handleDeleteReview} />
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
