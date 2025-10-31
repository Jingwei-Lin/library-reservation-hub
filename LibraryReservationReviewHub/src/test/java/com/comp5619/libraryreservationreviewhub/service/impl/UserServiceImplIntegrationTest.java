package com.comp5619.libraryreservationreviewhub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.comp5619.libraryreservationreviewhub.constant.UserConstant;
import com.comp5619.libraryreservationreviewhub.exception.BusinessException;
import com.comp5619.libraryreservationreviewhub.model.dto.user.UserQueryRequest;
import com.comp5619.libraryreservationreviewhub.model.dto.user.UserUpdateRequest;
import com.comp5619.libraryreservationreviewhub.model.entity.User;
import com.comp5619.libraryreservationreviewhub.model.vo.LoginUserVO;
import com.comp5619.libraryreservationreviewhub.model.vo.UserVO;
import com.comp5619.libraryreservationreviewhub.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 集成测试：UserServiceImpl
 * 自动加载 Spring Boot 上下文 + MyBatis-Plus + 事务支持。
 */
@SpringBootTest
@Transactional  // 每个测试自动回滚
class UserServiceImplIntegrationTest {

    @Autowired
    private UserService userService;

    /**
     *  注册 + 登录
     */
    @Test
    void testUserRegister_AndLogin_Success() {
        // 注册新用户
        long userId = userService.userRegister(
                "integration_test@example.com",
                "password123",
                "password123",
                "John",
                "Smith",
                "0400000000"
        );
        assertTrue(userId > 0);

        // 模拟登录请求
        MockHttpServletRequest request = new MockHttpServletRequest();
        LoginUserVO vo = userService.userLogin("integration_test@example.com", "password123", request);

        assertEquals("John Smith", vo.getUserName());
        assertEquals("user", vo.getUserRole());
    }

    /**
     * 重复邮箱注册
     */
    @Test
    void testUserRegister_AccountExists_ThrowsException() {
        userService.userRegister("dup@example.com", "password123", "password123", "A", "B", "0400");
        assertThrows(BusinessException.class, () ->
                userService.userRegister("dup@example.com", "password123", "password123", "A", "B", "0400"));
    }

    /**
     *  密码不匹配
     */
    @Test
    void testUserRegister_PasswordMismatch() {
        assertThrows(BusinessException.class, () ->
                userService.userRegister("user@example.com", "password123", "wrong", "T", "U", "0400"));
    }

    /**
     *  测试 getLoginUser
     */
    @Test
    void testGetLoginUser_Success() {
        // 注册 + 登录
        long id = userService.userRegister("login_user@example.com", "password123", "password123", "Jane", "Lee", "0400");
        MockHttpServletRequest request = new MockHttpServletRequest();
        LoginUserVO vo = userService.userLogin("login_user@example.com", "password123", request);

        // 从 session 获取当前登录用户
        User currentUser = userService.getLoginUser(request);
        assertEquals(id, currentUser.getId());
    }

