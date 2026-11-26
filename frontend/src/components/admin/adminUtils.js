// Helpers the admin page's sections share.

/** Case-insensitive substring match, for the search box above each table */
export const includesQuery = (value, q) => String(value ?? '').toLowerCase().includes(q);

/** How many pages a count fills (at least one) */
export const pageCount = (count, size) => Math.max(1, Math.ceil((count || 0) / size));

/** Whether the signed-in user may load the admin data */
export const isAdminUser = (user, isAuthenticated) => isAuthenticated && (user?.isAdmin || user?.userRole === 'admin');

/** A reservation's status filter: borrowed (and overdue), active (reserved too), or all */
export const filterReservations = (records, filter) => {
  switch (filter) {
    case 'borrowed':
      return records.filter(r => ['borrowed', 'overdue'].includes(r.reservationStatus));
    case 'active':
      return records.filter(r => ['reserved', 'borrowed', 'overdue'].includes(r.reservationStatus));
    default:
      return records;
  }
};

export const isDueToday = (dueTime) => {
  if (!dueTime) return false;
  const d = new Date(dueTime);
  const now = new Date();
  return d.getFullYear() === now.getFullYear() && d.getMonth() === now.getMonth() && d.getDate() === now.getDate();
};

export const isOverdue = (dueTime, status) => {
  if (!dueTime) return false;
  const d = new Date(dueTime);
  return d.getTime() < Date.now() && status !== 'returned' && status !== 'cancelled';
};
