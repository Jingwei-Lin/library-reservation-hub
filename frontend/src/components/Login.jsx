// Login.jsx
import './Login.css';
import React, { useState } from 'react';
import { useNavigate, useSearchParams, Link } from 'react-router-dom';
import { loginUser } from '../services/authApi';
import { safeRedirect } from '../safeRedirect';
import { getTheme } from '../themeUtils';

const Login = ({ setIsAuthenticated, setUser }) => {
    const theme = getTheme();
    const [formData, setFormData] = useState({ userAccount: '', userPassword: '' });
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();

    const handleChange = (e) => {
        setFormData({ ...formData, [e.target.name]: e.target.value });
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setLoading(true);

        try {
            const response = await loginUser(formData); // call backend
            sessionStorage.setItem('user', JSON.stringify(response));
            setUser(response);             // update app-level user state
            setIsAuthenticated(true);      // update app-level auth state
            // back to the page the user was on when their session ended, if any
            navigate(safeRedirect(searchParams.get('redirect'), '/dashboard'), { replace: true });
        } catch (err) {
            // Axios error message from authApi.js
            const backendMessage = err.message;
            setError(backendMessage || 'Login failed');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className={`page-container login-container ${theme === "dark" ? "dark-mode" : ""}`}>
            <button onClick={() => navigate("/dashboard")} className="back-button">
                ← Back to Dashboard
            </button>

            <h2>Login</h2>
            <form onSubmit={handleSubmit}>
                <div>
                    <label htmlFor="userAccount">Email:</label>
                    <input
                        type="email"
                        id="userAccount"
                        name="userAccount"
                        value={formData.userAccount}
                        onChange={handleChange}
                        required
                    />
                </div>
                <div>
                    <label htmlFor="userPassword">Password:</label>
                    <input
                        type="password"
                        id="userPassword"
                        name="userPassword"
                        value={formData.userPassword}
                        onChange={handleChange}
                        required
                    />
                </div>
                {error && <p className="error">{error}</p>}
                <button type="submit" disabled={loading}>
                    {loading ? 'Logging in...' : 'Login'}
                </button>
            </form>
            <p>
                Don&apos;t have an account? <Link to="/register">Register here</Link>
            </p>
        </div>
    );
};

export default Login;