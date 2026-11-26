import React, { useEffect, useState } from 'react';
import { bookService } from '../../services/bookService';
import { reviewService } from '../../services/reviewService';
import { adminUserService } from '../../services/adminUserService';
import NumericStepper from '../NumericStepper';
import { includesQuery, pageCount } from './adminUtils';
import { AdminPager, NoResultsRow } from './AdminTableParts';

const RV_PAGE_SIZE = 10;

/**
 * The admin page's reviews: a page of reviews, edit the rating and comment in place, save and delete.
 * - canLoad: whether the signed-in user is an admin
 */
const ReviewsSection = ({ canLoad }) => {
  const [reviews, setReviews] = useState([]);
  const [reviewsOriginal, setReviewsOriginal] = useState([]);
  const [rvLoading, setRvLoading] = useState(false);
  const [rvError, setRvError] = useState('');
  const [rvSuccess, setRvSuccess] = useState('');
  const [rvPage, setRvPage] = useState(1);
  const [rvTotal, setRvTotal] = useState(0);
  const [reviewsQuery, setReviewsQuery] = useState('');
  const filteredReviews = React.useMemo(() => {
    const q = reviewsQuery.trim().toLowerCase();
    if (!q) return reviews;
    return reviews.filter(r => (
      includesQuery(r.reviewId, q) ||
      includesQuery(r.bookTitle ?? r.bookId, q) ||
      includesQuery(r.userName ?? r.userId, q) ||
      includesQuery(r.rating, q) ||
      includesQuery(r.comment, q) ||
      includesQuery(r.reviewDate, q)
    ));
  }, [reviews, reviewsQuery]);

  /**
   * Adds the book titles and user names, fetching each book and user once rather than once per review
   */
  const enrichReviewsWithNames = async (list) => {
    if (!Array.isArray(list) || list.length === 0) return list || [];
    const bookIds = Array.from(new Set(list.map(r => r.bookId).filter(Boolean)));
    const userIds = Array.from(new Set(list.map(r => r.userId).filter(Boolean)));
    const bookMap = new Map();
    const userMap = new Map();

    await Promise.all([
      Promise.all(bookIds.map(async (id) => {
        try {
          const resp = await bookService.getBookById(id);
          if (resp && resp.code === 0 && resp.data) {
            bookMap.set(id, resp.data.title);
          }
        } catch (_) { /* no-op */ }
      })),
      Promise.all(userIds.map(async (id) => {
        try {
          const res = await adminUserService.getUserById(id);
          if (res && res.code === 0 && res.data) {
            const fn = res.data.firstName || '';
            const ln = res.data.lastName || '';
            const name = `${fn}${fn && ln ? ' ' : ''}${ln}`.trim() || res.data.userAccount || String(id);
            userMap.set(id, name);
          }
        } catch (_) { /* no-op */ }
      }))
    ]);

    return list.map(r => ({
      ...r,
      bookTitle: r.bookTitle || bookMap.get(r.bookId),
      userName: r.userName || userMap.get(r.userId),
    }));
  };

  /**
   * Loads a page of reviews, keeping the originals to tell what was edited
   */
  const fetchAllReviews = async () => {
    try {
      setRvLoading(true); setRvError(''); setRvSuccess('');
      const res = await reviewService.getAllReviews({ current: rvPage, pageSize: RV_PAGE_SIZE });
      if (res && res.code === 0 && res.data) {
        const records = res.data.records || [];
        setRvTotal(res.data.total ?? 0);
        const enriched = await enrichReviewsWithNames(records);
        setReviews(enriched);
        setReviewsOriginal(enriched);
      } else {
        setRvError(res?.message || 'Failed to load reviews');
        setReviews([]); setReviewsOriginal([]); setRvTotal(0);
      }
    } catch (err) {
      setRvError(err.message || 'Failed to load reviews');
      setReviews([]); setReviewsOriginal([]); setRvTotal(0);
    } finally {
      setRvLoading(false);
    }
  };

  /**
   * Changes the rating or comment of a review here; nothing is saved until Save
   */
  const handleReviewFieldChange = (reviewId, field, value) => {
    setReviews(prev => {
      const copy = [...prev];
      const idx = copy.findIndex(it => it.reviewId === reviewId);
      if (idx >= 0) {
        copy[idx] = { ...copy[idx], [field]: value };
      }
      return copy;
    });
  };

  /**
   * Whether the review differs from the original
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
   * Saves a review's edits: asks first, checks the rating is 1 to 5 and the comment isn't empty, then reloads
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
   * Deletes a review after asking, then reloads
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
    if (canLoad) {
      fetchAllReviews();
    }
  }, [rvPage]); // only when the review page changes

  const pages = pageCount(rvTotal, RV_PAGE_SIZE);
  return (
    <div className="admin-section">
      <h3>Reviews</h3>
      <div className="admin-controls">
        <input
          className="input input-sm"
          placeholder="Search reviews..."
          value={reviewsQuery}
          onChange={(e) => setReviewsQuery(e.target.value)}
        />
        <span>Page size: {RV_PAGE_SIZE}</span>
        <span>Page: {rvPage} / {pages}</span>
        <button className="btn btn-primary" onClick={fetchAllReviews}>Refresh</button>
      </div>
      {rvLoading && <div className="loading">Loading reviews...</div>}
      {rvError && <div className="alert alert-error">{rvError}</div>}
      {rvSuccess && <div className="alert alert-success">{rvSuccess}</div>}
      <table className="admin-table reviews-table">
        <thead>
          <tr>
            <th>ID</th><th>Book</th><th>User</th><th>Rating</th><th>Comment</th><th>Date</th><th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {filteredReviews.length === 0 ? (
            <NoResultsRow colSpan={7} />
          ) : filteredReviews.map((r, idx) => (
            <tr key={r.reviewId || idx}>
              <td>{r.reviewId}</td>
              <td>{r.bookTitle || r.bookId}</td>
              <td>{r.userName || r.userId}</td>
              <td><NumericStepper min={1} max={5} step={1} value={r.rating ?? 1} onChange={(val) => handleReviewFieldChange(r.reviewId, 'rating', val)} /></td>
              <td><input className="input" value={r.comment || ''} onChange={(e) => handleReviewFieldChange(r.reviewId, 'comment', e.target.value)} /></td>
              <td>{r.reviewDate ? new Date(r.reviewDate).toLocaleString() : ''}</td>
              <td className="actions">
                <button className="btn btn-primary" onClick={() => handleReviewUpdate(r)}>Save</button>
                <button className="btn btn-danger" onClick={() => handleReviewDelete(r.reviewId)}>Delete</button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <AdminPager page={rvPage} pages={pages} onPage={setRvPage} />
    </div>
  );
};

export default ReviewsSection;
