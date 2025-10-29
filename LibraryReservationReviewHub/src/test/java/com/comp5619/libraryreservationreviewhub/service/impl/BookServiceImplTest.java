package com.comp5619.libraryreservationreviewhub.service.impl;

import com.comp5619.libraryreservationreviewhub.model.vo.BookVO;
import com.comp5619.libraryreservationreviewhub.service.BookService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import lombok.extern.slf4j.Slf4j;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest
class BookServiceImplTest {


    @Resource
    private BookService bookService;
    

    @Test
    void testGetRandomBooks_DefaultLimit() {
        List<BookVO> books = bookService.getRandomBooks(10);
        log.info("随机取出的书籍: {}", books);
        Assertions.assertNotNull(books, "结果不应为 null");
        Assertions.assertFalse(books.isEmpty(), "随机书籍列表不应为空");
        Assertions.assertTrue(books.size() <= 10, "随机书籍数量应小于等于10");
    }
}