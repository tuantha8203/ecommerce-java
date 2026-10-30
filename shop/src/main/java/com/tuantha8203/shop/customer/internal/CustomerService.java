package com.tuantha8203.shop.customer.internal;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerService {

    private final CustomerRepository customers;

    @Transactional
    public CustomerProfile register(RegisterCustomerRequest request) {
        // Friendly early check; the unique index on lower(email) is the real guard against races (step 1.5)
        if (customers.findByEmail(request.email()).isPresent()) {
            throw new EmailAlreadyRegisteredException(request.email());
        }
        Customer saved = customers.save(new Customer(request.email(), request.fullName(), request.phone()));
        return CustomerProfile.from(saved);
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
        customers.flush(); // force the UPDATE clearing the old default to run FIRST, otherwise the unique index may
        // reject it
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

    private Customer load(long customerId) {
        return customers.findById(customerId).orElseThrow(() -> new CustomerNotFoundException(customerId));
    }
}
