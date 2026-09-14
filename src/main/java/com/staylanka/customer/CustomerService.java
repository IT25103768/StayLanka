package com.staylanka.customer;

import com.staylanka.common.NotFoundException;
import com.staylanka.security.CurrentUserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {
    @org.springframework.beans.factory.annotation.Autowired private com.staylanka.common.AuditService audit;
    @org.springframework.beans.factory.annotation.Autowired private com.staylanka.common.InputRules rules;
    private final CustomerProfileRepository customerRepository;
    private final CurrentUserService currentUserService;

    public CustomerService(CustomerProfileRepository customerRepository, CurrentUserService currentUserService) {
        this.customerRepository = customerRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public CustomerProfile current(Authentication authentication) {
        return currentUserService.customer(authentication);
    }

    @Transactional
    public void updateCurrent(Authentication authentication, CustomerProfileForm form) {
        rules.validate(form);
        CustomerProfile profile = currentUserService.customer(authentication);
        profile.update(form.getFirstName().trim(), form.getLastName().trim(), trimToNull(form.getPhone()),
                trimToNull(form.getAddress()), trimToNull(form.getNationality()),
                trimToNull(form.getIdentificationNumber()));
        profile.setPreferences(form.getPreferences(), form.isMarketingConsent());
        audit.record(profile, "UPDATE");
    }

    @Transactional(readOnly = true)
    public Page<CustomerProfile> search(String term, int page) {
        return customerRepository.search(term == null ? "" : term.trim(),
                PageRequest.of(Math.max(page, 0), 12, Sort.by("firstName").ascending()));
    }

    @Transactional(readOnly = true)
    public CustomerProfile get(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Customer was not found."));
    }

    @Transactional
    public void toggleActive(Long id) {
        CustomerProfile profile = get(id);
        profile.getUser().setActive(!profile.getUser().isActive());
        audit.record(profile, "STATUS_CHANGE");
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
