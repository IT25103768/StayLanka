package com.staylanka.customer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CustomerProfileForm {
    @Size(max = 1000) private String preferences;
    private boolean marketingConsent;
    public String getPreferences() { return preferences; }
    public void setPreferences(String value) { preferences = value; }
    public boolean isMarketingConsent() { return marketingConsent; }
    public void setMarketingConsent(boolean value) { marketingConsent = value; }
    @NotBlank
    @Size(max = 80)
    private String firstName;

    @NotBlank
    @Size(max = 80)
    private String lastName;

    @Pattern(regexp = "^$|^[+]?[0-9 ()-]{7,20}$", message = "Enter a valid phone number")
    private String phone;

    @Size(max = 255)
    private String address;

    @Size(max = 80)
    private String nationality;

    @Size(max = 80)
    private String identificationNumber;

    public static CustomerProfileForm from(CustomerProfile profile) {
        CustomerProfileForm form = new CustomerProfileForm();
        form.preferences = profile.getPreferences();
        form.marketingConsent = profile.isMarketingConsent();
        form.firstName = profile.getFirstName();
        form.lastName = profile.getLastName();
        form.phone = profile.getPhone();
        form.address = profile.getAddress();
        form.nationality = profile.getNationality();
        form.identificationNumber = profile.getIdentificationNumber();
        return form;
    }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getNationality() { return nationality; }
    public void setNationality(String nationality) { this.nationality = nationality; }
    public String getIdentificationNumber() { return identificationNumber; }
    public void setIdentificationNumber(String identificationNumber) { this.identificationNumber = identificationNumber; }
}
