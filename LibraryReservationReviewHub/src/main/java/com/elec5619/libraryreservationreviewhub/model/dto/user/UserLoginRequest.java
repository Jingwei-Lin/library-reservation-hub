package com.elec5619.libraryreservationreviewhub.model.dto.user;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

/**
 * Login request
 */
@Data
public class UserLoginRequest implements Serializable {

    private static final long serialVersionUID = 8735650154179439661L;

    /**
     * email (used as userAccount)
     */
    private String userAccount;

    /**
     * Password (kept out of toString so it is never logged)
     */
    @ToString.Exclude
    private String userPassword;

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
}
