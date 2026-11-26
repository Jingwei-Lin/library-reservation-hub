import React from "react";

/** Five stars for an average rating: full, half (from .25 to .75) or empty */
const starClass = (star, rating) => {
    const fullStars = Math.floor(rating);
    const decimal = rating - fullStars;
    let isFilled = star <= fullStars;
    let isHalf = false;
    if (!isFilled && star === fullStars + 1 && decimal >= 0.25 && decimal < 0.75) {
        isHalf = true;
    } else if (!isFilled && star === fullStars + 1 && decimal >= 0.75) {
        isFilled = true;
    }
    return isFilled ? "filled" : isHalf ? "half" : "";
};

/** The book's average rating, as stars and a number */
const RatingSummary = ({ bookRating }) => (
    <div className="book-rating-display">
        <div className="star-rating-large">
            {[1, 2, 3, 4, 5].map((star) => (
                <span key={star} className={`star large ${starClass(star, bookRating.avgRating ?? 0)}`}> ★ </span>
            ))}
        </div>
        {(bookRating?.avgRating ?? 0) > 0 ? (
            <p className="rating-text">
                <strong>{bookRating.avgRating.toFixed(1)}</strong> out of 5
            </p>
        ) : (
            <p className="rating-text">No ratings yet</p>
        )}
    </div>
);

export default RatingSummary;
