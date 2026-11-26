package com.comp5619.libraryreservationreviewhub.mapper;

import com.comp5619.libraryreservationreviewhub.model.entity.Book;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface BookMapper {

    /* ================= 基础 CRUD ================= */

    /** 按 ID 查询：加入 description；去掉无意义的 ORDER BY */
    @Select("""
        SELECT book_id AS bookId, title, author, genre, description, quantity, held_quantity, available, path, is_delete AS isDelete
        FROM book
        WHERE book_id = #{id}
        """)
    Book selectById(@Param("id") Long id);

    /** Reads the book and locks its row until the transaction ends, so stock changes on it run one at a time */
    @Select("""
        SELECT book_id AS bookId, title, author, genre, description, quantity, held_quantity, available, path, is_delete AS isDelete
        FROM book
        WHERE book_id = #{id}
        FOR UPDATE
        """)
    Book selectByIdForUpdate(@Param("id") Long id);

    @Select("""
        SELECT COUNT(1)
        FROM book
        WHERE title = #{title} AND author = #{author} AND genre = #{genre}
        """)
    long countByTag(@Param("title") String title,
                    @Param("author") String author,
                    @Param("genre") String genre);


    @Insert("""
        INSERT INTO book (title, author, genre, description, quantity, held_quantity, available, path)
        VALUES (#{title}, #{author}, #{genre}, #{description}, #{quantity}, 0, #{available}, #{path})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "bookId", keyColumn = "book_id")
    int insertBook(Book b);

    /** 动态更新：仅更新非空字段，补充 description 与 path */
    @Update("""
        <script>
        UPDATE book
        <set>
          <if test="title         != null">title         = #{title},</if>
          <if test="author        != null">author        = #{author},</if>
          <if test="genre         != null">genre         = #{genre},</if>
          <if test="description   != null">description   = #{description},</if>
          <if test="quantity      != null">quantity      = #{quantity},</if>
          <if test="path          != null">path          = #{path},</if>
        </set>
        WHERE book_id = #{bookId}
        </script>
        """)
    int updateBook(Book b);

    /**
     * Soft delete. Copies held for the queue go back to the shelf count, because deleting the book ends its queue
     * (the caller cancels the entries in the same transaction).
     */
    @Update("""
        UPDATE book
        SET is_delete = TRUE,
            quantity = quantity + held_quantity,
            held_quantity = 0
        WHERE book_id = #{id} AND is_delete = FALSE
        """)
    int deleteById(@Param("id") Long id);

    @Update("UPDATE book SET is_delete = FALSE WHERE book_id = #{id} AND is_delete = TRUE")
    int restoreById(@Param("id") Long id);

    /*
     * Stock changes are single atomic UPDATEs rather than read-modify-write in Java, so concurrent
     * reservations and returns cannot overwrite each other's counts.
     * quantity is the shelf and held_quantity the copies set aside for notified users; a held copy is not
     * part of quantity. `available` (quantity > 0) is set by the book triggers.
     */

    /** Sets a copy aside for a notified user */
    @Update("UPDATE book SET held_quantity = held_quantity + 1 WHERE book_id = #{id}")
    int incrementHeldQuantity(@Param("id") Long id);

    /** Gives up a held copy; returns 0 when no copy is held */
    @Update("UPDATE book SET held_quantity = held_quantity - 1 WHERE book_id = #{id} AND held_quantity > 0")
    int decrementHeldQuantity(@Param("id") Long id);

    /**
     * Takes one copy from the shelf. Returns 0 when none is left, so two people can never both get the
     * last copy.
     */
    @Update("UPDATE book SET quantity = quantity - 1 WHERE book_id = #{id} AND quantity > 0 AND is_delete = FALSE")
    int takeAvailableCopy(@Param("id") Long id);

    /** Puts one copy back on the shelf */
    @Update("UPDATE book SET quantity = quantity + 1 WHERE book_id = #{id}")
    int returnCopy(@Param("id") Long id);

    /* ================== 分页 / 搜索 ================== */

    /** 统计：把 description 纳入关键词匹配 */
    @Select("""
        <script>
        SELECT COUNT(1)
        FROM book
        <where>
          <if test="kw != null and kw != ''">
            (title LIKE CONCAT('%', #{kw}, '%')
             OR author LIKE CONCAT('%', #{kw}, '%')
             OR genre  LIKE CONCAT('%', #{kw}, '%')
             OR description LIKE CONCAT('%', #{kw}, '%'))
          </if>
          <if test="available != null">
            AND available = #{available}
          </if>
          <if test="!includeDeleted">
            AND is_delete = FALSE
          </if>
        </where>
        </script>
        """)
    long countByKeyword(@Param("kw") String keyword,
                        @Param("available") Boolean available,
                        @Param("includeDeleted") boolean includeDeleted);

    /**
     *
     *
     */
    @Select("""
        <script>
        SELECT book_id AS bookId, title, author, genre, description, quantity, held_quantity, available, path, is_delete AS isDelete
        FROM book
        <where>
          <if test="kw != null and kw != ''">
            (title LIKE CONCAT('%', #{kw}, '%')
             OR author LIKE CONCAT('%', #{kw}, '%')
             OR genre  LIKE CONCAT('%', #{kw}, '%')
             OR description LIKE CONCAT('%', #{kw}, '%'))
          </if>
          <if test="available != null">
            AND available = #{available}
          </if>
          <if test="!includeDeleted">
            AND is_delete = FALSE
          </if>
        </where>
        ${orderBySql}
        LIMIT #{limit} OFFSET #{offset}
        </script>
        """)
    List<Book> searchByKeywordWithOrder(@Param("kw") String keyword,
                                        @Param("available") Boolean available,
                                        @Param("includeDeleted") boolean includeDeleted,
                                        @Param("offset") long offset,
                                        @Param("limit") long limit,
                                        @Param("orderBySql") String orderBySql);

    /** 按体裁查询：补充 description */
    @Select("""
        SELECT book_id AS bookId, title, author, genre, description, quantity, held_quantity, available, path, is_delete AS isDelete
        FROM book
        WHERE genre = #{genre} AND is_delete = FALSE
        ORDER BY title
        """)
    List<Book> selectByGenre(@Param("genre") String genre);


}