package com.comp5619.libraryreservationreviewhub.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 更新用户请求
 */
@Data
public class UserUpdateRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * email (used as userAccount)
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
     * phone
     */
    private String phone;

    /**
     * is_admin (maps to userRole: true = "admin", false = "user")
     */
    private Boolean isAdmin;

    private static final long serialVersionUID = 1L;
}