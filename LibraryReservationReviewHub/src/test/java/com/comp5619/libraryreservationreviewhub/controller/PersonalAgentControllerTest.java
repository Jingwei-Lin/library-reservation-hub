package com.comp5619.libraryreservationreviewhub.controller;

import com.comp5619.libraryreservationreviewhub.app.PersonalAgent;
import com.comp5619.libraryreservationreviewhub.constant.UserConstant;
import com.comp5619.libraryreservationreviewhub.exception.BusinessException;
import com.comp5619.libraryreservationreviewhub.exception.ErrorCode;
import com.comp5619.libraryreservationreviewhub.model.entity.User;
import com.comp5619.libraryreservationreviewhub.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import reactor.core.publisher.Flux;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * The chat passes the signed-in user's id to the agent (which hands it to the tools); guests keep the chat, and a
 * user who is banned (or deleted) since logging in is treated as a guest.
 */
@ExtendWith(MockitoExtension.class)
class PersonalAgentControllerTest {

    private static final String PATH = "/ai/personal_agent/chat/server_sent_event";

    private MockMvc mockMvc;

    @Mock
    private PersonalAgent personalAgent;

    @Mock
    private UserService userService;

    @InjectMocks
    private PersonalAgentController controller;

    private final String chatId = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        when(personalAgent.doChatByStream(anyString(), anyString(), anyString(), any())).thenReturn(Flux.just("ok"));
    }

    @Test
    void aSignedInUsersIdIsPassedToTheAgent() throws Exception {
        User user = sessionUser();
        when(userService.getLoginUser(any(HttpServletRequest.class))).thenReturn(user);

        mockMvc.perform(get(PATH).param("message", "my reservations").param("chatId", chatId).session(sessionWith(user)));

        verify(personalAgent).doChatByStream(eq("my reservations"), eq(chatId),
                eq("{\"status\":\"logged_in\",\"userId\":1,\"userName\":\"Jingwei\"}"), eq(1L));
    }

    @Test
    void aGuestStillChatsWithNoUserId() throws Exception {
        when(userService.getLoginUser(any(HttpServletRequest.class)))
                .thenThrow(new BusinessException(ErrorCode.NOT_LOGIN_ERROR));

        mockMvc.perform(get(PATH).param("message", "recommend a book").param("chatId", chatId));

        verify(personalAgent).doChatByStream(eq("recommend a book"), eq(chatId),
                eq("{\"status\":\"not_logged_in\"}"), isNull());
    }

    @Test
    void aUserBannedSinceLoginChatsAsAGuest() throws Exception {
        // the session still holds the user, but the database says banned
        when(userService.getLoginUser(any(HttpServletRequest.class)))
                .thenThrow(new BusinessException(ErrorCode.USER_BANNED));

        mockMvc.perform(get(PATH).param("message", "my reservations").param("chatId", chatId)
                .session(sessionWith(sessionUser())));

        verify(personalAgent).doChatByStream(eq("my reservations"), eq(chatId),
                eq("{\"status\":\"not_logged_in\"}"), isNull());
    }

    private static User sessionUser() {
        User user = new User();
        user.setId(1L);
        user.setFirstName("Jingwei");
        return user;
    }

    private static MockHttpSession sessionWith(User user) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(UserConstant.USER_LOGIN_STATE, user);
        return session;
    }
}
