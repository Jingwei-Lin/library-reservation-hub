import React, { useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { loginUser } from '../services/authApi';
import { safeRedirect } from '../safeRedirect';

const AdminLogin = ({ setIsAuthenticated, setUser }) => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const [adminForm, setAdminForm] = useState({ userAccount: '', userPassword: '' });
  const [adminError, setAdminError] = useState('');
  const [adminLoading, setAdminLoading] = useState(false);

  const handleAdminChange = (e) => {
    const { name, value } = e.target;
    setAdminForm(prev => ({ ...prev, [name]: value }));
  };

  const handleAdminSubmit = async (e) => {
    e.preventDefault();
    setAdminError('');
    setAdminLoading(true);
    try {
      const data = await loginUser({ userAccount: adminForm.userAccount, userPassword: adminForm.userPassword });
      const isAdminFlag = data?.isAdmin === true || data?.userRole === 'admin';
      if (!isAdminFlag) {
        setAdminError('该账户非管理员');
        return;
      }
      sessionStorage.setItem('user', JSON.stringify(data));
      setUser(data);
      setIsAuthenticated(true);
      // back to the admin page the session ended on, if any
      navigate(safeRedirect(searchParams.get('redirect'), '/admin/dashboard'), { replace: true });
    } catch (err) {
      setAdminError(err.message || '登录失败');
    } finally {
      setAdminLoading(false);
    }
  };

  return (
    <div className="admin-login" style={{ maxWidth: 400, margin: '40px auto', padding: '20px' }}>
      <h2>Admin Login</h2>
      <form onSubmit={handleAdminSubmit}>
        <div>
          <label htmlFor="userAccount">Email:</label>
          <input
            type="email"
            id="userAccount"
            name="userAccount"
            value={adminForm.userAccount || ''}
            onChange={handleAdminChange}
            required
          />
        </div>
        <div>
          <label htmlFor="userPassword">Password:</label>
          <input
            type="password"
            id="userPassword"
            name="userPassword"
            value={adminForm.userPassword || ''}
            onChange={handleAdminChange}
            required
          />
        </div>
        {adminError && <div style={{ color: 'red' }}>{adminError}</div>}
        <button type="submit" disabled={adminLoading}>
          {adminLoading ? 'Logging in...' : 'Login as Admin'}
        </button>
      </form>
    </div>
  );
};

export default AdminLogin;