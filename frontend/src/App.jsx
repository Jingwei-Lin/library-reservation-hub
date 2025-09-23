import './App.css';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { useState } from 'react';
import Login from './components/Login.jsx';
import Register from './components/Register.jsx';
import Dashboard from './components/Dashboard.jsx';
import UserPanel from './components/UserPanel';
import Book from './components/Book'; // Adjust path based on your structure

function App() {
    // Centralized authentication state
    const [isAuthenticated, setIsAuthenticated] = useState(!!localStorage.getItem("user"));
    const [user, setUser] = useState(JSON.parse(localStorage.getItem("user") || '{}'));

    return (
        <Router>
            <div className="App">
                {/* Show user panel only when logged in */}
                {isAuthenticated && <UserPanel user={user} setIsAuthenticated={setIsAuthenticated} />}

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
                            !isAuthenticated ? (
                                <Register />
                            ) : (
                                <Navigate to="/dashboard" />
                            )
                        }
                    />
                    <Route
                        path="/dashboard"
                        element={
                            <Dashboard
                                user={user}
                                setIsAuthenticated={setIsAuthenticated}
                                isAuthenticated={isAuthenticated}
                            />
                        }
                    />
                    <Route
                        path="/book/:id"
                        element={
                            <Book isAuthenticated={isAuthenticated} />
                        }
                    />
                    <Route path="/" element={<Navigate to="/login" />} />
                </Routes>
            </div>
        </Router>
    );
}

export default App;