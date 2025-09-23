package com.comp5619.libraryreservationreviewhub.mapper;

import com.comp5619.libraryreservationreviewhub.model.entity.Book;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface BookMapper {

    /* ================= 基础 CRUD ================= */

    @Select("SELECT book_id as bookId, title, author, genre, quantity, available, path FROM book WHERE book_id = #{id}")
    Book selectById(@Param("id") Long id);

    /**
     * 若无 ISBN，可用 (title, author, genre) 作为“近似唯一”约束做去重检查
     * 建议在数据库加唯一索引：UNIQUE idx_book_tag (title, author, genre)
     */
    @Select("""
        SELECT COUNT(1) 
        FROM book 
        WHERE title = #{title} AND author = #{author} AND genre = #{genre}
        """)
    long countByTag(@Param("title") String title,
                    @Param("author") String author,
                    @Param("genre") String genre);

    @Insert("""
        INSERT INTO book (title, author, genre, quantity, available, path)
        VALUES (#{title}, #{author}, #{genre}, #{quantity}, #{available}, #{path})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "bookId", keyColumn = "book_id")
    int insertBook(Book b);

    /** 动态更新：仅更新非空字段 */
    @Update("""
        <script>
        UPDATE book
        <set>
          <if test="title    != null">title    = #{title},</if>
          <if test="author   != null">author   = #{author},</if>
          <if test="genre    != null">genre    = #{genre},</if>
          <if test="quantity != null">quantity = #{quantity},</if>
          <if test="available!= null">available= #{available},</if>
          <if test="path!= null">path= #{path},</if>
        </set>
        WHERE book_id = #{bookId}
        </script>
        """)
    int updateBook(Book b);

    /** 逻辑删除：将 available 标记为 false（仅当当前为 true 时才更新） */
    @Update("UPDATE book SET available = FALSE WHERE book_id = #{id} AND available = TRUE")
    int deleteById(@Param("id") Long id);

    @Update("UPDATE book SET available = TRUE WHERE book_id = #{id}")
    int restoreById(@Param("id") Long id);

    /* ================== 分页 / 搜索 ================== */

    @Select("""
        <script>
        SELECT COUNT(1)
        FROM book
        <where>
          <if test="kw != null and kw != ''">
            (title LIKE CONCAT('%', #{kw}, '%')
             OR author LIKE CONCAT('%', #{kw}, '%')
             OR genre  LIKE CONCAT('%', #{kw}, '%'))
          </if>
          <if test="available != null">
            AND available = #{available}
          </if>
        </where>
        </script>
        """)
    long countByKeyword(@Param("kw") String keyword,
                        @Param("available") Boolean available);

    /**
     * 注意：orderBy 参数用 ${} 直拼，必须在 Service 做字段白名单校验，防止 SQL 注入
     * 例：允许的字段集 {"title","author","genre","quantity","available"}
     */
    @Select("""
        <script>
        SELECT book_id as bookId, title, author, genre, quantity, available, path
        FROM book
        <where>
          <if test="kw != null and kw != ''">
            (title LIKE CONCAT('%', #{kw}, '%')
             OR author LIKE CONCAT('%', #{kw}, '%')
             OR genre  LIKE CONCAT('%', #{kw}, '%'))
          </if>
          <if test="available != null">
            AND available = #{available}
          </if>
        </where>
        ${orderBy}
        LIMIT #{limit} OFFSET #{offset}
        </script>
        """)
    List<Book> searchByKeywordWithOrder(@Param("kw") String keyword,
                                        @Param("available") Boolean available,
                                        @Param("offset") long offset,
                                        @Param("limit") long limit,
                                        @Param("orderBy") String orderBySql);

    @Select("""
        SELECT book_id as bookId, title, author, genre, quantity, available, path
        FROM book
        WHERE genre = #{genre}
        ORDER BY book_id DESC
        """)
    List<Book> selectByGenre(@Param("genre") String genre);
}
