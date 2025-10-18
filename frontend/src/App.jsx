// App.jsx
import './App.css';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { useState, useEffect } from 'react';
import Login from './components/Login.jsx';
import Register from './components/Register.jsx';
import Dashboard from './components/Dashboard.jsx';
import AdminLogin from './components/AdminLogin.jsx';
import AdminDashboard from './components/AdminDashboard.jsx';

import Reservations from './components/Reservations.jsx';
import Book from './components/Book';
import { validateSession, logoutUser } from './services/authApi';
import ReservationForm from "./components/ReservationForm.jsx";
import WaitingList from "./components/WaitingList.jsx";
import Profile from "./components/Profile.jsx";

function App() {
    const [isAuthenticated, setIsAuthenticated] = useState(() => !!sessionStorage.getItem("user"));
    const [user, setUser] = useState(() => JSON.parse(sessionStorage.getItem("user") || '{}'));
    const [theme, setTheme] = useState(() => sessionStorage.getItem("theme") || "light");

    // Validate session on mount
    useEffect(() => {
        const checkSession = async () => {
            if (sessionStorage.getItem("user")) {
                try {
                    await validateSession(); // Check with backend
                } catch (error) {
                    console.error('Session validation failed:', error.message);
                    logoutUser(); // Clear sessionStorage
                    setIsAuthenticated(false);
                    setUser({});
                }
            }
        };
        checkSession();
    }, []);

    useEffect(() => {
        document.body.classList.remove("light", "dark");
        document.body.classList.add(theme);
        sessionStorage.setItem("theme", theme);
    }, [theme]);

    const toggleTheme = () => {
        setTheme(prev => (prev === "light" ? "dark" : "light"));
    };

    return (
        <Router>
            <div className={`App ${theme}`}>

                <Routes>
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
                        path="/book/:id"
                        element={<Book isAuthenticated={isAuthenticated} />}
                    />
                    <Route path="/" element={<Navigate to="/dashboard" />} />
                    <Route
                        path="/reservations"
                        element={<Reservations isAuthenticated={isAuthenticated} />}
                    />
                    <Route path="/reservation/form/:bookId" element={<ReservationForm isAuthenticated={isAuthenticated} setIsAuthenticated={setIsAuthenticated} />} />
                    <Route path="/waitinglist" element={<WaitingList isAuthenticated={isAuthenticated} setIsAuthenticated={setIsAuthenticated} />} />
                    <Route path="/profile" element={<Profile user={user} setIsAuthenticated={setIsAuthenticated} theme={theme} />} />
                    <Route path="/admin/login" element={<AdminLogin setIsAuthenticated={setIsAuthenticated} setUser={setUser} />} />
                    <Route path="/admin/dashboard" element={<AdminDashboard user={user} isAuthenticated={isAuthenticated} setIsAuthenticated={setIsAuthenticated} />} />
                    <Route path="/admin" element={<Navigate to="/admin/login" />} />
                </Routes>
            </div>
        </Router>
    );
}

export default App;