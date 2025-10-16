import React, { useState, useRef, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { logoutUser } from "../services/authApi";
import "./Header.css";

const Header = ({ user, setIsAuthenticated, isAuthenticated, theme, toggleTheme, searchTerm, setSearchTerm, handleSearch }) => {
    const [isPanelOpen, setIsPanelOpen] = useState(false);
    const panelRef = useRef(null);
    const navigate = useNavigate();

    const handleLogout = () => {
        logoutUser();
        setIsAuthenticated(false);
        setIsPanelOpen(false);
    };

    const handleLoginRedirect = () => {
        navigate("/login");
    };

    const handleProfileRedirect = () => {
        navigate("/profile");
        setIsPanelOpen(false);
    };

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
        <header className={`header ${theme === "dark" ? "dark-mode" : ""}`}>
            <form className="search-bar" onSubmit={handleSearch}>
                <input
                    type="text"
                    placeholder="Search books, authors, categories..."
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                />
                <button type="submit">Search</button>
                <button type="button" onClick={() => { setSearchTerm(""); handleSearch({ preventDefault: () => {} }); }} className="clear-btn">
                    Clear
                </button>
            </form>
            <div className="user-wrapper" ref={panelRef}>
                {isAuthenticated ? (
                    <>
                        <div className="dark-mode-toggle" onClick={toggleTheme}>
                            {theme === "dark" ? "🌞" : "🌙"}
                        </div>
                        <div className="user-icon" onClick={() => setIsPanelOpen(!isPanelOpen)}>
                            👤
                        </div>
                        <div className={`user-panel ${isPanelOpen ? "open" : ""}`}>
                            <h3>User Panel</h3>
                            <p>{user?.firstName} {user?.lastName}</p>
                            <p>Email: {user?.userAccount}</p>
                            <p>UserID: {user?.id}</p>
                            <button onClick={handleProfileRedirect}>Profile</button>
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
    );
};

export default Header;