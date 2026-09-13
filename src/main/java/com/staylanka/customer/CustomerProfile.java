package com.staylanka.customer;

import com.staylanka.common.BaseEntity;
import com.staylanka.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "customer_profiles")
public class CustomerProfile extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private AppUser user;

    @Column(name = "first_name", nullable = false, length = 80)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 80)
    private String lastName;

    @Column(length = 30)
    private String phone;

    @Column(length = 255)
    private String address;

    @Column(length = 80)
    private String nationality;

    @Column(name = "identification_number", length = 80)
    private String identificationNumber;

    protected CustomerProfile() {
    }

    public CustomerProfile(AppUser user, String firstName, String lastName, String phone) {
        this.user = user;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
    }

    public void update(String firstName, String lastName, String phone, String address,
                       String nationality, String identificationNumber) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.address = address;
        this.nationality = nationality;
        this.identificationNumber = identificationNumber;
    }

    public AppUser getUser() {
        return user;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public String getPhone() {
        return phone;
    }

    public String getAddress() {
        return address;
    }

    public String getNationality() {
        return nationality;
    }

    public String getIdentificationNumber() {
        return identificationNumber;
    }
}

