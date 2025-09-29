import './App.css';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { useState, useEffect } from 'react';
import Login from './components/Login.jsx';
import Register from './components/Register.jsx';
import Dashboard from './components/Dashboard.jsx';
import UserPanel from './components/UserPanel';
import Reservations from './components/Reservations';
import Book from './components/Book'; // Adjust path if needed

function App() {
    // Centralized authentication state
    const [isAuthenticated, setIsAuthenticated] = useState(() => !!localStorage.getItem("user"));
    const [user, setUser] = useState(() => JSON.parse(localStorage.getItem("user") || '{}'));

    // Theme state (default to light if none saved)
    const [theme, setTheme] = useState(() => localStorage.getItem("theme") || "light");

    // Apply theme to body whenever it changes
    useEffect(() => {
        document.body.classList.remove("light", "dark"); // clear old classes
        document.body.classList.add(theme); // apply new class
        localStorage.setItem("theme", theme); // persist in localStorage
    }, [theme]);

    // Toggle between light/dark themes
    const toggleTheme = () => {
        setTheme(prev => (prev === "light" ? "dark" : "light"));
    };

    return (
        <Router>
            <div className={`App ${theme}`}>
                {/* Show user panel only when logged in */}
                {isAuthenticated && (
                    <UserPanel
                        user={user}
                        setIsAuthenticated={setIsAuthenticated}
                        toggleTheme={toggleTheme}
                        theme={theme}
                    />
                )}

                <Routes>
                    <Route
                        path="/login"
                        element={
                            !isAuthenticated ? (
                                <Login setIsAuthenticated={setIsAuthenticated} setUser={setUser} />
                            ) : (
                                <Navigate to="/dashboard" />
                            )
                        }
                    />
                    <Route
                        path="/register"
                        element={
                            !isAuthenticated ? <Register /> : <Navigate to="/dashboard" />
                        }
                    />
                    <Route
                        path="/dashboard"
                        element={
                            <Dashboard
                                user={user}
                                setIsAuthenticated={setIsAuthenticated}
                                isAuthenticated={isAuthenticated}
                                theme={theme}
                                toggleTheme={toggleTheme}
                            />
                        }
                    />
                    <Route
                        path="/book/:id"
                        element={<Book isAuthenticated={isAuthenticated} />}
                    />
                    <Route path="/" element={<Navigate to="/login" />} />

                    <Route
                        path="/reservations"
                        element={<Reservations isAuthenticated={isAuthenticated} />}
                    />
                </Routes>
            </div>
        </Router>
    );
}

export default App;
