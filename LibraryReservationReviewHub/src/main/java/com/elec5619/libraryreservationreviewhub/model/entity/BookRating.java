package com.elec5619.libraryreservationreviewhub.model.entity;

import lombok.Data;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import java.math.BigDecimal;

@Data
public class BookRating {
    @TableId("book_id")
    private Long bookId;

    @TableField("avg_rating")
    private BigDecimal avgRating;

    /** How many reviews the average is over; kept by the same procedure as the average */
    @TableField("review_count")
    private Integer reviewCount;
}