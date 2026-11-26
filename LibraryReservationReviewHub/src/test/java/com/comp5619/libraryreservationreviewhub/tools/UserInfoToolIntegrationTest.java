package com.comp5619.libraryreservationreviewhub.tools;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.comp5619.libraryreservationreviewhub.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The AI assistant's user tool, called directly: guests, unknown users, a new user, a seeded user with reviews,
 * and the error path. The user always comes from the ToolContext the server sets.
 */
@SpringBootTest
@Transactional
class UserInfoToolIntegrationTest {

    @Autowired
    private UserInfoTool userInfoTool;

    @Autowired
    private UserService userService;

    @Test
    void aGuestIsNotLoggedIn() {
        JSONObject json = JSONUtil.parseObj(userInfoTool.getUserInfo(context(null)));

        assertThat(json.getStr("status")).isEqualTo("not_logged_in");
        assertThat(json.containsKey("userInfo")).isFalse();
    }

    @Test
    void anUnknownUserIsNotLoggedIn() {
        JSONObject json = JSONUtil.parseObj(userInfoTool.getUserInfo(context(99999L)));

        assertThat(json.getStr("status")).isEqualTo("not_logged_in");
        assertThat(json.containsKey("userInfo")).isFalse();
    }

    @Test
    void aNewUserHasTheirProfileAndNoHistory() {
        long id = userService.userRegister(
                "no_resv_user@example.com", "password123", "password123", "Tom", "Lee", "0412345678");

        JSONObject json = JSONUtil.parseObj(userInfoTool.getUserInfo(context(id)));

        assertThat(json.getStr("status")).isEqualTo("logged_in");
        JSONObject profile = json.getJSONObject("userInfo");
        assertThat(profile.getStr("firstName")).isEqualTo("Tom");
        assertThat(profile.getStr("lastName")).isEqualTo("Lee");
        assertThat(json.getJSONArray("borrowHistory")).isEmpty();
        assertThat(json.getJSONArray("reviews")).isEmpty();
    }

    @Test
    void aSeededUserHasTheirReviews() {
        // user 1 wrote reviews 1, 2, 4, 6 and 8 in the seed data
        JSONObject json = JSONUtil.parseObj(userInfoTool.getUserInfo(context(1L)));

        assertThat(json.getStr("status")).isEqualTo("logged_in");
        assertThat(json.getJSONObject("userInfo").getLong("id")).isEqualTo(1L);
        JSONArray reviews = json.getJSONArray("reviews");
        assertThat(reviews).hasSize(5);
        assertThat(reviews.toList(JSONObject.class)).extracting(r -> r.getStr("comment")).contains("Great book!");
    }

    @Test
    void aFailureIsReportedAsAnError() {
        // no services injected, so the lookup throws
        UserInfoTool brokenTool = new UserInfoTool();

        // checked as text: the tool puts the exception message into its JSON unescaped
        assertThat(brokenTool.getUserInfo(context(1L))).startsWith("{\"status\":\"error\"");
    }

    private static ToolContext context(Long userId) {
        return new ToolContext(UserInfoTool.toolContextFor(userId));
    }
}
