import React, { useEffect, useState } from 'react';
import { adminUserService } from '../../services/adminUserService';
import { waitingListService } from '../../services/waitingListService';
import { includesQuery, pageCount } from './adminUtils';
import { AdminPager, NoResultsRow } from './AdminTableParts';

const WL_PAGE_SIZE = 10;

// every user, page by page
const fetchAllUsers = async () => {
  const PAGE_SIZE = 100; // large pages, fewer requests
  const first = await adminUserService.listUsersPage({ current: 1, pageSize: PAGE_SIZE });
  if (!(first && first.code === 0 && first.data)) {
    throw new Error(first?.message || 'Failed to load users');
  }
  const total = Number(first.data.total ?? 0);
  const pages = Math.max(1, Math.ceil(total / PAGE_SIZE));
  const all = [...(first.data.records || [])];
  for (let p = 2; p <= pages; p++) {
    try {
      const res = await adminUserService.listUsersPage({ current: p, pageSize: PAGE_SIZE });
      if (res && res.code === 0 && res.data) {
        all.push(...(res.data.records || []));
      }
    } catch (_) { /* no-op */ }
  }
  return all;
};

/**
 * The admin page's waiting list, read-only: every user's entries, no actions.
 * - canLoad: whether the signed-in user is an admin
 */
const WaitingListSection = ({ canLoad }) => {
  const [waitingLists, setWaitingLists] = useState([]);
  const [wlLoading, setWlLoading] = useState(false);
  const [wlError, setWlError] = useState('');
  const [wlPage, setWlPage] = useState(1);
  const [wlQuery, setWlQuery] = useState('');
  const filteredWaitingLists = React.useMemo(() => {
    const q = wlQuery.trim().toLowerCase();
    if (!q) return waitingLists;
    return waitingLists.filter(w => (
      includesQuery(w.waitId, q) ||
      includesQuery(w.userName ?? w.userId, q) ||
      includesQuery(w.bookTitle ?? w.bookId, q) ||
      includesQuery(w.status, q) ||
      includesQuery(w.joinDate, q) ||
      includesQuery(w.notificationDate, q)
    ));
  }, [waitingLists, wlQuery]);

  const fetchWaitingListsAll = async () => {
    try {
      setWlLoading(true); setWlError('');
      const usersAll = await fetchAllUsers();
      const rowsByUser = await Promise.all(usersAll.map(async (u) => {
        const displayName = `${u.firstName || ''}${u.firstName && u.lastName ? ' ' : ''}${u.lastName || ''}`.trim() || u.userAccount || String(u.id);
        try {
          const resp = await waitingListService.getByUserAdmin(u.id);
          if (resp && resp.code === 0 && Array.isArray(resp.data)) {
            return resp.data.map(item => ({
              ...item,
              userId: item.userId ?? u.id,
              userName: item.userName || displayName,
              bookTitle: item.bookTitle || item.bookId,
            }));
          }
        } catch (_) { /* no-op per user */ }
        return [];
      }));
      const rows = rowsByUser.flat();
      // by user id, then newest entry first (wait id descending)
      rows.sort((a, b) => {
        const ua = Number(a.userId ?? 0); const ub = Number(b.userId ?? 0);
        if (ua !== ub) return ua - ub;
        return Number(b.waitId ?? 0) - Number(a.waitId ?? 0);
      });
      setWaitingLists(rows);
      setWlPage(1);
    } catch (err) {
      setWlError(err.message || 'Failed to load waiting lists');
      setWaitingLists([]);
    } finally {
      setWlLoading(false);
    }
  };

  useEffect(() => {
    if (canLoad) {
      fetchWaitingListsAll();
    }
  }, []);

  const pages = pageCount(filteredWaitingLists.length, WL_PAGE_SIZE);
  const start = (wlPage - 1) * WL_PAGE_SIZE;
  const pageItems = filteredWaitingLists.slice(start, start + WL_PAGE_SIZE);
  return (
    <div className="admin-section">
      <h3>Waiting List</h3>
      <div className="admin-controls">
        <input
          className="input input-sm"
          placeholder="Search waiting list..."
          value={wlQuery}
          onChange={(e) => setWlQuery(e.target.value)}
        />
        <span>Page size: {WL_PAGE_SIZE}</span>
        <button className="btn btn-primary" onClick={fetchWaitingListsAll}>Refresh</button>
        <span>Page: {wlPage} / {pages}</span>
      </div>
      {wlLoading && <div className="loading">Loading waiting lists...</div>}
      {wlError && <div className="alert alert-error">{wlError}</div>}
      <table className="admin-table waitinglist-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>User</th>
            <th>Book</th>
            <th>Join Date</th>
            <th>Status</th>
            <th>Notified</th>
            <th>Notification Date</th>
          </tr>
        </thead>
        <tbody>
          {pageItems.length === 0 ? (
            <NoResultsRow colSpan={7} />
          ) : pageItems.map((w, idx) => (
            <tr key={w.waitId || `${w.userId}-${w.bookId}-${idx}`}>
              <td>{w.waitId}</td>
              <td>{w.userName || w.userId}</td>
              <td>{w.bookTitle || w.bookId}</td>
              <td>{w.joinDate || 'N/A'}</td>
              <td>{String(w.status || '').toLowerCase()}</td>
              <td>{w.isNotified ? 'Yes' : 'No'}</td>
              <td>{w.notificationDate || 'N/A'}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <AdminPager page={wlPage} pages={pages} onPage={setWlPage} />
    </div>
  );
};

export default WaitingListSection;
