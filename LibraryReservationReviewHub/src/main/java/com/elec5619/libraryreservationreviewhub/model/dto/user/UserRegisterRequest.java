package com.elec5619.libraryreservationreviewhub.model.dto.user;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

/**
 * Registration request
 */
@Data
public class UserRegisterRequest implements Serializable {

    /**
     * email (used as userAccount/username)
     */
    private String userAccount;

    /**
     * Password (kept out of toString so it is never logged)
     */
    @ToString.Exclude
    private String userPassword;

    /**
     * Password confirmation
     */
    @ToString.Exclude
    private String checkPassword;

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

    private static final long serialVersionUID = 1L;

    public String getUserAccount() {
        return userAccount;
    }

    public void setUserAccount(String userAccount) {
        this.userAccount = userAccount;
    }

    public String getUserPassword() {
        return userPassword;
    }

    public void setUserPassword(String userPassword) {
        this.userPassword = userPassword;
    }

    public String getCheckPassword() {
        return checkPassword;
    }

    public void setCheckPassword(String checkPassword) {
        this.checkPassword = checkPassword;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
