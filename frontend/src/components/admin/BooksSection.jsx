import React, { useEffect, useState } from 'react';
import { bookService } from '../../services/bookService';
import NumericStepper from '../NumericStepper';
import { includesQuery, pageCount } from './adminUtils';
import { AdminPager, NoResultsRow } from './AdminTableParts';

const BOOKS_PAGE_SIZE = 10;

/**
 * The admin page's books: a page of books (deleted ones too), edit in place, delete and restore.
 * - canLoad: whether the signed-in user is an admin
 */
const BooksSection = ({ canLoad }) => {
  const [books, setBooks] = useState([]);
  const [booksPage, setBooksPage] = useState(1);
  const [booksTotal, setBooksTotal] = useState(0);
  const [booksLoading, setBooksLoading] = useState(false);
  const [booksError, setBooksError] = useState('');
  const [booksSuccess, setBooksSuccess] = useState('');
  const [booksOriginal, setBooksOriginal] = useState([]);
  const [booksQuery, setBooksQuery] = useState('');
  const filteredBooks = React.useMemo(() => {
    const q = booksQuery.trim().toLowerCase();
    if (!q) return books;
    return books.filter(b => (
      includesQuery(b.bookId, q) ||
      includesQuery(b.title, q) ||
      includesQuery(b.author, q) ||
      includesQuery(b.genre, q) ||
      includesQuery(b.quantity, q) ||
      includesQuery(b.available, q)
    ));
  }, [books, booksQuery]);

  /**
   * Loads a page of books, with the loading, error and success messages
   */
  const fetchBooks = async () => {
    try {
      setBooksLoading(true); setBooksError(''); setBooksSuccess('');
      const res = await bookService.getAllBooks(booksPage, BOOKS_PAGE_SIZE, true);
      if (res && res.code === 0 && res.data) {
        const records = res.data.records || [];
        setBooks(records);
        setBooksOriginal(records);
        setBooksTotal(res.data.total ?? 0);
      } else {
        setBooksError(res?.message || 'Failed to load books');
        setBooks([]); setBooksOriginal([]); setBooksTotal(0);
      }
    } catch (err) {
      setBooksError(err.message || 'Failed to load books'); setBooksOriginal([]); setBooksTotal(0);
    } finally {
      setBooksLoading(false);
    }
  };

  /**
   * Changes one field of a book here; nothing is saved until Save
   */
  const handleBookFieldChange = (bookId, field, value) => {
    setBooks(prev => {
      const copy = [...prev];
      const idx = copy.findIndex(it => it.bookId === bookId);
      if (idx >= 0) {
        const updated = { ...copy[idx], [field]: value };
        if (field === 'quantity') {
          const q = Number(value);
          const safeQ = Number.isFinite(q) ? q : 0;
          updated.available = safeQ > 0;
        }
        copy[idx] = updated;
      }
      return copy;
    });
  };

  const isBookUnchanged = (b) => {
    const orig = booksOriginal.find(o => o.bookId === b.bookId);
    if (!orig) return false;
    return (
      (b.title || '') === (orig.title || '') &&
      (b.author || '') === (orig.author || '') &&
      (b.genre || '') === (orig.genre || '') &&
      Number(b.quantity ?? 0) === Number(orig.quantity ?? 0) &&
      Boolean(b.available) === Boolean(orig.available)
    );
  };

  /**
   * Saves a book's edits, then reloads the list
   */
  const handleBookUpdate = async (b) => {
    if (isBookUnchanged(b)) {
      alert('No changes made');
      return;
    }
    const confirmed = window.confirm('Are you sure you want to save the changes?');
    if (!confirmed) {
      await fetchBooks();
      setBooksSuccess('');
      setBooksError('');
      return;
    }
    try {
      setBooksError(''); setBooksSuccess('');
      const res = await bookService.updateBook(b.bookId, { ...b, bookId: b.bookId });
      if (res && res.code === 0) {
        setBooksSuccess('Book updated successfully');
        await fetchBooks();
        setTimeout(() => setBooksSuccess(''), 2000);
      } else {
        setBooksError(res?.message || 'Update failed');
      }
    } catch (err) {
      setBooksError(err.message || 'Update failed');
    }
  };

  /**
   * Deletes a book (it is only marked deleted), then reloads the list
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
        setBooksError(res?.message || 'Delete failed');
      }
    } catch (err) {
      setBooksError(err.message || 'Delete failed');
    }
  };

  /**
   * Restores a deleted book, then reloads the list
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
        setBooksError(res?.message || 'Restore failed');
      }
    } catch (err) {
      setBooksError(err.message || 'Restore failed');
    }
  };

  useEffect(() => {
    if (canLoad) {
      fetchBooks();
    }
  }, [booksPage]);

  const pages = pageCount(booksTotal, BOOKS_PAGE_SIZE);
  return (
    <div className="admin-section">
      <h3>Books</h3>
      <div className="admin-controls">
        <input
          className="input input-sm"
          placeholder="Search books..."
          value={booksQuery}
          onChange={(e) => setBooksQuery(e.target.value)}
        />
        <span>Page size: {BOOKS_PAGE_SIZE}</span>
        <span>Page: {booksPage} / {pages}</span>
        <button className="btn btn-primary" onClick={fetchBooks}>Refresh</button>
      </div>
      {booksLoading && <div className="loading">Loading books...</div>}
      {booksError && <div className="alert alert-error">{booksError}</div>}
      {booksSuccess && <div className="alert alert-success">{booksSuccess}</div>}
      <table className="admin-table books-table">
        <thead>
          <tr>
            <th>ID</th><th>Title</th><th>Author</th><th>Genre</th><th>Quantity</th><th>Held</th><th>Available</th><th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {filteredBooks.length === 0 ? (
            <NoResultsRow colSpan={8} />
          ) : filteredBooks.map((b, idx) => (
            <tr key={b.bookId || idx}>
              <td>{b.bookId}{b.deleted && <div className="book-deleted-label">Deleted</div>}</td>
              <td><input className="input" value={b.title || ''} onChange={(e) => handleBookFieldChange(b.bookId, 'title', e.target.value)} /></td>
              <td><input className="input" value={b.author || ''} onChange={(e) => handleBookFieldChange(b.bookId, 'author', e.target.value)} /></td>
              <td><input className="input" value={b.genre || ''} onChange={(e) => handleBookFieldChange(b.bookId, 'genre', e.target.value)} /></td>
              <td><NumericStepper min={0} step={1} value={b.quantity ?? 0} onChange={(val) => handleBookFieldChange(b.bookId, 'quantity', val)} /></td>
              {/* held copies are set aside for notified waiting-list users; available follows the shelf count */}
              <td title="Copies set aside for notified waiting-list users">{b.heldQuantity ?? 0}</td>
              <td><input className="checkbox" type="checkbox" checked={!!b.available} disabled readOnly title="Worked out from Quantity" /></td>
              <td className="actions">
                <button className="btn btn-primary" onClick={() => handleBookUpdate(b)}>Save</button>
                {b.deleted
                  ? <button className="btn btn-outline" onClick={() => handleBookRestore(b.bookId)}>Restore</button>
                  : <button className="btn btn-danger" onClick={() => handleBookDelete(b.bookId)}>Delete</button>}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <AdminPager page={booksPage} pages={pages} onPage={setBooksPage} />
    </div>
  );
};

export default BooksSection;
