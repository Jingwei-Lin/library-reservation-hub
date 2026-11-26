package com.comp5619.libraryreservationreviewhub.model.dto.book;

import lombok.Data;

import java.io.Serializable;

/**
 * A new book
 */
@Data
public class BookAddRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String title;

    private String author;

    private String genre;

    /**
     * Copies
     */
    private Integer quantity;

    /**
     * Ignored: availability follows the shelf count (a trigger sets it)
     */
    private Boolean available;

    private String description;

    /**
    * Cover image file name
    */
    private String path;
}
