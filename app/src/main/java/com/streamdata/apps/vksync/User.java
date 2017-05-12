package com.streamdata.apps.vksync;

import android.graphics.Bitmap;

/**
 * TODO: Add a class header comment!
 */
public class User {
    private final String firstName;
    private final String lastName;
    private final String mobilePhone;
    private final Bitmap photo;

    public User(String firstName, String lastName, String mobilePhone, Bitmap photo) {
        this.mobilePhone = mobilePhone;
        this.firstName = firstName;
        this.lastName = lastName;
        this.photo = photo;
    }

    public String getMobilePhone() {
        return mobilePhone;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getFullName() {
        return String.format("%s %s", lastName, firstName).trim();
    }

    public Bitmap getPhoto() {
        return photo;
    }

    public String toString() {
        return String.format(
                "First name: %s, Last name: %s, Mobile phone: %s",
                firstName.isEmpty() ? "Unknown" : firstName,
                lastName.isEmpty() ? "Unknown" : lastName,
                mobilePhone.isEmpty() ? "Not available" : mobilePhone
        );
    }
}
