package com.staylanka.customer;
import com.staylanka.auth.*;
import com.staylanka.common.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.persistence.EntityManager;

@Service
@PreAuthorize("hasAnyRole('PROFILE_MANAGER','ADMIN')")
public class ProfileOperationsService {
    private final RegistrationService registration; private final CustomerService customers;
    private final CustomerProfileRepository profiles; private final InputRules rules; private final AuditService audit; private final EntityManager em;
    public ProfileOperationsService(RegistrationService registration,CustomerService customers,CustomerProfileRepository profiles,InputRules rules,AuditService audit,EntityManager em){
        this.registration=registration;this.customers=customers;this.profiles=profiles;this.rules=rules;this.audit=audit;this.em=em;
    }
    @Transactional public CustomerProfile create(WalkInForm form){
        rules.validate(form); RegistrationForm registrationForm=new RegistrationForm();
        registrationForm.setEmail(form.getEmail()); registrationForm.setFirstName(form.getFirstName()); registrationForm.setLastName(form.getLastName());registrationForm.setPhone(form.getPhone());
        String password="Walkin9"+java.util.UUID.randomUUID();registrationForm.setPassword(password);registrationForm.setConfirmPassword(password);
        registration.register(registrationForm);
        CustomerProfile profile=profiles.findByUserEmailIgnoreCase(form.getEmail().trim()).orElseThrow();update(profile.getId(),form);return profile;
    }
    @Transactional public void update(Long id,CustomerProfileForm form){
        rules.validate(form);CustomerProfile profile=customers.get(id);
        profile.update(form.getFirstName().trim(),form.getLastName().trim(),form.getPhone(),form.getAddress(),form.getNationality(),form.getIdentificationNumber());
        profile.setPreferences(form.getPreferences(),form.isMarketingConsent());audit.record(profile,"UPDATE");
    }
    @Transactional public void anonymise(Long id){
        CustomerProfile profile=customers.get(id);
        long active=em.createQuery("select count(r) from Reservation r where r.customer.id=:id and r.status in (com.staylanka.reservation.ReservationStatus.PENDING,com.staylanka.reservation.ReservationStatus.CONFIRMED,com.staylanka.reservation.ReservationStatus.CHECKED_IN)",Long.class).setParameter("id",id).getSingleResult();
        if(active>0) throw new BusinessRuleException("Resolve active reservations before anonymising this profile.");
        profile.update("Anonymised","Guest",null,null,null,null); profile.setPreferences(null,false);
        profile.getUser().setEmail("anonymised-"+java.util.UUID.randomUUID()+"@invalid.local");profile.getUser().setActive(false);
        audit.record(profile,"ANONYMISE");
    }
}
