package com.elec5619.libraryreservationreviewhub.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.elec5619.libraryreservationreviewhub.constant.UserConstant;
import com.elec5619.libraryreservationreviewhub.exception.BusinessException;
import com.elec5619.libraryreservationreviewhub.exception.ErrorCode;
import com.elec5619.libraryreservationreviewhub.exception.ThrowUtils;
import com.elec5619.libraryreservationreviewhub.mapper.UserMapper;
import com.elec5619.libraryreservationreviewhub.model.dto.user.UserQueryRequest;
import com.elec5619.libraryreservationreviewhub.model.dto.user.UserUpdateRequest;
import com.elec5619.libraryreservationreviewhub.model.entity.User;
import com.elec5619.libraryreservationreviewhub.model.vo.LoginUserVO;
import com.elec5619.libraryreservationreviewhub.model.vo.UserVO;
import com.elec5619.libraryreservationreviewhub.service.LoginAttempts;
import com.elec5619.libraryreservationreviewhub.service.UserService;
import com.elec5619.libraryreservationreviewhub.utils.PasswordUtil;
import com.elec5619.libraryreservationreviewhub.utils.ValidCheckUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Users: accounts, sign-in and sessions
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    /** The one answer for an unknown e-mail and a wrong password, so neither tells whether an account exists */
    static final String SIGN_IN_REFUSED = "Incorrect e-mail or password";

    @Resource
    private LoginAttempts loginAttempts;

    /**
     * Registers a user
     *
     * @param userAccount   e-mail address
     * @param userPassword  password
     * @param checkPassword password, typed again
     * @param firstName     first_name
     * @param lastName      last_name
     * @param phone         phone
     * @return
     */
    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword, String firstName, String lastName, String phone) {
        // 1. Check the fields
        if (StrUtil.hasBlank(userAccount, userPassword, checkPassword, firstName, lastName,phone)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Parameters cannot be blank");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "User account is too short");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Password must be at least 8 characters");
        }
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Passwords do not match");
        }
        // a simple e-mail format check
        if (!ValidCheckUtil.isValidEmail(userAccount)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Invalid email format");
        }
        // phone: 8 to 15 digits
        if (StrUtil.isNotBlank(phone) && !ValidCheckUtil.isValidPhone(phone)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Invalid phone format");
        }
        // 2. The e-mail must not be registered already
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("email", userAccount);
        long count = this.baseMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Account already exists");
        }
        // 3. Hash the password
        String encryptPassword = PasswordUtil.encode(userPassword);
        // 4. Save the user
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPhone(phone);
        user.setIsAdmin(false); // Default non-admin
        boolean saveResult = this.save(user);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Registration failed, database error");
        }
        return user.getId();
    }

    /**
     * Signs a user in
     *
     * @param userAccount  e-mail address
     * @param userPassword password
     * @param request
     * @return the user, without the password hash
     */
    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. Check the fields
        if (StrUtil.hasBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Parameters cannot be blank");
        }

        // counted (or refused, past the limit) before anything is checked; a success below clears the count
        loginAttempts.begin(userAccount);

        // 2. Find the user
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("email", userAccount);
        User user = this.getOne(queryWrapper);
        // 3. Check the password; an unknown e-mail gets the same answer and was counted the same way
        if (user == null || !PasswordUtil.matches(userPassword, user.getUserPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, SIGN_IN_REFUSED);
        }
        loginAttempts.reset(userAccount);
        // user is banned
        if (user.getStatus().equals(0)) {
            throw new BusinessException(ErrorCode.USER_BANNED);
        }
        // an account still on the old MD5 hash gets a BCrypt hash now that the password is known
        if (PasswordUtil.needsUpgrade(userPassword, user.getUserPassword())) {
            String bcrypt = PasswordUtil.encode(userPassword);
            // only if the stored hash is still the old one: two first logins at once must not each write their
            // own hash, or the session holding the overwritten one would be signed out (getLoginUser compares)
            boolean upgraded = this.update(new UpdateWrapper<User>().set("password", bcrypt)
                    .eq("user_id", user.getId()).eq("password", user.getUserPassword()));
            if (upgraded) {
                user.setUserPassword(bcrypt);
            } else {
                user = this.getById(user.getId());
                ThrowUtils.throwIf(user == null || !PasswordUtil.matches(userPassword, user.getUserPassword()),
                        ErrorCode.PARAMS_ERROR, SIGN_IN_REFUSED);
            }
        }
        // 4. The user, without the password hash
        LoginUserVO loginUserVO = getLoginUserVO(user);


        // Set properties for LoginUserVO
        BeanUtils.copyProperties(user, loginUserVO);
        loginUserVO.setUserName(user.getFirstName() + " " + user.getLastName());
        loginUserVO.setUserRole(user.getIsAdmin() ? "admin" : "user");

        // 5. Remember the user in the session
        // a new session id at login, so an id set or seen before login (session fixation) is not signed in
        request.getSession();
        request.changeSessionId();
        request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, user);
        return loginUserVO;
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // reload the user from the database
        Long userId = currentUser.getId();
        String passwordAtLogin = currentUser.getUserPassword();
        currentUser = this.getById(userId);
        // user is not exist
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // the password has changed since this session logged in: the session is signed out, so a session someone
        // else holds stops working when the owner changes their password
        if (!Objects.equals(passwordAtLogin, currentUser.getUserPassword())) {
            request.getSession().removeAttribute(UserConstant.USER_LOGIN_STATE);
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // user is banned
        if (currentUser.getStatus().equals(0)) {
            throw new BusinessException(ErrorCode.USER_BANNED);
        }

        return currentUser;
    }

    /**
     * The signed-in user, without the password hash
     *
     * @param user the user
     * @return the view
     */
    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtil.copyProperties(user, loginUserVO);
        loginUserVO.setUserName(user.getFirstName() + " " + user.getLastName());
        loginUserVO.setUserRole(user.getIsAdmin() ? "admin" : "user");
        return loginUserVO;
    }

    /**
     * A user, without the password hash
     *
     * @param user
     * @return
     */
    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);
        userVO.setUserName(user.getFirstName() + " " + user.getLastName());
        userVO.setUserRole(user.getIsAdmin() ? "admin" : "user");
        return userVO;
    }

    /**
     * Users, without their password hashes
     *
     * @param userList
     * @return
     */
    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }

        return userList.stream()
                .map(this::getUserVO)
                .collect(Collectors.toList());
    }

    @Override
    public boolean userLogout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute(UserConstant.USER_LOGIN_STATE) == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "Not logged in");
        }
        // Invalidate the whole session, so its id can't be used again
        session.invalidate();
        return true;
    }

    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "empty params");
        }
        Long id = userQueryRequest.getId();
        String userAccount = userQueryRequest.getUserAccount(); // email
        String firstName = userQueryRequest.getFirstName();
        String lastName = userQueryRequest.getLastName();
        String phone = userQueryRequest.getPhone();
        Boolean isAdmin = userQueryRequest.getIsAdmin();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        // database column names (the entity's property names are not columns)
        queryWrapper.eq(ObjUtil.isNotNull(id), "user_id", id);
        queryWrapper.eq(ObjUtil.isNotNull(isAdmin), "is_admin", isAdmin);
        queryWrapper.like(StrUtil.isNotBlank(userAccount), "email", userAccount);
        queryWrapper.like(StrUtil.isNotBlank(phone), "phone", phone);
        // Search across firstName or lastName if either is provided
        if (StrUtil.isNotBlank(firstName) || StrUtil.isNotBlank(lastName)) {
            queryWrapper.and(wrapper ->
                    wrapper.like(StrUtil.isNotBlank(firstName), "first_name", firstName)
                            .or().like(StrUtil.isNotBlank(lastName), "last_name", lastName));
        }
        // sortField goes into ORDER BY as raw SQL, so it may only name a known column
        if (StrUtil.isNotEmpty(sortField)) {
            String column = USER_SORT_COLUMNS.get(sortField);
            ThrowUtils.throwIf(column == null, ErrorCode.PARAMS_ERROR, "Unsupported sort field");
            boolean ascending = "ascend".equalsIgnoreCase(sortOrder) || "asc".equalsIgnoreCase(sortOrder);
            queryWrapper.orderBy(true, ascending, column);
        }
        return queryWrapper;
    }

    /** Sortable fields of the admin user list: request name → column. */
    private static final java.util.Map<String, String> USER_SORT_COLUMNS = java.util.Map.of(
            "id", "user_id",
            "userAccount", "email",
            "firstName", "first_name",
            "lastName", "last_name",
            "phone", "phone",
            "isAdmin", "is_admin",
            "status", "status",
            "createTime", "create_time");

    @Override
    public boolean isAdmin(User user) {
        return user != null && Boolean.TRUE.equals(user.getIsAdmin());
    }

    @Override
    public String getUserNameById(Long userId) {
        User u = this.getById(userId);
        return u.getFirstName()+ " " + u.getLastName();
    }

    /**
     * Update user profile
     */
    @Override
    public boolean updateUserProfile(UserUpdateRequest req, User user) {
        // 1. Validate input
        ThrowUtils.throwIf(req == null, ErrorCode.PARAMS_ERROR, "Request cannot be null");
        ThrowUtils.throwIf(StrUtil.isBlank(req.getFirstName()), ErrorCode.PARAMS_ERROR, "First name cannot be blank");
        ThrowUtils.throwIf(StrUtil.isBlank(req.getLastName()), ErrorCode.PARAMS_ERROR, "Last name cannot be blank");
        ThrowUtils.throwIf(StrUtil.isBlank(req.getEmail()), ErrorCode.PARAMS_ERROR, "Email cannot be blank");
        // the same format rules as registration and the admin edit
        ThrowUtils.throwIf(!ValidCheckUtil.isValidEmail(req.getEmail()), ErrorCode.PARAMS_ERROR, "Invalid email format");
        ThrowUtils.throwIf(StrUtil.isNotBlank(req.getPhone()) && !ValidCheckUtil.isValidPhone(req.getPhone()),
                ErrorCode.PARAMS_ERROR, "Invalid phone format");
        if (StrUtil.isNotBlank(req.getPassword())) {
            ThrowUtils.throwIf(req.getPassword().length() < 8, ErrorCode.PARAMS_ERROR, "Password must be at least 8 characters");
            ThrowUtils.throwIf(!req.getPassword().equals(req.getConfirmPassword()), ErrorCode.PARAMS_ERROR, "Passwords do not match");
        }

        // Changing the e-mail (the login) or the password needs the current password, so a session left open
        // elsewhere can't take the account over
        boolean emailChanged = !req.getEmail().equals(user.getUserAccount());
        if (emailChanged || StrUtil.isNotBlank(req.getPassword())) {
            ThrowUtils.throwIf(!PasswordUtil.matches(req.getCurrentPassword(), user.getUserPassword()),
                    ErrorCode.PARAMS_ERROR, "Current password is incorrect");
        }

        // 2. Check if email is already in use by another user
        if (emailChanged) {
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            // not the user's own row: e-mails compare case-insensitively, so a case-only change would find it
            queryWrapper.eq("email", req.getEmail()).ne("user_id", user.getId());
            long count = this.baseMapper.selectCount(queryWrapper);
            ThrowUtils.throwIf(count > 0, ErrorCode.PARAMS_ERROR, "Email already in use");
        }

        // 3. Prepare updated user object
        User updatedUser = new User();
        updatedUser.setId(user.getId());
        updatedUser.setFirstName(req.getFirstName());
        updatedUser.setLastName(req.getLastName());
        updatedUser.setUserAccount(req.getEmail());
        updatedUser.setPhone(req.getPhone());
        if (StrUtil.isNotBlank(req.getPassword())) {
            updatedUser.setUserPassword(PasswordUtil.encode(req.getPassword()));
        }

        // 4. Update in database
        boolean updateResult = this.updateById(updatedUser);
        ThrowUtils.throwIf(!updateResult, ErrorCode.SYSTEM_ERROR, "Failed to update profile");

        return true;
    }

    @Override
    public boolean adminUpdateUser(UserUpdateRequest req, User operator) {
        ThrowUtils.throwIf(req == null || req.getId() == null, ErrorCode.PARAMS_ERROR);
        User target = this.getById(req.getId());
        ThrowUtils.throwIf(target == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(req.getStatus() != null && req.getStatus() != 0 && req.getStatus() != 1,
                ErrorCode.PARAMS_ERROR, "Status must be 0 (banned) or 1 (active)");

        // An admin can't change their own role or status. The caller is an admin, so this also means the
        // last admin can never be demoted or banned. Sending the current value unchanged is fine.
        if (target.getId().equals(operator.getId())) {
            ThrowUtils.throwIf(req.getIsAdmin() != null && !req.getIsAdmin().equals(target.getIsAdmin()),
                    ErrorCode.OPERATION_ERROR, "You can't change your own admin role");
            ThrowUtils.throwIf(req.getStatus() != null && !req.getStatus().equals(target.getStatus()),
                    ErrorCode.OPERATION_ERROR, "You can't change your own account status");
        }

        // the same rules as registration for the fields this request changes (null = leave as is)
        ThrowUtils.throwIf(req.getFirstName() != null && StrUtil.isBlank(req.getFirstName()), ErrorCode.PARAMS_ERROR,
                "First name cannot be blank");
        ThrowUtils.throwIf(req.getLastName() != null && StrUtil.isBlank(req.getLastName()), ErrorCode.PARAMS_ERROR,
                "Last name cannot be blank");
        ThrowUtils.throwIf(req.getEmail() != null && StrUtil.isBlank(req.getEmail()), ErrorCode.PARAMS_ERROR,
                "Email cannot be blank");
        ThrowUtils.throwIf(StrUtil.isNotBlank(req.getPhone()) && !ValidCheckUtil.isValidPhone(req.getPhone()),
                ErrorCode.PARAMS_ERROR, "Invalid phone format");

        User update = new User();
        update.setId(target.getId());
        update.setFirstName(req.getFirstName());
        update.setLastName(req.getLastName());
        update.setPhone(req.getPhone());
        update.setIsAdmin(req.getIsAdmin());
        update.setStatus(req.getStatus());
        // the request calls the e-mail `email`; the entity calls it `userAccount`
        if (StrUtil.isNotBlank(req.getEmail()) && !req.getEmail().equals(target.getUserAccount())) {
            ThrowUtils.throwIf(!ValidCheckUtil.isValidEmail(req.getEmail()), ErrorCode.PARAMS_ERROR,
                    "Invalid email format");
            // the e-mail is the login: an admin changing their own needs their current password, as on the
            // profile page, so an admin session left open can't be used to take the account over
            if (target.getId().equals(operator.getId())) {
                ThrowUtils.throwIf(!PasswordUtil.matches(req.getCurrentPassword(), target.getUserPassword()),
                        ErrorCode.PARAMS_ERROR, "Current password is incorrect");
            }
            QueryWrapper<User> sameEmail = new QueryWrapper<>();
            sameEmail.eq("email", req.getEmail()).ne("user_id", target.getId());
            ThrowUtils.throwIf(this.baseMapper.selectCount(sameEmail) > 0, ErrorCode.PARAMS_ERROR,
                    "Email already in use");
            update.setUserAccount(req.getEmail());
        }

        boolean result = this.updateById(update);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return true;
    }

}