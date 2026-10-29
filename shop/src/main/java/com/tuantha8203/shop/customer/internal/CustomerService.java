package com.tuantha8203.shop.customer.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customers;

    @Transactional
    public void changeDefaultAddress(long customerId, long addressId) {
        Customer customer = customers
                .findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khách " + customerId));

        customer.clearDefaultAddress();
        customers.flush(); // ép UPDATE bỏ mặc định chạy TRƯỚC, nếu không unique index có thể báo trùng
        customer.markDefault(addressId);
    }
}
