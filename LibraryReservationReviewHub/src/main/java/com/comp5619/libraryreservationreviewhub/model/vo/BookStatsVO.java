package com.comp5619.libraryreservationreviewhub.model.vo;

import lombok.Data;

import java.io.Serializable;

/** The library statistics on the Dashboard, counted by the database over the listed (not deleted) books */
@Data
public class BookStatsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** listed books */
    private long totalBooks;

    /** listed books with a copy on the shelf */
    private long availableBooks;

    /** distinct genres among the listed books */
    private long categories;
}
