import api, { isSessionEnded } from './api';

// Admin calls on other users' accounts. Failures come back as { code, message } like every other response.
export const adminUserService = {
  listUsersPage: async ({ current = 1, pageSize = 20, keyword } = {}) => {
    try {
      const body = { current, pageSize, userAccount: keyword };
      const response = await api.post('/user/list/page/vo', body);
      return response.data;
    } catch (error) {
      if (isSessionEnded(error)) throw error;
      return { code: -1, message: error.response?.data?.message || 'Could not load the users', data: null };
    }
  },

  // one user, by id (admin: /user/get?id=…)
  getUserById: async (id) => {
    try {
      const response = await api.get('/user/get', { params: { id } });
      return response.data;
    } catch (error) {
      if (isSessionEnded(error)) throw error;
      throw new Error(error.response?.data?.message || 'Could not load the user');
    }
  },

  // A failure is reported, never retried elsewhere: /user/updateProfile edits the signed-in admin's OWN profile,
  // so a retry there would overwrite the admin's account with the other user's details.
  updateUser: async (payload) => {
    try {
      const response = await api.post('/user/update', payload);
      return response.data;
    } catch (error) {
      if (isSessionEnded(error)) throw error;
      return { code: -1, message: error.response?.data?.message || 'Could not update the user', data: null };
    }
  },
};
