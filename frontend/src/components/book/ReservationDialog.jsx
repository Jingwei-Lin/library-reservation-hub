import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { reservationService } from "../../services/reservationService";
import { isSessionEnded } from "../../services/api";
import Portal from '../Portal';
import { addDays, localDate, parseLocalDate } from "../../dates";

/** The latest due date for a pickup date (a Date or "YYYY-MM-DD"): 30 local days after it */
const maxDueFor = (pickupTime) => addDays(pickupTime, 30);

/**
 * The Reserve dialog: pickup and due dates, checked here and by the server. Shown while open; closing it forgets
 * what was typed.
 * - book, bookId: the book being reserved
 * - isAuthenticated: a guest is sent to sign in
 * - onClose: closes the dialog
 * - onReserved: the reservation was made (the dialog then closes and goes to My Reservations)
 */
const ReservationDialog = ({ book, bookId, isAuthenticated, onClose, onReserved }) => {
    const navigate = useNavigate();
    const [reservationForm, setReservationForm] = useState({
        pickupTime: "",
        dueTime: ""
    });
    const [reservationLoading, setReservationLoading] = useState(false);
    const [reservationError, setReservationError] = useState("");
    const [reservationSuccess, setReservationSuccess] = useState("");

    // the page behind the dialog doesn't scroll while it is open
    useEffect(() => {
        document.body.classList.add('popup-open');
        return () => {
            document.body.classList.remove('popup-open');
        };
    }, []);

    const handleReservationSubmit = async (e) => {
        e.preventDefault();
        if (!isAuthenticated) {
            navigate("/login");
            return;
        }

        setReservationLoading(true);
        setReservationError("");
        setReservationSuccess("");

        try {
            // local midnight, like `today` below
            const pickupDate = parseLocalDate(reservationForm.pickupTime);
            const dueDate = parseLocalDate(reservationForm.dueTime);
            const today = new Date();
            today.setHours(0, 0, 0, 0);

            if (pickupDate < today) {
                throw new Error("Pickup date cannot be in the past");
            }
            if (dueDate <= pickupDate) {
                throw new Error("Due date must be after pickup date");
            }

            // the longest loan: 30 days from pickup
            const maxDueDate = new Date(pickupDate);
            maxDueDate.setDate(pickupDate.getDate() + 30);

            if (dueDate > maxDueDate) {
                throw new Error("Reservation period cannot exceed 30 days");
            }

            const response = await reservationService.createReservation(bookId, {
                pickupTime: reservationForm.pickupTime,
                dueTime: reservationForm.dueTime
            });

            if (response && response.code === 0) {
                setReservationSuccess("Reservation created successfully!");
                onReserved();
                setTimeout(() => {
                    onClose();
                    navigate("/reservations");
                }, 2000);
            } else if (response && response.code === 50001) {
                setReservationError(response.message);
            } else {
                setReservationError(response?.message || "Failed to create reservation");
            }
        } catch (err) {
            if (isSessionEnded(err)) {
                onClose();   // the app shows the notice; the page stays, as a guest
            } else {
                setReservationError(err.message || "Failed to create reservation");
            }
        } finally {
            setReservationLoading(false);
        }
    };

    const handleReservationFormChange = (e) => {
        setReservationForm({ ...reservationForm, [e.target.name]: e.target.value });
    };

    return (
        <Portal>
            <div className="popup-overlay">
                <div className="reservation-popup">
                    <div className="popup-header">
                        <h3>Reserve "{book?.title}"</h3>
                        <button
                            className="close-popup"
                            onClick={onClose}
                            aria-label="Close reservation popup"
                        >
                            ×
                        </button>
                    </div>
                    <form onSubmit={handleReservationSubmit} className="reservation-form">
                        <div className="form-group">
                            <label htmlFor="pickupTime">Pickup Date:</label>
                            <input
                                type="date"
                                id="pickupTime"
                                name="pickupTime"
                                value={reservationForm.pickupTime}
                                onChange={handleReservationFormChange}
                                required
                                min={localDate()}
                                max={addDays(new Date(), 7)}
                            />
                            <small className="date-hint">Select a date within the next 7 days</small>
                        </div>
                        <div className="form-group">
                            <label htmlFor="dueTime">Due Date:</label>
                            <input
                                type="date"
                                id="dueTime"
                                name="dueTime"
                                value={reservationForm.dueTime}
                                onChange={handleReservationFormChange}
                                required
                                min={reservationForm.pickupTime || localDate()}
                                max={reservationForm.pickupTime ? maxDueFor(reservationForm.pickupTime) : maxDueFor(new Date())}
                            />
                            <small className="date-hint">
                                {reservationForm.pickupTime
                                    ? `Due date must be within 30 days of pickup (max: ${maxDueFor(reservationForm.pickupTime)})`
                                    : "Select pickup date first"
                                }
                            </small>
                        </div>
                        {reservationError && <p className="error">{reservationError}</p>}
                        {reservationSuccess && <p className="success">{reservationSuccess}</p>}
                        <div className="popup-actions">
                            <button
                                type="button"
                                onClick={onClose}
                                className="cancel-btn"
                            >
                                Cancel
                            </button>
                            <button
                                type="submit"
                                disabled={reservationLoading}
                                className="confirm-btn"
                            >
                                {reservationLoading ? "Reserving..." : "Confirm Reservation"}
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        </Portal>
    );
};

export default ReservationDialog;
