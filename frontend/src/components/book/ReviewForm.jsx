import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { reviewService } from "../../services/reviewService";

/**
 * Write a Review: a 1–5 star rating and a comment (up to 1000 characters, as the server allows).
 * - bookId: the book reviewed
 * - isAuthenticated: a guest is sent to sign in
 * - onSubmitStart: clears the page's error
 * - onReviewAdded: reloads the reviews and the rating
 */
const ReviewForm = ({ bookId, isAuthenticated, onSubmitStart, onReviewAdded }) => {
    const navigate = useNavigate();
    const [newReview, setNewReview] = useState({ rating: 0, comment: "" });
    const [reviewSuccess, setReviewSuccess] = useState("");
    const [reviewError, setReviewError] = useState("");

    const handleAddReview = async (e) => {
        e.preventDefault();
        setReviewSuccess("");
        setReviewError("");

        if (!isAuthenticated) {
            navigate("/login");
            return;
        }
        if (newReview.rating < 1 || newReview.rating > 5) {
            setReviewError("Rating must be between 1 and 5.");
            return;
        }
        if (!newReview.comment.trim()) {
            setReviewError("Comment cannot be empty.");
            return;
        }

        try {
            onSubmitStart();
            const reviewData = { rating: newReview.rating, comment: newReview.comment };
            await reviewService.addReview(parseInt(bookId), reviewData);

            setReviewSuccess("✅ Review submitted successfully!");

            setNewReview({ rating: 0, comment: "" });

            // show the new review straight away
            await onReviewAdded();
        } catch (err) {
            window.alert(err.message || "Failed to add review");
            console.error("Error adding review:", err);
        }
    };

    const handleStarClick = (rating) => {
        setNewReview({ ...newReview, rating });
    };

    return (
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
                    maxLength={1000}
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
            {reviewSuccess && <p className="review-success">{reviewSuccess}</p>}
            {reviewError && <p className="review-error">{reviewError}</p>}
        </form>
    );
};

export default ReviewForm;
