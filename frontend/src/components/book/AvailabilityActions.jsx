import React from "react";

/**
 * What the user can do about the book: Reserve Now when a copy is on the shelf; otherwise the waiting list (join,
 * or their place, the hold deadline once a copy is held for them, and cancel).
 */
const AvailabilityActions = ({
    book, hasActiveReservation, isOnWaitingList, isNotified, holdExpiresAt, waitingStatus,
    onReserve, onJoinWaiting, onCancelWaiting,
}) => {
    if (hasActiveReservation) {
        return <p className="reservation-status">You have already reserved this book.</p>;
    }
    if (book.available && book.quantity > 0) {
        return (
            <button
                className="reserve-button"
                onClick={onReserve}
                aria-label="Reserve book"
                disabled={hasActiveReservation}
            >
                Reserve Now
            </button>
        );
    }
    if (!isOnWaitingList) {
        return (
            <button
                className="join-waiting-button"
                onClick={onJoinWaiting}
                aria-label="Join waiting list"
            >
                Join Waiting List
            </button>
        );
    }
    return (
        <>
            <p className="waiting-status">
                You are on the waiting list. Status: {waitingStatus?.charAt(0).toUpperCase() + waitingStatus?.slice(1)}
            </p>
            {isNotified && holdExpiresAt && (
                <p className="hold-deadline">
                    A copy is held for you. Reserve by {new Date(holdExpiresAt).toLocaleString()}
                </p>
            )}
            {isNotified && (
                <button
                    className="reserve-button"
                    onClick={onReserve}
                    aria-label="Reserve book after notification"
                >
                    Reserve Now
                </button>
            )}
            <button
                className="cancel-waiting-button"
                onClick={onCancelWaiting}
                aria-label="Cancel waiting list entry"
            >
                Cancel Waiting
            </button>
        </>
    );
};

export default AvailabilityActions;
