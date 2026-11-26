package com.elec5619.libraryreservationreviewhub.common;

import lombok.Data;

/**
 * common page request
 */
@Data
public class PageRequest {

    /** The largest page any list returns */
    public static final int MAX_PAGE_SIZE = 100;

    /**
     * Current page number
     */
    private int current = 1;

    /**
     * Current page size
     */
    private int pageSize = 10;

    /**
     * sort field
     */
    private String sortField;

    /**
     * Sort order (default descending)
     */
    private String sortOrder = "ASC";
}


