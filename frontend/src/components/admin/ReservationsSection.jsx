import React, { useEffect, useState } from 'react';
import { reservationService } from '../../services/reservationService';
import { filterReservations, includesQuery, isDueToday, isOverdue, pageCount } from './adminUtils';
import { AdminPager, NoResultsRow } from './AdminTableParts';

const RES_PAGE_SIZE = 5;

/**
 * The admin page's reservations: confirm a borrow or a return, filtered by status, paged here.
 * - canLoad: whether the signed-in user is an admin
 */
const ReservationsSection = ({ canLoad }) => {
  const [reservations, setReservations] = useState([]);
  const [resLoading, setResLoading] = useState(false);
  const [resError, setResError] = useState('');
  const [resSuccess, setResSuccess] = useState('');
  const [resPage, setResPage] = useState(1);
  const [resFilter, setResFilter] = useState('all'); // borrowed | active | all
  const [resQuery, setResQuery] = useState('');
  const filteredReservations = React.useMemo(() => {
    const q = resQuery.trim().toLowerCase();
    if (!q) return reservations;
    return reservations.filter(r => (
      includesQuery(r.reservationId, q) ||
      includesQuery(r.userId, q) ||
      includesQuery(r.bookTitle ?? r.bookId, q) ||
      includesQuery(r.reservationStatus, q) ||
      includesQuery(r.pickupTime ? new Date(r.pickupTime).toLocaleDateString() : '', q) ||
      includesQuery(r.dueTime ? new Date(r.dueTime).toLocaleDateString() : '', q)
    ));
  }, [reservations, resQuery]);

  useEffect(() => {
    const maxPage = Math.max(1, Math.ceil(filteredReservations.length / RES_PAGE_SIZE));
    if (resPage > maxPage) {
      setResPage(maxPage);
    }
  }, [filteredReservations.length]);

  const fetchReservations = async () => {
    try {
      setResLoading(true); setResError(''); setResSuccess('');
      // Fetch a large page then paginate client-side
      const res = await reservationService.getAllReservations(1, 1000);
      if (res && res.code === 0 && res.data) {
        const records = res.data.records || [];
        const filtered = filterReservations(records, resFilter);
        setReservations(filtered);
        setResPage(1);
      } else {
        setResError(res?.message || 'Failed to load reservations');
        setReservations([]);
      }
    } catch (err) {
      setResError(err.message || 'Failed to load reservations');
      setReservations([]);
    } finally {
      setResLoading(false);
    }
  };

  const handleConfirmBorrow = async (reservationId) => {
    const confirmed = window.confirm("Confirm marking this reservation as borrowed?");
    if (!confirmed) return;
    try {
      setResError("");
      setResSuccess("");
      const resp = await reservationService.updateReservationStatus(reservationId, "borrowed");
      if (resp && resp.code === 0 && resp.data === true) {
        setResSuccess("Borrow confirmed successfully");
        await fetchReservations();
        setTimeout(() => setResSuccess(""), 2000);
      } else {
        setResError(resp?.message || "Failed to confirm borrow");
      }
    } catch (err) {
      setResError(err.message || "Failed to confirm borrow");
    }
  };

  const handleConfirmReturn = async (reservationId) => {
    const confirmed = window.confirm("Confirm marking this reservation as returned?");
    if (!confirmed) return;
    try {
      setResError("");
      setResSuccess("");
      const resp = await reservationService.updateReservationStatus(reservationId, "returned");
      if (resp && resp.code === 0 && resp.data === true) {
        setResSuccess("Return confirmed successfully");
        await fetchReservations();
        setTimeout(() => setResSuccess(""), 2000);
      } else {
        setResError(resp?.message || "Failed to confirm return");
      }
    } catch (err) {
      setResError(err.message || "Failed to confirm return");
    }
  };

  useEffect(() => {
    if (canLoad) {
      fetchReservations();
    }
  }, [resFilter]);

  const pages = pageCount(filteredReservations.length, RES_PAGE_SIZE);
  const start = (resPage - 1) * RES_PAGE_SIZE;
  const pageItems = filteredReservations.slice(start, start + RES_PAGE_SIZE);
  return (
    <div className="admin-section">
      <h3>Reservations</h3>
      <div className="admin-controls">
        <span>Page size: {RES_PAGE_SIZE}</span>
        <label>Filter:</label>
        <select className="input input-sm" value={resFilter} onChange={(e) => setResFilter(e.target.value)}>
          <option value="all">All statuses</option>
          <option value="borrowed">Borrowed/Overdue only</option>
          <option value="active">Include reserved</option>
        </select>
        <button className="btn btn-primary" onClick={fetchReservations}>Refresh</button>
        <input
          className="input input-sm"
          placeholder="Search reservations..."
          value={resQuery}
          onChange={(e) => setResQuery(e.target.value)}
        />
        <span>Page: {resPage} / {pages}</span>
      </div>
      {resLoading && <div className="loading">Loading reservations...</div>}
      {resError && <div className="alert alert-error">{resError}</div>}
      {resSuccess && <div className="alert alert-success">{resSuccess}</div>}
      <table className="admin-table reservations-table">
        <thead>
          <tr>
            <th>User ID</th>
            <th>Book Title</th>
            <th>Borrowed Date</th>
            <th>Due Date</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {pageItems.length === 0 ? (
            <NoResultsRow colSpan={5} />
          ) : pageItems.map((r, idx) => {
            const red = isDueToday(r.dueTime) || isOverdue(r.dueTime, r.reservationStatus);
            return (
              <tr key={r.reservationId || idx} className={red ? 'row-red' : ''}>
                <td>{r.userId}</td>
                <td>{r.bookTitle || r.bookId}</td>
                <td>{r.pickupTime ? new Date(r.pickupTime).toLocaleDateString() : 'N/A'}</td>
                <td>{r.dueTime ? new Date(r.dueTime).toLocaleDateString() : 'N/A'}</td>
                <td className="actions">
                  {['borrowed', 'overdue'].includes(r.reservationStatus) && (
                    <button className="btn btn-primary" onClick={() => handleConfirmReturn(r.reservationId)}>Confirm Return</button>
                  )}
                  {r.reservationStatus === 'reserved' && (
                    <button className="btn btn-outline" onClick={() => handleConfirmBorrow(r.reservationId)}>Confirm Borrow</button>
                  )}
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
      <AdminPager page={resPage} pages={pages} onPage={setResPage} />
    </div>
  );
};

export default ReservationsSection;
