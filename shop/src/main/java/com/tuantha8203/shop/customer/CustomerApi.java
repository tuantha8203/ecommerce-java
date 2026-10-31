package com.tuantha8203.shop.customer;

import java.util.Optional;

public interface CustomerApi {

    /** Empty when the customer does not exist or has no address yet. */
    Optional<ShippingAddress> findDefaultAddress(long customerId);

    Optional<CustomerContact> findContact(long customerId);
}
