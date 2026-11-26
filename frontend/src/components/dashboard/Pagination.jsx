import React from "react";
import { pageButtons } from "../../pagination";

/**
 * Previous, the page buttons ("..." for gaps), Next.
 * - onPage(page): go to that page
 */
const Pagination = ({ currentPage, totalPages, onPage }) => (
    <div className="pagination">
        <button onClick={() => onPage(Math.max(currentPage - 1, 1))} disabled={currentPage === 1}>
            Previous
        </button>
        {pageButtons(currentPage, totalPages).map((page, index) => (
            <React.Fragment key={index}>
                {typeof page === "number" ? (
                    <button
                        onClick={() => onPage(page)}
                        className={`pagination-number ${currentPage === page ? 'active' : ''}`}
                    >
                        {page}
                    </button>
                ) : (
                    <span className="pagination-ellipsis">...</span>
                )}
            </React.Fragment>
        ))}
        <button onClick={() => onPage(Math.min(currentPage + 1, totalPages))} disabled={currentPage === totalPages}>
            Next
        </button>
    </div>
);

export default Pagination;
