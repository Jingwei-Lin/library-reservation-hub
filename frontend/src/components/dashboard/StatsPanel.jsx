import React from "react";

/**
 * The right-hand panel: the library's statistics (counted by the server) and Quick Actions.
 * - onTop10: shows the top 10 rated books
 */
const StatsPanel = ({ totalStats, onTop10 }) => (
    <div className="right-panel">
        <div className="stats-box">
            <h3>Library Statistics</h3>
            <p>Total books: {totalStats.totalBooks}</p>
            <p>Available books: {totalStats.totalAvailable}</p>
            <p>Categories: {totalStats.totalCategories}</p>
        </div>
        <div className="recommend-box">
            <h3>Quick Actions</h3>
            <ul>
                <li onClick={onTop10}>Top 10 Rated Books</li>
            </ul>
        </div>
    </div>
);

export default StatsPanel;
