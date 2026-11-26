package com.elec5619.libraryreservationreviewhub.common;

import lombok.Data;

import java.io.Serializable;

/**
 * common delete request
 */
@Data
public class DeleteRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    private static final long serialVersionUID = 1L;
}

