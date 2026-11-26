package com.comp5619.libraryreservationreviewhub.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;

/**
 * 图书实体类
 */
@TableName(value = "book")  // 数据库表名小写 book
@Data
public class Book implements Serializable {

    /**
     * 图书ID - 主键 (对应 book_id)
     */
    @TableId(value = "book_id")
    private Long bookId;

    /**
     * 书名
     */
    private String title;

    /**
     * 作者
     */
    private String author;

    /**
     * 图书类别 (genre)
     */
    private String genre;

    /**
     * Copies on the shelf that anyone can reserve now
     */
    private Integer quantity;

    /**
     * Copies set aside for notified waiting-list users. Not part of {@code quantity}.
     */
    @TableField("held_quantity")
    private Integer heldQuantity = 0;

    /**
     * A copy is on the shelf ({@code quantity > 0}), worked out by the database triggers
     */
    private Boolean available;

    /**
     * Deleted by an admin (is_delete). Kept apart from {@code available}, which only describes stock, so a copy
     * coming back can't bring a deleted book back.
     */
    private Boolean isDelete;

    /**
     * 封面路径
     */
    private String path;

    /**
     * description
     */
    private String description;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;


    /** Held copies are not part of quantity, so every copy counted in it can be reserved */
    public Integer getAvailableQuantity() {
        return quantity != null ? quantity : 0;
    }

    public boolean isPubliclyAvailable() {
        return getAvailableQuantity() > 0;
    }
}