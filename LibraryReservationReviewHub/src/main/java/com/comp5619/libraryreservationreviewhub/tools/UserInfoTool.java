package com.comp5619.libraryreservationreviewhub.tools;

import com.comp5619.libraryreservationreviewhub.model.entity.Book;
import com.comp5619.libraryreservationreviewhub.model.entity.User;
import com.comp5619.libraryreservationreviewhub.model.vo.LoginUserVO;
import com.comp5619.libraryreservationreviewhub.model.vo.ReservationVO;
import com.comp5619.libraryreservationreviewhub.model.vo.ReviewVO;
import com.comp5619.libraryreservationreviewhub.service.BookService;
import com.comp5619.libraryreservationreviewhub.service.ReservationService;
import com.comp5619.libraryreservationreviewhub.service.ReviewService;
import com.comp5619.libraryreservationreviewhub.service.UserService;
import cn.hutool.json.JSONUtil;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 用户信息工具 - 供 AI 调用，用于查询当前用户的基础资料、借阅历史和评论
 * 已去掉日志输出，使用 userId 作为传参
 */
@Component
public class UserInfoTool {

    @Autowired
    private UserService userService;

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private BookService bookService;

    /** ToolContext key for the signed-in user's id. */
    public static final String CONTEXT_USER_ID = "userId";
    /** ToolContext key set for guests: a tool that takes a ToolContext is refused an empty one by Spring AI. */
    private static final String CONTEXT_GUEST = "guest";

    /**
     * The ToolContext the chat passes to the tools: the signed-in user's id from the session, or a guest marker.
     */
    public static Map<String, Object> toolContextFor(Long userId) {
        return userId == null ? Map.of(CONTEXT_GUEST, true) : Map.of(CONTEXT_USER_ID, userId);
    }

    /**
     * 查询当前登录用户的详细信息
     * 如果未登录，返回 {"status":"not_logged_in"}
     * <p>
     * The user id comes from the ToolContext the server sets for the request, not from a tool argument, so the
     * model can't be asked to read another user's data. A ToolContext parameter is left out of the tool's JSON
     * schema, so the model sees a tool without parameters.
     */
    @Tool(
            name = "get_user_info",
            description = "Retrieve the currently logged-in user's profile, borrowing history, and reviews. " +
                    "Takes no arguments: it always returns the user who is signed in to this chat. " +
                    "If the user is not logged in, return {\"status\":\"not_logged_in\"}."
    )
    public String getUserInfo(ToolContext toolContext) {
        Object id = toolContext == null ? null : toolContext.getContext().get(CONTEXT_USER_ID);
        return getUserInfo(id instanceof Long userId ? userId : null);
    }

    private String getUserInfo(Long userId) {
        try {
            // 未登录或未传 userId
            if (userId == null) {
                Map<String, Object> result = new HashMap<>();
                result.put("status", "not_logged_in");
                return JSONUtil.toJsonPrettyStr(result);
            }

            // 查找用户
            User user = userService.getById(userId);
            if (user == null) {
                Map<String, Object> result = new HashMap<>();
                result.put("status", "not_logged_in");
                return JSONUtil.toJsonPrettyStr(result);
            }

            // 获取用户基础信息
            LoginUserVO userVO = userService.getLoginUserVO(user);

            // 获取借阅历史
            List<ReservationVO> reservations = reservationService.getReservationsByUser(userId);

            // 获取评论记录
            List<ReviewVO> reviews = reviewService.getReviewsByUserId(userId);

            // 获取关联的书籍信息
            Map<Long, Book> borrowedBooks = new HashMap<>();
            if (reservations != null) {
                for (ReservationVO r : reservations) {
                    Book b = bookService.getBookById(r.getBookId());
                    if (b != null) borrowedBooks.put(b.getBookId(), b);
                }
            }

            // 组织返回数据
            Map<String, Object> result = new HashMap<>();
            result.put("status", "logged_in");
            result.put("userInfo", userVO);
            result.put("borrowHistory", reservations);
            result.put("reviews", reviews);
            result.put("borrowedBooks", borrowedBooks.values());

            return JSONUtil.toJsonPrettyStr(result);

        } catch (Exception e) {
            return "{\"status\":\"error\",\"message\":\"" + e.getMessage() + "\"}";
        }
    }
}