    /**
     *  未登录访问 getLoginUser
     */
    @Test
    void testGetLoginUser_NotLoggedIn() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        assertThrows(BusinessException.class, () -> userService.getLoginUser(request));
    }

    /**
     *  测试 updateUserProfile
     */
    @Test
    void testUpdateUserProfile_Success() {
        long id = userService.userRegister("update_me@example.com", "password123", "password123", "Old", "Name", "0400");
        User user = userService.getById(id);

        UserUpdateRequest req = new UserUpdateRequest();
        req.setFirstName("New");
        req.setLastName("Name");
        req.setEmail("update_me@example.com");
        req.setPhone("0412345678");

        boolean result = userService.updateUserProfile(req, user);
        assertTrue(result);

        User updated = userService.getById(id);
        assertEquals("New", updated.getFirstName());
    }

    /**
     *  邮箱重复 updateUserProfile
     */
    @Test
    void testUpdateUserProfile_EmailExists() {
        // 创建两个用户
        userService.userRegister("user1@example.com", "password123", "password123", "A", "B", "1");
        long id2 = userService.userRegister("user2@example.com", "password123", "password123", "C", "D", "2");
        User user2 = userService.getById(id2);

        // 修改 user2 为 user1 的邮箱
        UserUpdateRequest req = new UserUpdateRequest();
        req.setFirstName("X");
        req.setLastName("Y");
        req.setEmail("user1@example.com");

        assertThrows(BusinessException.class, () -> userService.updateUserProfile(req, user2));
    }

    /**
     *  测试 getQueryWrapper
     */
    @Test
    void testGetQueryWrapper() {
        UserQueryRequest req = new UserQueryRequest();
        req.setId(1L);
        req.setUserAccount("abc@example.com");
        req.setFirstName("John");
        req.setLastName("Doe");
        req.setIsAdmin(true);
        req.setSortField("id");
        req.setSortOrder("ascend");

        QueryWrapper<User> wrapper = userService.getQueryWrapper(req);
        assertNotNull(wrapper);
    }

    /**
     *  测试 isAdmin()
     */
    @Test
    void testIsAdmin() {
        User admin = new User();
        admin.setIsAdmin(true);
        assertTrue(userService.isAdmin(admin));

        User normal = new User();
        normal.setIsAdmin(false);
        assertFalse(userService.isAdmin(normal));
    }

    /**
     *  测试 getUserVOList()
     */
    @Test
    void testGetUserVOList() {
        User u1 = new User();
        u1.setFirstName("Alice");
        u1.setLastName("Smith");
        u1.setIsAdmin(false);

        List<UserVO> list = userService.getUserVOList(List.of(u1));
        assertEquals("Alice Smith", list.get(0).getUserName());
    }

    /**
     *  测试 userLogout()
     */
    @Test
    void testUserLogout_Success() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(UserConstant.USER_LOGIN_STATE, new User());
        request.setSession(session);

        assertTrue(userService.userLogout(request));
    }

    @Test
    void testUserLogout_NotLoggedIn() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        assertThrows(BusinessException.class, () -> userService.userLogout(request));
    }
    /**
     * 用户不存在 - 登录时抛异常
     */
    @Test
    void testUserLogin_UserNotExist_ThrowsException() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        assertThrows(BusinessException.class, () ->
                        userService.userLogin("nonexist@example.com", "password123", request),
                "Expected BusinessException for non-existent user");
    }

    /**
     * 密码错误 - 登录时抛异常
     */
    @Test
    void testUserLogin_WrongPassword_ThrowsException() {
        // 先注册
        userService.userRegister("wrongpass@example.com", "password123", "password123", "X", "Y", "0400");

        // 用错误密码登录
        MockHttpServletRequest request = new MockHttpServletRequest();
        assertThrows(BusinessException.class, () ->
                        userService.userLogin("wrongpass@example.com", "WRONG", request),
                "Expected BusinessException for wrong password");
    }

    /**
     * 用户被封禁 - 登录时抛异常
     */
    @Test
    void testUserLogin_UserBanned_ThrowsException() {
        // 1. 注册用户
        long id = userService.userRegister("banned@example.com", "password123", "password123", "Ban", "User", "0400");

        // 2. 手动修改状态为封禁 (0)
        User user = userService.getById(id);
        user.setStatus(0);
        userService.updateById(user);

        // 3. 尝试登录，应抛 BusinessException.USER_BANNED
        MockHttpServletRequest request = new MockHttpServletRequest();
        assertThrows(BusinessException.class, () ->
                        userService.userLogin("banned@example.com", "password123", request),
                "Expected BusinessException for banned user");
    }
    /**
     * getLoginUser - 用户在数据库中被删除后应抛 NOT_LOGIN_ERROR
     */
    @Test
    void testGetLoginUser_UserDeleted_ThrowsException() {
        // 注册并登录
        long id = userService.userRegister("delete_me@example.com", "password123", "password123", "Del", "User", "0400");
        MockHttpServletRequest request = new MockHttpServletRequest();
        LoginUserVO vo = userService.userLogin("delete_me@example.com", "password123", request);

        // 从 Session 拿当前用户（带 id）
        User current = userService.getLoginUser(request);
        assertNotNull(current);

        // 删除该用户
        boolean removed = userService.removeById(id);
        assertTrue(removed);

        // 再次调用 getLoginUser() 应抛异常
        assertThrows(BusinessException.class, () -> userService.getLoginUser(request),
                "Expected NOT_LOGIN_ERROR because user was deleted from DB");
    }
    /**
     * getLoginUser - 用户被封禁后应抛 USER_BANNED
     */
    @Test
    void testGetLoginUser_UserBanned_ThrowsException() {
        // 注册并登录
        long id = userService.userRegister("ban_getlogin@example.com", "password123", "password123", "Ban", "Get", "0400");
        MockHttpServletRequest request = new MockHttpServletRequest();
        LoginUserVO vo = userService.userLogin("ban_getlogin@example.com", "password123", request);

        // 手动封禁该用户
        User banned = userService.getById(id);
        banned.setStatus(0);
        userService.updateById(banned);

        // 调用 getLoginUser() 应抛 USER_BANNED 异常
        assertThrows(BusinessException.class, () -> userService.getLoginUser(request),
                "Expected USER_BANNED because user.status = 0");
    }

}
