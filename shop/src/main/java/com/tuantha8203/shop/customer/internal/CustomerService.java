package com.tuantha8203.shop.customer.internal;

import com.tuantha8203.shop.customer.CustomerApi;
import com.tuantha8203.shop.customer.ShippingAddress;
import com.tuantha8203.shop.shared.ConstraintViolations;
import com.tuantha8203.shop.shared.InvalidRequestException;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerService implements CustomerApi {

    private final CustomerRepository customers;

    @Transactional
    public CustomerProfile register(RegisterCustomerRequest request) {
        // Friendly early check; the unique index on lower(email) is the real guard against races (step 1.5)
        if (customers.findByEmail(request.email()).isPresent()) {
            throw new EmailAlreadyRegisteredException(request.email());
        }
        try {
            Customer saved = customers.saveAndFlush(new Customer(request.email(), request.fullName(), request.phone()));
            return CustomerProfile.from(saved);
        } catch (DataIntegrityViolationException ex) {
            // Two concurrent registrations with the same email both passed the check above
            if (ConstraintViolations.constraintName(ex)
                    .filter("uq_customers_email_lower"::equals)
                    .isPresent()) {
                throw new EmailAlreadyRegisteredException(request.email());
            }
            throw ex;
        }
    }

    public CustomerProfile getProfile(long customerId) {
        return CustomerProfile.from(load(customerId));
    }

    @Transactional
    public CustomerProfile updateProfile(long customerId, UpdateProfileRequest request) {
        Customer customer = load(customerId);
        customer.updateProfile(request.fullName(), request.phone());
        return CustomerProfile.from(customer);
    }

    public List<AddressView> listAddresses(long customerId) {
        return load(customerId).getAddresses().stream().map(AddressView::from).toList();
    }

    @Transactional
    public AddressView addAddress(long customerId, AddAddressRequest request) {
        Customer customer = load(customerId);
        Address address = customer.addAddress(
                request.receiverName(), request.phone(), request.street(), request.ward(), request.province());
        customers.flush(); // INSERT now so the new address has its generated id
        return AddressView.from(address);
    }

    @Transactional
    public void changeDefaultAddress(long customerId, long addressId) {
        Customer customer = load(customerId);
        customer.clearDefaultAddress();
        // force the UPDATE clearing the old default to run FIRST, otherwise the unique index may reject it
        customers.flush();
        customer.markDefault(addressId);
    }

    @Transactional
    public void removeAddress(long customerId, long addressId) {
        Customer customer = load(customerId);
        boolean wasDefault = customer.removeAddress(addressId);
        // Hibernate always executes DELETEs after UPDATEs within one flush.
        // Without this flush, promoting a new default would hit the unique index
        // while the old default row still exists.
        customers.flush();
        if (wasDefault) {
            customer.promoteFirstAddressToDefault();
        }
    }

    /** Used by other modules (ordering) through CustomerApi. */
    @Override
    public Optional<ShippingAddress> findDefaultAddress(long customerId) {
        return customers
                .findById(customerId)
                .flatMap(Customer::defaultAddress)
                .map(address -> new ShippingAddress(
                        address.getReceiverName(),
                        address.getPhone(),
                        address.getStreet(),
                        address.getWard(),
                        address.getProvince()));
    }

    private Customer load(long customerId) {
        return customers.findById(customerId).orElseThrow(() -> new CustomerNotFoundException(customerId));
    }

    /** Creates the profile for a new account, or attaches the account to an existing profile with its email. */
    @Transactional
    public long createOrLink(CustomerIdentity identity) {
        if (identity.email() == null || identity.email().isBlank()) {
            throw new InvalidRequestException("The account has no email address");
        }
        Optional<Customer> sameEmail = customers.findByEmail(identity.email());
        if (sameEmail.isPresent()) {
            Customer existing = sameEmail.get();
            // Never attach an account to a profile on the strength of an UNVERIFIED email:
            // anyone could register "victim@mail.com" in Keycloak and inherit the victim's orders
            if (existing.getKeycloakId() != null || !identity.emailVerified()) {
                throw new EmailAlreadyRegisteredException(identity.email());
            }
            existing.linkIdentity(identity.subject());
            return existing.getId();
        }
        return customers
                .saveAndFlush(Customer.fromIdentity(identity.subject(), identity.email(), identity.displayName()))
                .getId();
    }
}
