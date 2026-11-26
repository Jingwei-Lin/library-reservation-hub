package com.comp5619.libraryreservationreviewhub.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * A book, as the API returns it
 */
@Data
public class BookVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long bookId;

    private String title;

    private String author;

    private String genre;

    /**
     * Copies on the shelf
     */
    private Integer quantity;

    /**
     * on held quantity for waiting list
     */
    private Integer heldQuantity;

    /**
     * A copy can be reserved now
     */
    private Boolean available;

    /**
     * Cover image file name
     */
    private String path;

    /**
     * description
     */
    private String description;

    /**
     * Deleted by an admin. Only admins ever see a deleted book.
     */
    private Boolean deleted;

}