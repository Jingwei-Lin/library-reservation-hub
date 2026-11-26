import React, { useEffect, useState } from 'react';
import { adminUserService } from '../../services/adminUserService';
import { includesQuery, pageCount } from './adminUtils';
import { AdminPager, NoResultsRow } from './AdminTableParts';

const USERS_PAGE_SIZE = 10;

/**
 * The admin page's users: a page of users, ban and unban, edit in place.
 * - user: the signed-in admin (their own e-mail change needs their current password)
 * - canLoad: whether the signed-in user is an admin
 */
const UsersSection = ({ user, canLoad }) => {
  const [users, setUsers] = useState([]);
  const [usersPage, setUsersPage] = useState(1);
  const [usersTotal, setUsersTotal] = useState(0);
  const [usersLoading, setUsersLoading] = useState(false);
  const [usersError, setUsersError] = useState('');
  const [usersSuccess, setUsersSuccess] = useState('');
  const [usersOriginal, setUsersOriginal] = useState([]);
  // asked for when the signed-in admin changes their own e-mail (the login), as on the profile page
  const [ownCurrentPassword, setOwnCurrentPassword] = useState('');
  const [usersQuery, setUsersQuery] = useState('');
  const filteredUsers = React.useMemo(() => {
    const q = usersQuery.trim().toLowerCase();
    if (!q) return users;
    return users.filter(u => (
      includesQuery(u.id, q) ||
      includesQuery(u.userAccount, q) ||
      includesQuery(u.firstName, q) ||
      includesQuery(u.lastName, q) ||
      includesQuery(u.userRole, q)
    ));
  }, [users, usersQuery]);

  /**
   * Adds each user's status (1 = active, 0 = banned),
   * and keeps a copy in usersOriginal to tell whether a row was edited
   */
  const enrichUsersWithStatus = async (list) => {
    if (!Array.isArray(list) || list.length === 0) {
      setUsers(list || []);
      setUsersOriginal(list || []);
      return;
    }
    try {
      const enriched = await Promise.all(list.map(async (u) => {
        try {
          const res = await adminUserService.getUserById(u.id);
          if (res && res.code === 0 && res.data) {
            return { ...u, status: res.data.status };
          }
        } catch (_) { /* no-op */ }
        return { ...u };
      }));
      setUsers(enriched);
      setUsersOriginal(enriched);
    } catch (e) {
      setUsers(list || []);
      setUsersOriginal(list || []);
    }
  };

  /**
   * Loads a page of users, then their status (enrichUsersWithStatus),
   * with the loading, error and success messages
   */
  const fetchUsers = async () => {
    try {
      setUsersLoading(true); setUsersError(''); setUsersSuccess('');
      const res = await adminUserService.listUsersPage({ current: usersPage, pageSize: USERS_PAGE_SIZE });
      if (res && res.code === 0 && res.data) {
        const records = res.data.records || [];
        setUsersTotal(res.data.total ?? 0);
        await enrichUsersWithStatus(records);
      } else {
        setUsersError(res?.message || 'Failed to load users');
        setUsers([]); setUsersOriginal([]); setUsersTotal(0);
      }
    } catch (err) {
      setUsersError(err.message || 'Failed to load users');
      setUsersOriginal([]); setUsersTotal(0);
    } finally {
      setUsersLoading(false);
    }
  };

  /**
   * Changes one field of a user here; nothing is saved until Save
   */
  const handleUserFieldChange = (id, field, value) => {
    setUsers(prev => {
      const copy = [...prev];
      const idx = copy.findIndex(it => it.id === id);
      if (idx >= 0) {
        copy[idx] = { ...copy[idx], [field]: value };
      }
      return copy;
    });
  };

  /**
   * Whether the user differs from the original, so an unchanged row isn't saved
   */
  const isUserUnchanged = (u) => {
    const orig = usersOriginal.find(o => o.id === u.id);
    if (!orig) return false;
    const role = String(u.userRole || '').toLowerCase();
    const origRole = String(orig.userRole || '').toLowerCase();
    return (
      (u.userAccount || '') === (orig.userAccount || '') &&
      (u.firstName || '') === (orig.firstName || '') &&
      (u.lastName || '') === (orig.lastName || '') &&
      role === origRole
    );
  };

  /**
   * Whether this row is the signed-in admin's own and its e-mail differs from the loaded one
   */
  const isOwnEmailChange = (u) => {
    if (user?.id == null || u.id !== user.id) return false;
    const orig = usersOriginal.find(o => o.id === u.id);
    return !!orig && (u.userAccount || '') !== (orig.userAccount || '');
  };

  /**
   * Saves a user's edits: asks first, handles the admin role, shows the result and reloads
   */
  const handleUserUpdate = async (u) => {
    if (isUserUnchanged(u)) {
      alert('No changes made');
      return;
    }
    const ownEmailChange = isOwnEmailChange(u);
    if (ownEmailChange && !ownCurrentPassword) {
      setUsersSuccess('');
      setUsersError('Enter your current password to change your own e-mail.');
      return;
    }
    const confirmed = window.confirm('Are you sure you want to save the changes?');
    if (!confirmed) {
      await fetchUsers();
      setUsersSuccess('');
      setUsersError('');
      return;
    }
    try {
      setUsersError(''); setUsersSuccess('');
      const payload = {
        id: u.id,
        firstName: u.firstName,
        lastName: u.lastName,
        email: u.userAccount,
      };
      if (ownEmailChange) {
        payload.currentPassword = ownCurrentPassword;
      }
      if (typeof u.userRole === 'string') {
        const role = u.userRole.toLowerCase();
        payload.isAdmin = role === 'admin';
      }
      const res = await adminUserService.updateUser(payload);
      if (res && res.code === 0) {
        setOwnCurrentPassword('');
        setUsersSuccess('User updated successfully');
        await fetchUsers();
        setTimeout(() => setUsersSuccess(''), 2000);
      } else {
        setUsersError(res?.message || 'Update failed');
      }
    } catch (err) {
      setUsersError(err.message || 'Update failed');
    }
  };

  /**
   * Bans or unbans (status 1 → 0 bans, 0 → 1 unbans), after asking, then reloads
   */
  const handleToggleBan = async (u) => {
    const nextStatus = u.status === 0 ? 1 : 0;
    const msg = nextStatus === 0 ? 'Are you sure you want to ban this user?' : 'Are you sure you want to unban this user?';
    const confirmed = window.confirm(msg);
    if (!confirmed) {
      await fetchUsers();
      setUsersSuccess('');
      setUsersError('');
      return;
    }
    try {
      setUsersError(''); setUsersSuccess('');
      const payload = {
        id: u.id,
        firstName: u.firstName,
        lastName: u.lastName,
        email: u.userAccount,
        status: nextStatus,
      };
      if (typeof u.userRole === 'string') {
        payload.isAdmin = u.userRole.toLowerCase() === 'admin';
      }
      const res = await adminUserService.updateUser(payload);
      if (res && res.code === 0) {
        setUsersSuccess(nextStatus === 0 ? 'User has been banned' : 'User unbanned');
        await fetchUsers();
        setTimeout(() => setUsersSuccess(''), 2000);
      } else {
        setUsersError(res?.message || 'Failed to update user status');
      }
    } catch (err) {
      setUsersError(err.message || 'Failed to update user status');
    }
  };

  // loads the page of users whenever the page changes
  useEffect(() => {
    if (canLoad) {
      fetchUsers();
    }
  }, [usersPage]);

  const pages = pageCount(usersTotal, USERS_PAGE_SIZE);
  return (
    <div className="admin-section">
      <h3>Users</h3>
      <div className="admin-controls">
        <input
          className="input input-sm"
          placeholder="Search users..."
          value={usersQuery}
          onChange={(e) => setUsersQuery(e.target.value)}
        />
        <span>Page size: {USERS_PAGE_SIZE}</span>
        <span>Page: {usersPage} / {pages}</span>
        <button className="btn btn-primary" onClick={fetchUsers}>Refresh</button>
      </div>
      {usersLoading && <div className="loading">Loading users...</div>}
      {usersError && <div className="alert alert-error">{usersError}</div>}
      {usersSuccess && <div className="alert alert-success">{usersSuccess}</div>}
      <table className="admin-table users-table">
        <thead>
          <tr>
            <th>ID</th><th>Account</th><th>First Name</th><th>Last Name</th><th>Role</th><th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {filteredUsers.length === 0 ? (
            <NoResultsRow colSpan={6} />
          ) : filteredUsers.map((u, idx) => (
            <tr key={u.id || idx}>
              <td>{u.id}</td>
              <td>
                <input className="input" value={u.userAccount || ''} onChange={(e) => handleUserFieldChange(u.id, 'userAccount', e.target.value)} />
                {isOwnEmailChange(u) && (
                  <input
                    className="input"
                    type="password"
                    placeholder="Your current password"
                    aria-label="Your current password"
                    autoComplete="current-password"
                    value={ownCurrentPassword}
                    onChange={(e) => setOwnCurrentPassword(e.target.value)}
                    style={{ marginTop: 6 }}
                  />
                )}
              </td>
              <td><input className="input" value={u.firstName || ''} onChange={(e) => handleUserFieldChange(u.id, 'firstName', e.target.value)} /></td>
              <td><input className="input" value={u.lastName || ''} onChange={(e) => handleUserFieldChange(u.id, 'lastName', e.target.value)} /></td>
              <td><input className="input" value={u.userRole || ''} onChange={(e) => handleUserFieldChange(u.id, 'userRole', e.target.value)} /></td>
              <td className="actions">
                <button className="btn btn-primary" onClick={() => handleUserUpdate(u)}>Save</button>
                <button className="btn btn-outline" onClick={() => handleToggleBan(u)}>
                  {u.status === 0 ? 'Unban' : 'Ban'}
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <AdminPager page={usersPage} pages={pages} onPage={setUsersPage} />
    </div>
  );
};

export default UsersSection;
