package com.elec5619.libraryreservationreviewhub.model.dto.book;

import lombok.Data;

import java.io.Serializable;

/**
 * An admin's edit of a book. Fields left out are unchanged. A quantity change must say which shelf count it was
 * made from ({@code expectedQuantity}): the edit is refused if the shelf has changed since, so a page loaded
 * before a reservation or a return can't turn its old count into stock.
 */
@Data
public class BookUpdateRequest implements Serializable {

    private String title;

    private String author;

    private String genre;

    private String description;

    /** Cover image file name */
    private String path;

    /** The new shelf count */
    private Integer quantity;

    /** The shelf count the edit was made from (required with a quantity) */
    private Integer expectedQuantity;

    private static final long serialVersionUID = 1L;
}
