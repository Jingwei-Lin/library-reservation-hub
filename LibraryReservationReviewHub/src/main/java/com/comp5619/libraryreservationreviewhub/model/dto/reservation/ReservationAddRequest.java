package com.comp5619.libraryreservationreviewhub.model.dto.reservation;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class ReservationAddRequest implements Serializable {
    private Long bookId;
    private Date pickupTime; // Optional: user may specify desired pickup time
    private static final long serialVersionUID = 1L;
}