package com.comp5619.libraryreservationreviewhub.tools;

import com.comp5619.libraryreservationreviewhub.mapper.BookMapper;
import com.comp5619.libraryreservationreviewhub.mapper.ReservationMapper;
import com.comp5619.libraryreservationreviewhub.mapper.ReviewMapper;
import com.comp5619.libraryreservationreviewhub.model.entity.Book;
import com.comp5619.libraryreservationreviewhub.model.entity.Reservation;
import com.comp5619.libraryreservationreviewhub.model.entity.Review;
import com.comp5619.libraryreservationreviewhub.model.entity.User;
import com.comp5619.libraryreservationreviewhub.model.vo.LoginUserVO;
import com.comp5619.libraryreservationreviewhub.model.vo.ReservationVO;
import com.comp5619.libraryreservationreviewhub.model.vo.ReviewVO;
import com.comp5619.libraryreservationreviewhub.service.BookService;
import com.comp5619.libraryreservationreviewhub.service.ReservationService;
import com.comp5619.libraryreservationreviewhub.service.ReviewService;
import com.comp5619.libraryreservationreviewhub.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import cn.hutool.json.JSONUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户信息工具 - 供 AI 调用，用于查询指定用户的完整信息（基础资料、借阅历史、评论）
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

    @Autowired
    private HttpServletRequest request;

    /**
     * 查询指定用户的详细信息
     * 如果未提供 userId，则自动获取当前登录用户。
     * @return 用户信息（JSON 字符串形式）
     */
    @Tool(name = "get_user_info",
            description = "Get the user's personal information, " +
                    "borrowing history and comment records based on the user ID")
    public String getUserInfo() {


        try {
            // 自动识别当前登录用户
            User currentUser = userService.getLoginUser(request);
            if (currentUser == null) {
                return "No user is currently logged in.";
            }
            Long userId = currentUser.getId();
            LoginUserVO userVO = userService.getLoginUserVO(currentUser);

            // 获取借阅历史
            List<ReservationVO> reservations = reservationService.getReservationsByUser(userId);

            // 获取评论记录
            List<ReviewVO> reviews = reviewService.getReviewsByUserId(userId);

            // 获取对应书籍信息
            Map<Long, Book> borrowedBooks = new HashMap<>();
            for (ReservationVO r : reservations) {
                Book b = bookService.getBookById(r.getBookId());
                if (b != null) borrowedBooks.put(b.getBookId(), b);
            }

            // 组织返回数据
            Map<String, Object> result = new HashMap<>();
            result.put("userInfo", userVO);
            result.put("borrowHistory", reservations);
            result.put("reviews", reviews);
            result.put("borrowedBooks", borrowedBooks.values());

            return JSONUtil.toJsonPrettyStr(result);

        } catch (Exception e) {
            return "Error fetching user info: " + e.getMessage();
        }
    }
}
