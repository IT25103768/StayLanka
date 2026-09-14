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

    private final CustomerProfileRepository customerRepository;
    private final CurrentUserService currentUserService;

    public CustomerService(
            CustomerProfileRepository customerRepository,
            CurrentUserService currentUserService) {

        this.customerRepository = customerRepository;
        this.currentUserService = currentUserService;
    }

    /**
     * Returns the profile of the currently authenticated customer.
     */
    @Transactional(readOnly = true)
    public CustomerProfile current(Authentication authentication) {
        return currentUserService.customer(authentication);
    }

    /**
     * Updates the profile of the currently authenticated customer.
     */
    @Transactional
    public void updateCurrent(
            Authentication authentication,
            CustomerProfileForm form) {

        CustomerProfile profile = currentUserService.customer(authentication);

        profile.update(
                form.getFirstName().trim(),
                form.getLastName().trim(),
                trimToNull(form.getPhone()),
                trimToNull(form.getAddress()),
                trimToNull(form.getNationality()),
                trimToNull(form.getIdentificationNumber())
        );
    }

    /**
     * Searches customers with pagination.
     */
    @Transactional(readOnly = true)
    public Page<CustomerProfile> search(String term, int page) {

        String searchTerm = term == null ? "" : term.trim();
        int pageNumber = Math.max(page, 0);

        PageRequest pageRequest = PageRequest.of(
                pageNumber,
                12,
                Sort.by("firstName").ascending()
        );

        return customerRepository.search(searchTerm, pageRequest);
    }

    /**
     * Returns a customer by ID.
     *
     * @throws NotFoundException if the customer does not exist
     */
    @Transactional(readOnly = true)
    public CustomerProfile get(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() ->
                        new NotFoundException("Customer was not found.")
                );
    }

    /**
     * Toggles the active status of a customer's user account.
     */
    @Transactional
    public void toggleActive(Long id) {
        CustomerProfile profile = get(id);

        boolean active = profile.getUser().isActive();
        profile.getUser().setActive(!active);
    }

    /**
     * Trims a value and converts blank strings to null.
     */
    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}