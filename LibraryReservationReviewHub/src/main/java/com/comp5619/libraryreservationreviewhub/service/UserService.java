package com.comp5619.libraryreservationreviewhub.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import com.baomidou.mybatisplus.extension.service.IService;
import com.comp5619.libraryreservationreviewhub.model.dto.user.UserQueryRequest;
import com.comp5619.libraryreservationreviewhub.model.dto.user.UserUpdateRequest;
import com.comp5619.libraryreservationreviewhub.model.entity.User;
import com.comp5619.libraryreservationreviewhub.model.vo.LoginUserVO;
import com.comp5619.libraryreservationreviewhub.model.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;


import java.util.List;

/**
 * Users: accounts, sign-in and sessions
 */
public interface UserService extends IService<User> {

    /**
     * Registers a user
     *
     * @param userAccount   e-mail address
     * @param userPassword  password
     * @param checkPassword password, typed again
     * @param firstName     first_name
     * @param lastName      last_name
     * @param phone         phone
     * @return the new user's id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword, String firstName, String lastName, String phone);

    /**
     * Signs a user in
     *
     * @param userAccount  e-mail address
     * @param userPassword password
     * @param request
     * @return the user, without the password hash
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);


    /**
     * The signed-in user
     *
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * The signed-in user, without the password hash
     *
     * @param user
     * @return
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * A user, without the password hash
     *
     * @param user
     * @return
     */
    UserVO getUserVO(User user);

    /**
     * Users, without their password hashes
     *
     * @param userList
     * @return the users
     */
    List<UserVO> getUserVOList(List<User> userList);

    /**
     * Signs the user out
     *
     * @param request
     * @return
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * The query for a user search
     * @param userQueryRequest
     * @return
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);

    /**
     * Whether the user is an admin
     *
     * @param user
     * @return
     */
    boolean isAdmin(User user);

    String getUserNameById(Long userId);

    boolean updateUserProfile(UserUpdateRequest req, User user);

    /**
     * Admin edit of a user's name, e-mail, phone, role or status, with the same field checks as registration.
     * An admin can't change their own role or status, so an admin can't remove the last admin by themselves (two
     * admins demoting each other at the same moment is not prevented). An admin changing their own e-mail must
     * send their current password in {@code currentPassword}.
     *
     * @param req      the fields to change (null fields are left as they are)
     * @param operator the logged-in admin
     */
    boolean adminUpdateUser(UserUpdateRequest req, User operator);
}
