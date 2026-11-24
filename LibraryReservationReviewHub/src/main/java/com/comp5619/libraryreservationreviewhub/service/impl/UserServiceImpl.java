package com.comp5619.libraryreservationreviewhub.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.comp5619.libraryreservationreviewhub.constant.UserConstant;
import com.comp5619.libraryreservationreviewhub.exception.BusinessException;
import com.comp5619.libraryreservationreviewhub.exception.ErrorCode;
import com.comp5619.libraryreservationreviewhub.exception.ThrowUtils;
import com.comp5619.libraryreservationreviewhub.mapper.UserMapper;
import com.comp5619.libraryreservationreviewhub.model.dto.user.UserQueryRequest;
import com.comp5619.libraryreservationreviewhub.model.dto.user.UserUpdateRequest;
import com.comp5619.libraryreservationreviewhub.model.entity.User;
import com.comp5619.libraryreservationreviewhub.model.vo.LoginUserVO;
import com.comp5619.libraryreservationreviewhub.model.vo.UserVO;
import com.comp5619.libraryreservationreviewhub.service.UserService;
import com.comp5619.libraryreservationreviewhub.utils.PasswordUtil;
import com.comp5619.libraryreservationreviewhub.utils.SqlInjectionUtil;
import com.comp5619.libraryreservationreviewhub.utils.ValidCheckUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @description 针对表【Users】的数据库操作Service实现
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    /**
     * 用户注册
     *
     * @param userAccount   用户账户 (email)
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @param firstName     first_name
     * @param lastName      last_name
     * @param phone         phone
     * @return
     */
    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword, String firstName, String lastName, String phone) {
        // 1. 校验参数
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
        // ========== 新增：输入安全检查（防 SQL 注入 / 格式校验） ==========
        try {
            // 检查常见注入特征（单引号、注释、关键字等）
            SqlInjectionUtil.checkSqlInjection(userAccount, "userAccount");
            SqlInjectionUtil.checkSqlInjection(userPassword, "userPassword");
            SqlInjectionUtil.checkSqlInjection(checkPassword, "checkPassword");
            SqlInjectionUtil.checkSqlInjection(firstName, "firstName");
            SqlInjectionUtil.checkSqlInjection(lastName, "lastName");
            SqlInjectionUtil.checkSqlInjection(phone, "phone");
        } catch (RuntimeException ex) {
            // 把工具类的异常转换为业务异常，保持统一错误类型
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Illegal input detected");
        }
        // 简单邮箱格式校验（最小严格性）
        if (!ValidCheckUtil.isValidEmail(userAccount)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Invalid email format");
        }
        // 电话格式校验（只允许 8-15 位数字，可按需调整）
        if (StrUtil.isNotBlank(phone) && !ValidCheckUtil.isValidPhone(phone)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Invalid phone format");
        }
        // 2. 检查用户账号 (email) 是否和数据库中已有的重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("email", userAccount);
        long count = this.baseMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Account already exists");
        }
        // 3. 密码一定要加密
        String encryptPassword = PasswordUtil.encode(userPassword);
        // 4. 插入数据到数据库中
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPhone(phone);
        user.setIsAdmin(false); // Default non-admin
//        user.setCreateTime(new Date()); // Set current time
        boolean saveResult = this.save(user);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Registration failed, database error");
        }
        return user.getId();
    }

    /**
     * 用户登录
     *
     * @param userAccount  用户账户 (email)
     * @param userPassword 用户密码
     * @param request
     * @return 脱敏后的用户信息
     */
    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. 校验
        if (StrUtil.hasBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Parameters cannot be blank");
        }

        
        // 2. 查询数据库中的用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("email", userAccount);
        User user = this.getOne(queryWrapper);
        // 不存在，抛异常
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "User does not exist");
        }
        // 3. 密码校验
        if (!PasswordUtil.matches(userPassword, user.getUserPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Incorrect password");
        }
        // user is banned
        if (user.getStatus().equals(0)) {
            throw new BusinessException(ErrorCode.USER_BANNED);
        }
        // an account still on the old MD5 hash gets a BCrypt hash now that the password is known
        if (PasswordUtil.needsUpgrade(userPassword, user.getUserPassword())) {
            User upgrade = new User();
            upgrade.setId(user.getId());
            upgrade.setUserPassword(PasswordUtil.encode(userPassword));
            this.updateById(upgrade);
            user.setUserPassword(upgrade.getUserPassword());
        }
        // 4. 获取脱敏后的用户信息
        LoginUserVO loginUserVO = getLoginUserVO(user);


        // Set properties for LoginUserVO
        BeanUtils.copyProperties(user, loginUserVO);
        loginUserVO.setUserName(user.getFirstName() + " " + user.getLastName());
        loginUserVO.setUserRole(user.getIsAdmin() ? "admin" : "user");

        // 5. 记录用户的登录态
        // a new session id at login, so an id set or seen before login (session fixation) is not signed in
        request.getSession();
        request.changeSessionId();
        request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, user);
        return loginUserVO;
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 判断是否已经登录
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 从数据库中查询
        Long userId = currentUser.getId();
        currentUser = this.getById(userId);
        // user is not exist
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // user is banned
        if (currentUser.getStatus().equals(0)) {
            throw new BusinessException(ErrorCode.USER_BANNED);
        }

        return currentUser;
    }

    /**
     * 获取脱敏类的用户信息
     *
     * @param user 用户
     * @return 脱敏后的用户信息
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
     * 获得脱敏后的用户信息
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
     * 获取脱敏后的用户列表
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
        // 判断是否已经登录
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute(UserConstant.USER_LOGIN_STATE) == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "Not logged in");
        }
        // 移除登录态: invalidate the whole session, so its id can't be used again
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
            queryWrapper.eq("email", req.getEmail());
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

        User update = new User();
        update.setId(target.getId());
        update.setFirstName(req.getFirstName());
        update.setLastName(req.getLastName());
        update.setPhone(req.getPhone());
        update.setIsAdmin(req.getIsAdmin());
        update.setStatus(req.getStatus());
        // the request calls the e-mail `email`; the entity calls it `userAccount`
        if (StrUtil.isNotBlank(req.getEmail()) && !req.getEmail().equals(target.getUserAccount())) {
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