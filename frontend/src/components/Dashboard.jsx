import React, { useState, useRef, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import "./Dashboard.css";

const Dashboard = ({ user, setIsAuthenticated, isAuthenticated }) => {
    const [isPanelOpen, setIsPanelOpen] = useState(false);
    const panelRef = useRef(null);
    const navigate = useNavigate();

    const handleLogout = () => {
        localStorage.removeItem("user");
        setIsAuthenticated(false);
        setIsPanelOpen(false);
    };

    const handleLoginRedirect = () => {
        navigate("/login");
    };

    const handleProtectedClick = () => {
        if (!isAuthenticated) {
            navigate("/login");
        }
    };

    // Close panel when clicking outside
    useEffect(() => {
        const handleClickOutside = (event) => {
            if (panelRef.current && !panelRef.current.contains(event.target)) {
                setIsPanelOpen(false);
            }
        };
        document.addEventListener("mousedown", handleClickOutside);
        return () => {
            document.removeEventListener("mousedown", handleClickOutside);
        };
    }, []);

    return (
        <div className="dashboard">
            {/* Sidebar */}
            <nav className="sidebar">
                <div className="logo">
                    <span className="logo-icon">📚</span>
                    <span className="logo-text">LibraSys</span>
                </div>

                <div className="nav-box">
                    <h3>Navigation</h3>
                    <ul>
                        <li onClick={handleProtectedClick}>Home</li>
                        <li onClick={handleProtectedClick}>Browsed Books</li>
                        <li onClick={handleProtectedClick}>My Borrowings</li>
                    </ul>
                </div>

                <div className="category-box">
                    <h3>Categories</h3>
                    <ul>
                        <li>Fiction</li>
                        <li>Non-fiction</li>
                        <li>History</li>
                        <li>Technology</li>
                    </ul>
                </div>
            </nav>

            {/* Main Content */}
            <div className="main-content">
                <header className="header">
                    <div className="search-bar">
                        <input
                            type="text"
                            placeholder="Search books, authors, categories..."
                        />
                        <button>Search</button>
                    </div>

                    <div className="user-wrapper" ref={panelRef}>
                        {isAuthenticated ? (
                            <>
                                <div
                                    className="user-icon"
                                    onClick={() => setIsPanelOpen(!isPanelOpen)}
                                >
                                    👤
                                </div>

                                <div className={`user-panel ${isPanelOpen ? "open" : ""}`}>
                                    <h3>User Panel</h3>
                                    <p>{user.firstName} {user.lastName}</p>
                                    <p>Email: {user.userAccount}</p>
                                    <button onClick={handleLogout}>Logout</button>
                                </div>
                            </>
                        ) : (
                            <button className="login-button" onClick={handleLoginRedirect}>
                                Login
                            </button>
                        )}
                    </div>
                </header>

                <div className="content-wrapper">
                    {/* Central content */}
                    <div className="central-content">
                        <div className="welcome">
                            <h1>Welcome to our digital library</h1>
                            <p>Explore our digital library now</p>
                        </div>

                        <div className="book-listing">
                            <h2>Book Listing</h2>
                            <p>[Books will appear here]</p>
                        </div>
                    </div>

                    {/* Right panel */}
                    <div className="right-panel">
                        <div className="stats-box">
                            <h3>Library Statistics</h3>
                            <p>Total books: 100</p>
                            <p>Available books: 75</p>
                            <p>You borrowed: 0</p>
                        </div>

                        <div className="recommend-box">
                            <h3>Recommend for you</h3>
                            <ul>
                                <li>The Great Gatsby</li>
                                <li>1984</li>
                                <li>Sapiens</li>
                            </ul>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default Dashboard;
