import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { loginUser } from '../services/authApi';

interface LoginFormData {
    userAccount: string;
    userPassword: string;
}

const Login: React.FC = () => {
    const [formData, setFormData] = useState<LoginFormData>({
        userAccount: '',
        userPassword: '',
    });
    const [error, setError] = useState<string>('');
    const [loading, setLoading] = useState<boolean>(false);
    const navigate = useNavigate();

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        setFormData({
            ...formData,
            [e.target.name]: e.target.value,
        });
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError('');
        setLoading(true);

        try {
            const response = await loginUser(formData);
            // Store user data in localStorage (adjust based on your LoginUserVO structure)
            localStorage.setItem('user', JSON.stringify(response));
            navigate('/dashboard'); // Redirect to a protected route (create if needed)
        } catch (err: any) {
            setError(err.response?.data?.message || 'Login failed');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="login-container">
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
                Don't have an account? <a href="/register">Register here</a>
            </p>
        </div>
    );
};

export default Login;