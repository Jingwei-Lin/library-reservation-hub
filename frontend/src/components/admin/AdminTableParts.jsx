// Small pieces of markup the admin page's tables share.

/** The Prev / Next row under a table */
export const AdminPager = ({ page, pages, onPage }) => (
  <div className="admin-pagination-row" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '8px' }}>
    <button className="btn btn-secondary" onClick={() => onPage(Math.max(1, page - 1))} disabled={page <= 1}>Prev</button>
    <button className="btn btn-secondary" onClick={() => onPage(page + 1)} disabled={page >= pages}>Next</button>
  </div>
);

/** The row a table shows when nothing matches the search */
export const NoResultsRow = ({ colSpan }) => (
  <tr>
    <td colSpan={colSpan} style={{ textAlign: 'center', color: '#666' }}>No matched results</td>
  </tr>
);
