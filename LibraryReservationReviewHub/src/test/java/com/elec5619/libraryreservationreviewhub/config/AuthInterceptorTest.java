package com.elec5619.libraryreservationreviewhub.config;

import com.elec5619.libraryreservationreviewhub.annotation.AuthCheck;
import com.elec5619.libraryreservationreviewhub.exception.BusinessException;
import com.elec5619.libraryreservationreviewhub.exception.ErrorCode;
import com.elec5619.libraryreservationreviewhub.model.entity.User;
import com.elec5619.libraryreservationreviewhub.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthInterceptorTest {

    @SuppressWarnings("unused")
    static class Routes {
        @AuthCheck
        public void loginOnly() {
        }

        @AuthCheck(mustRole = "admin")
        public void adminOnly() {
        }

        @AuthCheck(mustRole = "Admin")
        public void mistypedRole() {
        }

        @AuthCheck(mustRole = "librarian")
        public void unknownRole() {
        }
    }

    private final UserService userService = mock(UserService.class);
    private final AuthInterceptor interceptor = new AuthInterceptor(userService);

    @Test
    void aRoleTheInterceptorDoesNotKnowRefusesEveryone() {
        for (boolean admin : new boolean[]{false, true}) {
            signedIn(admin);
            for (String route : new String[]{"mistypedRole", "unknownRole"}) {
                assertThatThrownBy(() -> call(route))
                        .isInstanceOf(BusinessException.class)
                        .extracting(e -> ((BusinessException) e).getCode())
                        .isEqualTo(ErrorCode.NO_AUTH_ERROR.getCode());
            }
        }
    }

    @Test
    void knownRolesWorkAsBefore() throws Exception {
        signedIn(false);
        assertThat(call("loginOnly")).isTrue();
        assertThatThrownBy(() -> call("adminOnly")).isInstanceOf(BusinessException.class);
        signedIn(true);
        assertThat(call("adminOnly")).isTrue();
    }

    private void signedIn(boolean admin) {
        User user = new User();
        user.setId(1L);
        user.setIsAdmin(admin);
        when(userService.getLoginUser(any())).thenReturn(user);
        when(userService.isAdmin(user)).thenReturn(admin);
    }

    private boolean call(String route) throws Exception {
        HandlerMethod handler = new HandlerMethod(new Routes(), Routes.class.getMethod(route));
        return interceptor.preHandle(new MockHttpServletRequest(), new MockHttpServletResponse(), handler);
    }
}
