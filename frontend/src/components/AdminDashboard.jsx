import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import './AdminDashboard.css';
import UsersSection from './admin/UsersSection';
import ReviewsSection from './admin/ReviewsSection';
import WaitingListSection from './admin/WaitingListSection';
import BooksSection from './admin/BooksSection';
import ReservationsSection from './admin/ReservationsSection';
import { isAdminUser } from './admin/adminUtils';

/**
 * The admin page: one section per table, each loading its own data.
 * Props:
 * - user: the signed-in user (with isAdmin / userRole)
 * - isAuthenticated: whether someone is signed in
 * - onSignOut: signs out and goes to the given page
 */
const AdminDashboard = ({ user, isAuthenticated, onSignOut }) => {
  const navigate = useNavigate();

  /**
   * On entry, checks that the user is an admin;
   * anyone else goes to the admin sign-in page
   */
  useEffect(() => {
    const isAdmin = user?.isAdmin === true || user?.userRole === 'admin';
    if (!isAuthenticated || !isAdmin) {
      // back here after signing in (an ended session has already gone to the same place)
      navigate('/admin/login?redirect=%2Fadmin%2Fdashboard', { replace: true });
    }
  }, [user, isAuthenticated, navigate]);

  // the sections load only for an admin (anyone else is on their way to the sign-in page)
  const canLoad = isAdminUser(user, isAuthenticated);

  return (
    <div className="admin-container">
      <div className="admin-title-row">
        <h2 className="page-title">Admin Panel</h2>
        <button className="btn btn-secondary admin-sign-out" onClick={() => onSignOut('/admin/login')}>Sign out</button>
      </div>

      <UsersSection user={user} canLoad={canLoad} />
      <ReviewsSection canLoad={canLoad} />
      <WaitingListSection canLoad={canLoad} />
      <BooksSection canLoad={canLoad} />
      <ReservationsSection canLoad={canLoad} />
    </div>
  );
};

export default AdminDashboard;
