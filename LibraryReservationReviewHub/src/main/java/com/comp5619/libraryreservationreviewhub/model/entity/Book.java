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
    @TableId(value = "book_id", type = IdType.AUTO)
    private Integer bookId;

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
     * 总数量
     */
    private Integer quantity;

    /**
     * 是否可借 (默认 true)
     */
    private Boolean available;

    /**
     * 逻辑删除标记 (如果你需要软删除，可加上；不需要就去掉)
     */
    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
