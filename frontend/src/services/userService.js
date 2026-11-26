import api from './api';

export const userService = {
    getUserProfile: async () => {
        const response = await api.get('/user/getProfile');
        return response.data;
    },

    updateUserProfile: async (profileData) => {
        const response = await api.put('/user/updateProfile', profileData);
        return response.data;
    },
};
