package com.tuantha8203.shop.customer.internal;

import com.tuantha8203.shop.customer.CurrentCustomerId;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
class CustomerController {

    private final CustomerService customers;

    // @PostMapping
    // @ResponseStatus(HttpStatus.CREATED)
    // CustomerProfile register(@Valid @RequestBody RegisterCustomerRequest request) {
    //     return customers.register(request);
    // }

    @GetMapping("/me")
    CustomerProfile me(@CurrentCustomerId long customerId) {
        return customers.getProfile(customerId);
    }

    @PutMapping("/me")
    CustomerProfile updateMe(@CurrentCustomerId long customerId, @Valid @RequestBody UpdateProfileRequest request) {
        return customers.updateProfile(customerId, request);
    }

    @GetMapping("/me/addresses")
    List<AddressView> myAddresses(@CurrentCustomerId long customerId) {
        return customers.listAddresses(customerId);
    }

    @PostMapping("/me/addresses")
    @ResponseStatus(HttpStatus.CREATED)
    AddressView addAddress(@CurrentCustomerId long customerId, @Valid @RequestBody AddAddressRequest request) {
        return customers.addAddress(customerId, request);
    }

    @PostMapping("/me/addresses/{addressId}/default")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void makeDefault(@CurrentCustomerId long customerId, @PathVariable long addressId) {
        customers.changeDefaultAddress(customerId, addressId);
    }

    @DeleteMapping("/me/addresses/{addressId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void removeAddress(@CurrentCustomerId long customerId, @PathVariable long addressId) {
        customers.removeAddress(customerId, addressId);
    }
}
