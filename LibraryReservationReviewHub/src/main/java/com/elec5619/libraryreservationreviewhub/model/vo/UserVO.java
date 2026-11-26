package com.elec5619.libraryreservationreviewhub.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * A user, without the password hash
 */
@Data
public class UserVO implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * email (userAccount)
     */
    private String userAccount;

    /**
     * first_name
     */
    private String firstName;

    /**
     * last_name
     */
    private String lastName;

    /**
     * computed userName (firstName + " " + lastName)
     */
    private String userName;

    /**
     * is_admin (derived userRole)
     */
    private String userRole;

    /**
     * phone
     */
    private String phone;

    /**
     * status: 1 active, 0 banned (the admin page shows and toggles it)
     */
    private Integer status;

    /**
     * create_time
     */
    private Date createTime;

    private static final long serialVersionUID = 1L;
}