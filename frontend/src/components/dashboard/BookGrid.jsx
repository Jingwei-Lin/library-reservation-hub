import React from "react";
import { coverUrl, showPlaceholderCover } from "../../covers";
import { asButton } from "../../asButton";

/**
 * The books on the current page, as cards.
 * - onSelect(book): a card was clicked
 */
const BookGrid = ({ books, onSelect }) => (
    <div className="books-grid">
        {books.map((book) => (
            <div
                key={book.bookId || book.title + book.author}
                className="book-card"
                {...asButton(() => onSelect(book))}
                style={{ cursor: book.bookId ? 'pointer' : 'not-allowed' }}
            >
                <div className="book-cover">
                    {book.path ? (
                        <img
                            src={coverUrl(book.path)}
                            alt={book.title}
                            onError={showPlaceholderCover(book)}
                        />
                    ) : (
                        <div className="book-cover-placeholder">📖</div>
                    )}
                </div>
                <div className="book-info">
                    <h3>{book.title || 'Untitled'}</h3>
                    <p className="books-author">by {book.author || 'Unknown'}</p>
                    <p className="books-genre">{book.genre || 'Unknown'}</p>
                    {book.avgRating != null && (
                        <div className="book-rating">
                            {book.avgRating.toFixed(1)} ★
                        </div>
                    )}
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
);

export default BookGrid;
