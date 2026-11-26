// App.jsx

import './App.css';
import { BrowserRouter as Router, Routes, Route, Navigate, useNavigate, useLocation } from 'react-router-dom';
import { useState, useEffect, useRef, useCallback } from 'react';
import Login from './components/Login.jsx';
import Register from './components/Register.jsx';
import Dashboard from './components/Dashboard.jsx';
import AdminLogin from './components/AdminLogin.jsx';
import AdminDashboard from './components/AdminDashboard.jsx';
import Reservations from './components/Reservations.jsx';
import { validateSession, logoutUser } from './services/authApi';
import { setSessionEndedHandler } from './services/api';
import { safeRedirect } from './safeRedirect';
import WaitingList from "./components/WaitingList.jsx";
import Profile from "./components/Profile.jsx";

// pages that need an account: when the session ends there, the user signs in again and comes back
const ACCOUNT_PAGES = ['/reservations', '/waitinglist', '/profile', '/admin/dashboard'];

const NOTICES = {
    expired: 'Your session has ended. Please sign in again.',
    banned: 'Your account has been suspended.',
};

function AppRoutes() {
    const [isAuthenticated, setIsAuthenticated] = useState(() => !!sessionStorage.getItem("user"));
    const [user, setUser] = useState(() => JSON.parse(sessionStorage.getItem("user") || '{}'));
    const [notice, setNotice] = useState(null);
    const navigate = useNavigate();
    const location = useLocation();

    // read by the session-ended handler, which lives outside React's render cycle
    const signedInRef = useRef(isAuthenticated);
    const endedRef = useRef(false);
    useEffect(() => {
        signedInRef.current = isAuthenticated;
        if (isAuthenticated) {
            // signed in (again): a new session, nothing to report
            endedRef.current = false;
            setNotice(null);
        }
    }, [isAuthenticated]);

    // Forget the signed-in user everywhere. The chat is keyed by the user (Dashboard), so its history and chat id
    // go too: nothing of the previous user is left for the next person at the same computer.
    const clearSignedIn = useCallback(() => {
        signedInRef.current = false;
        sessionStorage.removeItem('user');
        sessionStorage.removeItem('token');
        setUser({});
        setIsAuthenticated(false);
    }, []);

    // The Sign out button: end the server session, forget the user, back to the books as a guest
    const signOut = useCallback(async () => {
        await logoutUser();
        clearSignedIn();
        setNotice(null);
        navigate('/dashboard');
    }, [clearSignedIn, navigate]);

    // The shared API client calls this when the server says the session has ended (40100) or the account is
    // banned (40301). Several calls can fail at once; only the first one signs out and redirects.
    useEffect(() => {
        setSessionEndedHandler((reason) => {
            if (endedRef.current) return true;
            if (!signedInRef.current) return false;   // a guest: the page handles the answer itself
            endedRef.current = true;
            clearSignedIn();
            setNotice(NOTICES[reason] || NOTICES.expired);

            const { pathname, search } = window.location;
            if (ACCOUNT_PAGES.some((page) => pathname.startsWith(page))) {
                const signIn = pathname.startsWith('/admin') ? '/admin/login' : '/login';
                navigate(`${signIn}?redirect=${encodeURIComponent(pathname + search)}`, { replace: true });
            }
            return true;
        });
        return () => setSessionEndedHandler(() => false);
    }, [navigate, clearSignedIn]);

    // Validate session on mount
    useEffect(() => {
        const checkSession = async () => {
            try {
                const response = await validateSession(); // Always call
                if (response && response.code === 0 && response.data) {
                    // Session valid → restore from backend
                    const userData = response.data;
                    sessionStorage.setItem('user', JSON.stringify(userData));
                    setUser(userData);
                    setIsAuthenticated(true);
                } else {
                    // Invalid → ensure clean state
                    logoutUser();
                    setIsAuthenticated(false);
                    setUser({});
                }
            } catch (error) {
                console.error('Session validation failed:', error);
                logoutUser();
                setIsAuthenticated(false);
                setUser({});
            }
        };
        checkSession();
    }, []);

    // already signed in on a sign-in page: go where the sign-in would have gone
    const afterSignIn = (fallback) => safeRedirect(new URLSearchParams(location.search).get('redirect'), fallback);

    return (
        <div className="App">
            {notice && (
                <div className="session-notice" role="status">
                    <span>{notice}</span>
                    <button type="button" aria-label="Dismiss" onClick={() => setNotice(null)}>×</button>
                </div>
            )}
            <Routes>
                <Route
                    path="/dashboard"
                    element={
                        <Dashboard
                            user={user}
                            setUser={setUser}
                            setIsAuthenticated={setIsAuthenticated}
                            isAuthenticated={isAuthenticated}
                            onSignOut={signOut}
                        />
                    }
                />
                <Route
                    path="/login"
                    element={
                        !isAuthenticated ? (
                            <Login
                                setIsAuthenticated={setIsAuthenticated}
                                setUser={setUser}
                            />
                        ) : (
                            <Navigate to={afterSignIn('/dashboard')} replace />
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
                <Route path="/" element={<Navigate to="/dashboard" />} />
                <Route
                    path="/reservations"
                    element={<Reservations isAuthenticated={isAuthenticated} />}
                />
                <Route path="/waitinglist" element={<WaitingList isAuthenticated={isAuthenticated} />} />
                <Route path="/profile" element={<Profile user={user} />} />
                <Route path="/admin/login" element={<AdminLogin setIsAuthenticated={setIsAuthenticated} setUser={setUser} />} />
                <Route path="/admin/dashboard" element={<AdminDashboard user={user} isAuthenticated={isAuthenticated} setIsAuthenticated={setIsAuthenticated} />} />
                <Route path="/admin" element={<Navigate to="/admin/login" />} />
            </Routes>
        </div>
    );
}

function App() {
    return (
        <Router>
            <AppRoutes />
        </Router>
    );
}

export default App;
