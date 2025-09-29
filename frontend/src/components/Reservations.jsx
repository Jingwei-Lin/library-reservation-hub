import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { reservationService } from "../services/reservationService";
import "./Reservations.css";

const Reservations = ({ isAuthenticated }) => {
    const navigate = useNavigate();
    const [reservations, setReservations] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        if (!isAuthenticated) {
            navigate("/login");
            return;
        }

        const fetchReservations = async () => {
            try {
                setLoading(true);
                setError("");
                const response = await reservationService.getUserReservations();

                if (response && response.code === 0 && response.data) {
                    setReservations(Array.isArray(response.data) ? response.data : []);
                } else {
                    setError(response?.msg || "Invalid response format from server");
                }
            } catch (err) {
                setError(err.message || "Failed to load reservations");
            } finally {
                setLoading(false);
            }
        };

        fetchReservations();
    }, [isAuthenticated, navigate]);

    const handleCancel = async (reservationId) => {
        if (!window.confirm("Are you sure you want to cancel this reservation?")) return;

        try {
            setError("");
            const response = await reservationService.cancelReservation(reservationId);
            if (response && response.code === 0) {
                setReservations(reservations.filter(r => r.reservationId !== reservationId));
            } else {
                setError(response?.msg || "Failed to cancel reservation");
            }
        } catch (err) {
            setError(err.message || "Failed to cancel reservation");
        }
    };

    return (
        <div className="reservations-container">
            <h2>My Reservations</h2>
            <button onClick={() => navigate("/dashboard")} className="back-button">
                ← Back to Dashboard
            </button>

            {loading && <div className="loading">Loading reservations...</div>}
            {error && (
                <div className="error">
                    {error}
                    <button onClick={() => window.location.reload()} className="retry-btn">
                        Retry
                    </button>
                </div>
            )}

            {!loading && !error && reservations.length > 0 && (
                <table className="reservations-table">
                    <thead>
                    <tr>
                        <th>Book Title</th>
                        <th>Reservation Date</th>
                        <th>Pickup Date</th>
                        <th>Due Date</th>
                        <th>Status</th>
                        <th>Actions</th>
                    </tr>
                    </thead>
                    <tbody>
                    {reservations.map((res) => (
                        <tr key={res.reservationId}>
                            <td>{res.bookTitle || 'Unknown'}</td>
                            <td>{res.reservationTime ? new Date(res.reservationTime).toLocaleDateString() : 'N/A'}</td>
                            <td>{res.pickupTime ? new Date(res.pickupTime).toLocaleDateString() : 'N/A'}</td>
                            <td>{res.dueTime ? new Date(res.dueTime).toLocaleDateString() : 'N/A'}</td>
                            <td>{res.reservationStatus}</td>
                            <td>
                                {["reserved", "borrowed"].includes(res.reservationStatus) && (
                                    <button onClick={() => handleCancel(res.reservationId)} className="cancel-btn">
                                        Cancel
                                    </button>
                                )}
                            </td>
                        </tr>
                    ))}
                    </tbody>
                </table>
            )}

            {!loading && !error && reservations.length === 0 && (
                <div className="no-reservations">
                    You have no reservations.
                    <button onClick={() => navigate("/dashboard")}>Browse Books</button>
                </div>
            )}
        </div>
    );
};

export default Reservations;