import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { bookService } from '../services/bookService';
import { reviewService } from '../services/reviewService';
import { adminUserService } from '../services/adminUserService';
import './AdminDashboard.css';

/**
 * 组件入参
 * - user: 当前登录用户信息（含 isAdmin / userRole）
 * - isAuthenticated: 是否已登录
 * - setIsAuthenticated: 可用于更新登录态（本组件暂未使用）
 */
const AdminDashboard = ({ user, isAuthenticated, setIsAuthenticated }) => {
  const navigate = useNavigate();

  /**
 * 进入页面时校验管理员权限
 * 非管理员或未登录，统一跳转到后台登录页
 */
useEffect(() => {
    const isAdmin = user?.isAdmin === true || user?.userRole === 'admin';
    if (!isAuthenticated || !isAdmin) {
      navigate('/admin/login');
    }
  }, [user, isAuthenticated, navigate]);

  // ====== Users ======
  // 用户管理：分页查询、状态标记（封禁/正常）、内联编辑保存
  const [users, setUsers] = useState([]);
  const [usersPage, setUsersPage] = useState(1);
  const [usersSize, setUsersSize] = useState(10);
  const [usersLoading, setUsersLoading] = useState(false);
  const [usersError, setUsersError] = useState('');
  const [usersSuccess, setUsersSuccess] = useState('');
  const [usersOriginal, setUsersOriginal] = useState([]);

  /**
   * 根据用户ID补充用户状态（status：0 正常 / 1 封禁）
   * 并保持一份原始数据 usersOriginal 以便比较是否被修改
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
   * 分页拉取用户列表，并调用 enrichUsersWithStatus 补充状态
   * 同步维护加载/错误/成功提示
   */
  const fetchUsers = async () => {
    try {
      setUsersLoading(true); setUsersError(''); setUsersSuccess('');
      const res = await adminUserService.listUsersPage({ current: usersPage, pageSize: usersSize });
      if (res && res.code === 0 && res.data) {
        const records = res.data.records || [];
        await enrichUsersWithStatus(records);
      } else {
        setUsersError(res?.msg || 'Failed to load users');
        setUsers([]); setUsersOriginal([]);
      }
    } catch (err) {
      setUsersError(err.message || 'Failed to load users');
      setUsersOriginal([]);
    } finally {
      setUsersLoading(false);
    }
  };

  /**
   * 本地修改某个用户的一列字段（不直接提交后端）
   */
  const handleUserFieldChange = (index, field, value) => {
    setUsers(prev => {
      const copy = [...prev];
      copy[index] = { ...copy[index], [field]: value };
      return copy;
    });
  };

  /**
   * 比较当前用户与原始数据，判断是否有改动（避免无意义保存）
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
   * 保存用户编辑：含二次确认、角色处理（admin）、错误/成功提示与数据刷新
   */
  const handleUserUpdate = async (u) => {
    if (isUserUnchanged(u)) {
      alert('No changes made');
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
      if (typeof u.userRole === 'string') {
        const role = u.userRole.toLowerCase();
        payload.isAdmin = role === 'admin';
      }
      const res = await adminUserService.updateUser(payload);
      if (res && res.code === 0) {
        setUsersSuccess('User updated successfully');
        await fetchUsers();
        setTimeout(() => setUsersSuccess(''), 2000);
      } else {
        setUsersError(res?.msg || 'Update failed');
      }
    } catch (err) {
      setUsersError(err.message || 'Update failed');
    }
  };

  /**
   * 切换封禁状态：status 0→1 封禁 / 1→0 解禁，二次确认与刷新
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
        setUsersError(res?.msg || 'Failed to update user status');
      }
    } catch (err) {
      setUsersError(err.message || 'Failed to update user status');
    }
  };

  // Independent effect: load user pagination
  useEffect(() => {
    if (isAuthenticated && (user?.isAdmin || user?.userRole === 'admin')) {
      fetchUsers();
    }
  }, [usersPage, usersSize]);

  // ====== Books ======
  // 图书管理：分页查询、内联编辑保存、删除与恢复
  const [books, setBooks] = useState([]);
  const [booksPage, setBooksPage] = useState(1);
  const [booksSize, setBooksSize] = useState(50);
  const [booksLoading, setBooksLoading] = useState(false);
  const [booksError, setBooksError] = useState('');
  const [booksSuccess, setBooksSuccess] = useState('');

  /**
   * 分页拉取图书列表，并维护加载/错误/成功提示
   */
  const fetchBooks = async () => {
    try {
      setBooksLoading(true); setBooksError(''); setBooksSuccess('');
      const res = await bookService.getAllBooks(booksPage, booksSize);
      if (res && res.code === 0 && res.data) {
        setBooks(res.data.records || []);
      } else {
        setBooksError(res?.msg || 'Failed to load books');
        setBooks([]);
      }
    } catch (err) {
      setBooksError(err.message || 'Failed to load books');
    } finally {
      setBooksLoading(false);
    }
  };

  /**
   * 本地修改图书的某个字段（不直接提交后端）
   */
  const handleBookFieldChange = (index, field, value) => {
    setBooks(prev => {
      const copy = [...prev];
      copy[index] = { ...copy[index], [field]: value };
      return copy;
    });
  };

  /**
   * 保存图书编辑（内联保存），成功后刷新列表
   */
  const handleBookUpdate = async (b) => {
    try {
      setBooksError(''); setBooksSuccess('');
      const res = await bookService.updateBook(b.bookId, { ...b, bookId: b.bookId });
      if (res && res.code === 0) {
        setBooksSuccess('Book updated successfully');
        await fetchBooks();
        setTimeout(() => setBooksSuccess(''), 2000);
      } else {
        setBooksError(res?.msg || 'Update failed');
      }
    } catch (err) {
      setBooksError(err.message || 'Update failed');
    }
  };

  /**
   * 删除图书（软删除），成功后刷新列表
   */
  const handleBookDelete = async (bookId) => {
    try {
      setBooksError(''); setBooksSuccess('');
      const res = await bookService.deleteBook(bookId);
      if (res && res.code === 0) {
        setBooksSuccess('Book deleted successfully');
        await fetchBooks();
        setTimeout(() => setBooksSuccess(''), 2000);
      } else {
        setBooksError(res?.msg || 'Delete failed');
      }
    } catch (err) {
      setBooksError(err.message || 'Delete failed');
    }
  };

  /**
   * 恢复已删除图书，成功后刷新列表
   */
  const handleBookRestore = async (bookId) => {
    try {
      setBooksError(''); setBooksSuccess('');
      const res = await bookService.restoreBook(bookId);
      if (res && res.code === 0) {
        setBooksSuccess('Book restored successfully');
        await fetchBooks();
        setTimeout(() => setBooksSuccess(''), 2000);
      } else {
        setBooksError(res?.msg || 'Restore failed');
      }
    } catch (err) {
      setBooksError(err.message || 'Restore failed');
    }
  };

  useEffect(() => {
    if (isAuthenticated && (user?.isAdmin || user?.userRole === 'admin')) {
      fetchBooks();
    }
  }, [booksPage, booksSize]);

  // ====== Reviews ======
  // 评论管理：分页查询、内联编辑评分与评论、保存与删除
  const [reviews, setReviews] = useState([]);
  const [reviewsOriginal, setReviewsOriginal] = useState([]);
  const [rvLoading, setRvLoading] = useState(false);
  const [rvError, setRvError] = useState('');
  const [rvSuccess, setRvSuccess] = useState('');
  const [rvPage, setRvPage] = useState(1);
  const [rvSize, setRvSize] = useState(20);

  /**
   * 分页拉取评论列表，保存原始数据用于修改比较
   */
  const fetchAllReviews = async () => {
    try {
      setRvLoading(true); setRvError(''); setRvSuccess('');
      const res = await reviewService.getAllReviews({ current: rvPage, pageSize: rvSize });
      if (res && res.code === 0 && res.data) {
        const records = res.data.records || [];
        setReviews(records);
        setReviewsOriginal(records);
      } else {
        setRvError(res?.msg || 'Failed to load reviews');
        setReviews([]); setReviewsOriginal([]);
      }
    } catch (err) {
      setRvError(err.message || 'Failed to load reviews');
      setReviews([]); setReviewsOriginal([]);
    } finally {
      setRvLoading(false);
    }
  };

  /**
   * 本地修改评论的某个字段（如评分/评论内容）
   */
  const handleReviewFieldChange = (index, field, value) => {
    setReviews(prev => {
      const copy = [...prev];
      copy[index] = { ...copy[index], [field]: value };
      return copy;
    });
  };

  /**
   * 比较当前评论与原始记录，判断是否有改动
   */
  const isReviewUnchanged = (rv) => {
    const orig = reviewsOriginal.find(o => o.reviewId === rv.reviewId);
    if (!orig) return false;
    const currRating = Number(rv.rating);
    const origRating = Number(orig.rating);
    const currComment = (rv.comment || '').trim();
    const origComment = (orig.comment || '').trim();
    return currRating === origRating && currComment === origComment;
  };

  /**
   * 保存评论编辑：二次确认、评分范围校验（1-5）、非空校验、成功后刷新
   */
  const handleReviewUpdate = async (rv) => {
    if (isReviewUnchanged(rv)) {
      alert('No changes made');
      return;
    }
    const confirmed = window.confirm('Are you sure you want to save the changes?');
    if (!confirmed) {
      await fetchAllReviews();
      setRvSuccess('');
      setRvError('');
      return;
    }

    const ratingNum = Number(rv.rating);
    if (!Number.isFinite(ratingNum) || ratingNum < 1 || ratingNum > 5) {
      setRvError('Rating must be between 1 and 5');
      return;
    }
    const commentTrim = (rv.comment || '').trim();
    if (!commentTrim) {
      setRvError('Comment cannot be empty');
      return;
    }

    try {
      setRvError(''); setRvSuccess('');
      const ok = await reviewService.updateReview({ reviewId: rv.reviewId, rating: ratingNum, comment: commentTrim });
      if (ok) {
        setRvSuccess('Review updated successfully');
        await fetchAllReviews();
        setTimeout(() => setRvSuccess(''), 2000);
      } else {
        setRvError('Update failed');
      }
    } catch (err) {
      setRvError(err.message || 'Update failed');
    }
  };

  /**
   * 删除评论：二次确认，成功后刷新
   */
  const handleReviewDelete = async (reviewId) => {
    const confirmed = window.confirm('Are you sure you want to delete this review?');
    if (!confirmed) return;
    try {
      setRvError(''); setRvSuccess('');
      const ok = await reviewService.deleteReview(reviewId);
      if (ok) {
        setRvSuccess('Review deleted successfully');
        await fetchAllReviews();
        setTimeout(() => setRvSuccess(''), 2000);
      } else {
        setRvError('Delete failed');
      }
    } catch (err) {
      setRvError(err.message || 'Delete failed');
    }
  };

  useEffect(() => {
    if (isAuthenticated && (user?.isAdmin || user?.userRole === 'admin')) {
      fetchAllReviews();
    }
  }, [rvPage, rvSize]); // 仅在评论分页变化时触发

  // ====== UI Rendering ======
  return (
    <div className="admin-container">
      <h2 className="page-title">Admin Panel</h2>

      {/* 用户管理区块 */}
      <div className="admin-section">
        <h3>Users</h3>
        <div className="admin-controls">
          <label>Page:</label>
          <input
            className="input input-sm"
            type="number"
            min={1}
            value={usersPage}
            onChange={(e) => setUsersPage(Math.max(1, Number(e.target.value)))}
          />
          <label>Size:</label>
          <input
            className="input input-sm"
            type="number"
            min={1}
            value={usersSize}
            onChange={(e) => setUsersSize(Math.max(1, Number(e.target.value)))}
          />
          <button className="btn btn-primary" onClick={fetchUsers}>Load Users</button>
        </div>
        {usersLoading && <div className="loading">Loading users...</div>}
        {usersError && <div className="alert alert-error">{usersError}</div>}
        {usersSuccess && <div className="alert alert-success">{usersSuccess}</div>}
        <table className="admin-table">
          <thead>
            <tr>
              <th>ID</th><th>Account</th><th>First Name</th><th>Last Name</th><th>Role</th><th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {users.map((u, idx) => (
              <tr key={u.id || idx}>
                <td>{u.id}</td>
                <td><input className="input" value={u.userAccount || ''} onChange={(e) => handleUserFieldChange(idx, 'userAccount', e.target.value)} /></td>
                <td><input className="input" value={u.firstName || ''} onChange={(e) => handleUserFieldChange(idx, 'firstName', e.target.value)} /></td>
                <td><input className="input" value={u.lastName || ''} onChange={(e) => handleUserFieldChange(idx, 'lastName', e.target.value)} /></td>
                <td><input className="input" value={u.userRole || ''} onChange={(e) => handleUserFieldChange(idx, 'userRole', e.target.value)} /></td>
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
      </div>

      {/* 评论管理区块 */}
      <div className="admin-section">
        <h3>Reviews</h3>
        <div className="admin-controls">
          <label>Page:</label>
          <input className="input input-sm" type="number" min={1} value={rvPage} onChange={(e) => setRvPage(Math.max(1, Number(e.target.value)))} />
          <label>Size:</label>
          <input className="input input-sm" type="number" min={1} value={rvSize} onChange={(e) => setRvSize(Math.max(1, Number(e.target.value)))} />
          <button className="btn btn-primary" onClick={fetchAllReviews}>Load Reviews</button>
        </div>
        {rvLoading && <div className="loading">Loading reviews...</div>}
        {rvError && <div className="alert alert-error">{rvError}</div>}
        {rvSuccess && <div className="alert alert-success">{rvSuccess}</div>}
        <table className="admin-table">
          <thead>
            <tr>
              <th>ID</th><th>Book</th><th>User</th><th>Rating</th><th>Comment</th><th>Date</th><th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {reviews.map((r, idx) => (
              <tr key={r.reviewId || idx}>
                <td>{r.reviewId}</td>
                <td>{r.bookId}</td>
                <td>{r.userId}</td>
                <td><input className="input" type="number" min={1} max={5} value={r.rating ?? 1} onChange={(e) => handleReviewFieldChange(idx, 'rating', Number(e.target.value))} /></td>
                <td><input className="input" value={r.comment || ''} onChange={(e) => handleReviewFieldChange(idx, 'comment', e.target.value)} /></td>
                <td>{r.reviewDate}</td>
                <td className="actions">
                  <button className="btn btn-primary" onClick={() => handleReviewUpdate(r)}>Save</button>
                  <button className="btn btn-danger" onClick={() => handleReviewDelete(r.reviewId)}>Delete</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* 图书管理区块 */}
      <div className="admin-section">
        <h3>Books</h3>
        <div className="admin-controls">
          <label>Page:</label>
          <input className="input input-sm" type="number" min={1} value={booksPage} onChange={(e) => setBooksPage(Math.max(1, Number(e.target.value)))} />
          <label>Size:</label>
          <input className="input input-sm" type="number" min={1} value={booksSize} onChange={(e) => setBooksSize(Math.max(1, Number(e.target.value)))} />
          <button className="btn btn-primary" onClick={fetchBooks}>Load Books</button>
        </div>
        {booksLoading && <div className="loading">Loading books...</div>}
        {booksError && <div className="alert alert-error">{booksError}</div>}
        {booksSuccess && <div className="alert alert-success">{booksSuccess}</div>}
        <table className="admin-table">
          <thead>
            <tr>
              <th>ID</th><th>Title</th><th>Author</th><th>Genre</th><th>ISBN</th><th>Available</th><th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {books.map((b, idx) => (
              <tr key={b.bookId || idx}>
                <td>{b.bookId}</td>
                <td><input className="input" value={b.title || ''} onChange={(e) => handleBookFieldChange(idx, 'title', e.target.value)} /></td>
                <td><input className="input" value={b.author || ''} onChange={(e) => handleBookFieldChange(idx, 'author', e.target.value)} /></td>
                <td><input className="input" value={b.genre || ''} onChange={(e) => handleBookFieldChange(idx, 'genre', e.target.value)} /></td>
                <td><input className="input" value={b.isbn || ''} onChange={(e) => handleBookFieldChange(idx, 'isbn', e.target.value)} /></td>
                <td><input className="checkbox" type="checkbox" checked={!!b.available} onChange={(e) => handleBookFieldChange(idx, 'available', e.target.checked)} /></td>
                <td className="actions">
                  <button className="btn btn-primary" onClick={() => handleBookUpdate(b)}>Save</button>
                  <button className="btn btn-danger" onClick={() => handleBookDelete(b.bookId)}>Delete</button>
                  <button className="btn btn-outline" onClick={() => handleBookRestore(b.bookId)}>Restore</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default AdminDashboard;
