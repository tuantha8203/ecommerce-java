package com.tuantha8203.shop.customer.internal;

public record CustomerProfile(Long id, String email, String fullName, String phone) {

    static CustomerProfile from(Customer customer) {
        return new CustomerProfile(customer.getId(), customer.getEmail(), customer.getFullName(), customer.getPhone());
    }
}
