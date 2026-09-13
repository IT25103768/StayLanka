package com.staylanka.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class StaffForm {
    @NotBlank @Email @Size(max = 190)
    private String email;
    @NotBlank @Size(min = 8, max = 72)
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,72}$", message = "Use at least 8 characters with a letter and number")
    private String password;
    @NotBlank @Size(max = 80)
    private String firstName;
    @NotBlank @Size(max = 80)
    private String lastName;
    @NotBlank @Size(max = 100)
    private String jobTitle;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }
}

