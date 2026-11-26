import React from "react";
import Portal from '../Portal';

/** Asks before a review is deleted */
const DeleteReviewDialog = ({ onCancel, onConfirm }) => (
    <Portal>
        <div className="popup-overlay">
            <div className="delete-popup">
                <div className="popup-header">
                    <h3>Delete Review</h3>
                    <button
                        className="close-popup"
                        onClick={onCancel}
                        aria-label="Close delete popup"
                    >
                        ×
                    </button>
                </div>
                <div className="popup-body">
                    <p>Are you sure you want to delete this review <strong>permanently</strong>?</p>
                </div>
                <div className="popup-actions">
                    <button
                        className="cancel-btn"
                        onClick={onCancel}
                    >
                        Cancel
                    </button>
                    <button
                        className="confirm-btn"
                        onClick={onConfirm}
                    >
                        Yes, Delete
                    </button>
                </div>
            </div>
        </div>
    </Portal>
);

export default DeleteReviewDialog;
