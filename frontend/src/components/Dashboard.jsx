import './Dashboard.css';
import React from 'react';

const Dashboard = ({ user }) => {
    return (
        <div className="page-container dashboard-container">
            <h1>Welcome to the Dashboard!</h1>
            {user.firstName && <p>Hello, {user.firstName} {user.lastName}</p>}
            {user.userAccount && <p>Email: {user.userAccount}</p>}
            {/* No logout button here; use the panel */}
        </div>
    );
};

export default Dashboard;
