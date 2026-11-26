import React from "react";

/** Whether the signed-in user (from the session copy) may delete this review: their own, or any for an admin */
const canDelete = (review) => {
    const user = JSON.parse(sessionStorage.getItem("user"));
    return review.userId === user?.id || user?.userRole === "admin";
};

/**
 * The book's reviews, in the server's order, with Delete on the ones the user may delete.
 * - onDelete: asks to delete a review (by id)
 */
const ReviewList = ({ reviews, isAuthenticated, onDelete }) => (
    reviews.length > 0 ? (
        <div className="reviews-list">
            {reviews.map((review) => (
                <div key={review.reviewId} className="review-card">
                    <div className="review-header">
                        <span className="review-user">{review.userName}</span>
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
                    {isAuthenticated && canDelete(review) && (
                        <div className="review-actions">
                            <button
                                className="delete-review-button"
                                onClick={() => onDelete(review.reviewId)}
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
    )
);

export default ReviewList;
