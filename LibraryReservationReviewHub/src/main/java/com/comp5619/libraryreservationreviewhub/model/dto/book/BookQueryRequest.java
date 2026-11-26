package com.comp5619.libraryreservationreviewhub.model.dto.book;

import lombok.Data;

import java.io.Serializable;

/**
 * Book search
 */
@Data
public class BookQueryRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer bookId;

    private String title;

    private String author;

    private String genre;

    /**
     * Only books with a copy available
     */
    private Boolean available;

    /**
     * Keyword
     */
    private String searchText;

    private long current = 1;

    private long pageSize = 10;

    private String sortField;

    /**
     * Sort order (asc/desc)
     */
    private String sortOrder;
}
