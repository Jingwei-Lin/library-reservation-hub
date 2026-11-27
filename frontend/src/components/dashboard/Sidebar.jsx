import React from "react";
import { asButton } from "../../asButton";

// [genre, label]: the genre as stored, and how the menu shows it
const GENRES = [
    ["Fantasy", "Fantasy"],
    ["Science Fiction", "Science Fiction"],
    ["Mystery", "Mystery"],
    ["Romance", "Romance"],
    ["Historical Fiction", "Historical Fiction"],
    ["Horror", "Horror"],
    ["Thriller", "Thriller"],
    ["Young Adult", "Young Adult"],
    ["Non-Fiction", "Non-Fiction"],
    ["Poetry Classic", "Poetry/Classic"],
];

/**
 * The Dashboard's left menu: All Books, My Reservations, My Waiting List (marked when a copy is held for the
 * user), and the genres.
 * - onAllBooks, onGenre(genre): change the list
 * - onLeave(path): leaves the Dashboard for another page
 */
const Sidebar = ({ hasNotification, onAllBooks, onGenre, onLeave }) => (
    <nav className="sidebar">
        <div className="logo">
            <span className="logo-icon">📚</span>
            <span className="logo-text">Library Hub</span>
        </div>
        <div className="nav-box">
            <h3>Navigation</h3>
            <ul>
                <li {...asButton(onAllBooks)}>
                    All Books
                </li>
                <li {...asButton(() => onLeave("/reservations"))}>
                    My Reservations
                </li>
                <li
                    {...asButton(() => onLeave("/waitinglist"))}
                    className={hasNotification ? "shiny" : ""}
                >
                    My Waiting List
                    {hasNotification && <span className="notification-dot"></span>}
                </li>
            </ul>
        </div>
        <div className="category-box">
            <h3>Categories</h3>
            <ul>
                {GENRES.map(([genre, label]) => (
                    <li key={genre} {...asButton(() => onGenre(genre))}>{label}</li>
                ))}
            </ul>
        </div>
    </nav>
);

export default Sidebar;
