package com.comp5619.libraryreservationreviewhub.model.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 用户
 * @TableName user
 */
@TableName(value = "Users")
@Data
public class User implements Serializable {
    /**
     * id
     */
    @TableId(value = "user_id", type = IdType.AUTO)
    private Long id;

    /**
     * email (used as userAccount/username)
     */
    @TableField("email")
    private String userAccount;

    /**
     * password
     */
    @TableField("password")
    private String userPassword;

    /**
     * first_name
     */
    @TableField("first_name")
    private String firstName;

    /**
     * last_name
     */
    @TableField("last_name")
    private String lastName;

    /**
     * is_admin (maps to userRole: true = "admin", false = "user")
     */
    @TableField("is_admin")
    private Boolean isAdmin;

    /**
     * phone
     */
    @TableField("phone")
    private String phone;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    public void setUserPassword(String userPassword) {
        this.userPassword = userPassword;
    }

    public String getUserPassword() {
        return userPassword;
    }

    public void setIsAdmin(boolean isAdmin) {
        this.isAdmin = isAdmin;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setCreateTime(Date date) {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setUserAccount(String userAccount) {
        this.userAccount = userAccount;
    }

    public String getUserAccount() {
        return userAccount;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPhone() {
        return phone;
    }
}