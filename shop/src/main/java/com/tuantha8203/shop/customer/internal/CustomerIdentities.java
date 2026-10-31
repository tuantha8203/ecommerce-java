package com.tuantha8203.shop.customer.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/** Maps a logged-in account to its customer profile, creating the profile on first use. */
@Service
@RequiredArgsConstructor
public class CustomerIdentities {

    private final CustomerRepository customers;
    private final CustomerService customerService;

    public long resolve(CustomerIdentity identity) {
        return customers
                .findByKeycloakId(identity.subject())
                .map(Customer::getId)
                .orElseGet(() -> provision(identity));
    }

    private long provision(CustomerIdentity identity) {
        try {
            return customerService.createOrLink(identity);
        } catch (DataIntegrityViolationException ex) {
            // A brand-new user fired two requests at once and the other one created the profile first.
            // This runs outside the failed transaction, so the lookup sees the committed row.
            return customers
                    .findByKeycloakId(identity.subject())
                    .map(Customer::getId)
                    .orElseThrow(() -> ex);
        }
    }
}
