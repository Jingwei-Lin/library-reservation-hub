import React from "react";
import { Link } from "react-router-dom";

/** Any address the app doesn't have */
const NotFound = () => (
    <div className="page-container not-found">
        <h2>Page not found</h2>
        <p>There is nothing at this address.</p>
        <Link to="/dashboard">Back to the books</Link>
    </div>
);

export default NotFound;
