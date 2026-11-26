package com.comp5619.libraryreservationreviewhub.common;

import lombok.Data;

/**
 * common page request
 */
@Data
public class PageRequest {

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


