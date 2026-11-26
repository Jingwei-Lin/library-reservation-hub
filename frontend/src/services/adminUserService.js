import api, { isSessionEnded } from './api';

export const adminUserService = {
  listUsersPage: async ({ current = 1, pageSize = 20, keyword } = {}) => {
    try {
      const body = { current, pageSize, userAccount: keyword };
      const response = await api.post('/user/list/page/vo', body);
      return response.data;
    } catch (error) {
      if (isSessionEnded(error)) throw error;
      // 如果接口未开启，返回一个结构化的失败信息供前端提示
      return { code: -1, msg: error.response?.data?.message || '用户分页接口不可用', data: null };
    }
  },

  // 根据ID获取用户（管理员接口：/user/get?id=xxx）
  getUserById: async (id) => {
    try {
      const response = await api.get('/user/get', { params: { id } });
      return response.data;
    } catch (error) {
      if (isSessionEnded(error)) throw error;
      throw new Error(error.response?.data?.message || '获取用户失败');
    }
  },

  updateUser: async (payload) => {
    try {
      const response = await api.post('/user/update', payload);
      return response.data;
    } catch (error) {
      if (isSessionEnded(error)) throw error;
      // 如果报错降级接口（/user/updateProfile）
      try {
        const resp2 = await api.put('/user/updateProfile', payload);
        return resp2.data;
      } catch (err2) {
        if (isSessionEnded(err2)) throw err2;
        return { code: -1, msg: err2.response?.data?.message || '更新用户失败', data: null };
      }
    }
  },
};
