package com.staylanka.customer;
public class WalkInForm extends CustomerProfileForm {
    @jakarta.validation.constraints.NotBlank
    @jakarta.validation.constraints.Email
    @jakarta.validation.constraints.Size(max=190)
    private String email;
    public String getEmail(){return email;}
    public void setEmail(String email){this.email=email;}
}
