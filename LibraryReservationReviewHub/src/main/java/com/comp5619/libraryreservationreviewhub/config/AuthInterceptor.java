package com.comp5619.libraryreservationreviewhub.config;

import com.comp5619.libraryreservationreviewhub.annotation.AuthCheck;
import com.comp5619.libraryreservationreviewhub.constant.UserConstant;
import com.comp5619.libraryreservationreviewhub.exception.BusinessException;
import com.comp5619.libraryreservationreviewhub.exception.ErrorCode;
import com.comp5619.libraryreservationreviewhub.exception.ThrowUtils;
import com.comp5619.libraryreservationreviewhub.model.entity.User;
import com.comp5619.libraryreservationreviewhub.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Enforces {@link AuthCheck} before a handler runs.
 * <ul>
 *   <li>{@code @AuthCheck} with no role: the caller must be logged in (otherwise 40100 Not logged in).</li>
 *   <li>{@code @AuthCheck(mustRole = "admin")}: the caller must also be an admin (otherwise 40101 No permission).</li>
 *   <li>Any other role: refused for everyone (40101), so a typo can't open a route.</li>
 * </ul>
 * Errors go through GlobalExceptionHandler, so responses keep the usual HTTP 200 + {@code code} shape.
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final UserService userService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        AuthCheck authCheck = handlerMethod.getMethodAnnotation(AuthCheck.class);
        if (authCheck == null) {
            return true;
        }
        // throws NOT_LOGIN_ERROR when there is no session user, USER_BANNED when the user is banned
        User loginUser = userService.getLoginUser(request);
        String mustRole = authCheck.mustRole();
        if (UserConstant.ADMIN_ROLE.equals(mustRole)) {
            ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR);
        } else if (!mustRole.isEmpty()) {
            // a role this interceptor doesn't know (a typo such as "Admin", or a new role) refuses everyone
            // instead of letting any logged-in user through
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        return true;
    }
}
