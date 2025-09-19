import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { registerUser } from '../services/authApi';

interface RegisterFormData {
    userAccount: string;
    userPassword: string;
    checkPassword: string;
    firstName: string;
    lastName: string;
    phone: string;
}

const Register: React.FC = () => {
    const [formData, setFormData] = useState<RegisterFormData>({
        userAccount: '',
        userPassword: '',
        checkPassword: '',
        firstName: '',
        lastName: '',
        phone: '',
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

        if (formData.userPassword !== formData.checkPassword) {
            setError('Passwords do not match');
            return;
        }

        setLoading(true);

        try {
            await registerUser(formData);
            alert('Registration successful! Please log in.'); // Or redirect to login
            navigate('/login');
        } catch (err: any) {
            setError(err.response?.data?.message || 'Registration failed');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="register-container">
            <h2>Register</h2>
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
                <div>
                    <label htmlFor="checkPassword">Confirm Password:</label>
                    <input
                        type="password"
                        id="checkPassword"
                        name="checkPassword"
                        value={formData.checkPassword}
                        onChange={handleChange}
                        required
                    />
                </div>
                <div>
                    <label htmlFor="firstName">First Name:</label>
                    <input
                        type="text"
                        id="firstName"
                        name="firstName"
                        value={formData.firstName}
                        onChange={handleChange}
                        required
                    />
                </div>
                <div>
                    <label htmlFor="lastName">Last Name:</label>
                    <input
                        type="text"
                        id="lastName"
                        name="lastName"
                        value={formData.lastName}
                        onChange={handleChange}
                        required
                    />
                </div>
                <div>
                    <label htmlFor="phone">Phone:</label>
                    <input
                        type="tel"
                        id="phone"
                        name="phone"
                        value={formData.phone}
                        onChange={handleChange}
                        required
                    />
                </div>
                {error && <p className="error">{error}</p>}
                <button type="submit" disabled={loading}>
                    {loading ? 'Registering...' : 'Register'}
                </button>
            </form>
            <p>
                Already have an account? <a href="/login">Login here</a>
            </p>
        </div>
    );
};

export default Register;