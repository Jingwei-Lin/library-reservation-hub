import React, { useState } from 'react';
import './UserPanel.css';
import { FaUserCircle } from 'react-icons/fa';
import { logoutUser } from '../services/authApi';
import { useNavigate } from 'react-router-dom';

const UserPanel = ({ user, setIsAuthenticated }) => {
    const [open, setOpen] = useState(false);
    const navigate = useNavigate();

    const handleLogout = () => {
        logoutUser();
        setIsAuthenticated(false);
        navigate('/login');
    };

    return (
        <>
            {/* User icon */}
            <FaUserCircle
                className="user-icon"
                size={36}
                onClick={() => setOpen(!open)}
            />

            {/* Sliding panel */}
            <div className={`user-panel ${open ? 'open' : ''}`}>
                <h3>User Info</h3>
                <p>Name: {user.firstName || 'N/A'} {user.lastName || ''}</p>
                <p>Email: {user.userAccount || 'N/A'}</p>

                <button onClick={handleLogout}>Logout</button>
            </div>

            {/* Overlay */}
            {open && <div className="overlay" onClick={() => setOpen(false)} />}
        </>
    );
};

export default UserPanel;
