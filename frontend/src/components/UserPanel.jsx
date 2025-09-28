import React from "react";
import "./UserPanel.css";

function UserPanel({ user, isOpen }) {
    return (
        <div
            className={`user-panel ${isOpen ? "open" : ""}`}
            onClick={(e) => e.stopPropagation()} // prevent overlay click
        >
            <h2>User Information</h2>
            <p><strong>Name:</strong> {user.firstName} {user.lastName}</p>
            <p><strong>Email:</strong> {user.userAccount}</p>
            <p><strong>Role:</strong> {user.userRole || "User"}</p>
            <p><strong>Reservations:</strong> <span onClick={() => navigate("/reservations")} className="link">View My Reservations</span></p>
        </div>
    );
}


export default UserPanel;
