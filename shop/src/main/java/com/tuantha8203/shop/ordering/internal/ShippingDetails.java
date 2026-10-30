package com.tuantha8203.shop.ordering.internal;

import com.tuantha8203.shop.customer.ShippingAddress;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** A group of columns stored inside the orders table, not a separate table. */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShippingDetails {

    @Column(name = "ship_receiver_name", nullable = false)
    private String receiverName;

    @Column(name = "ship_phone", nullable = false)
    private String phone;

    @Column(name = "ship_street", nullable = false)
    private String street;

    @Column(name = "ship_ward", nullable = false)
    private String ward;

    @Column(name = "ship_province", nullable = false)
    private String province;

    static ShippingDetails copyOf(ShippingAddress address) {
        ShippingDetails details = new ShippingDetails();
        details.receiverName = address.receiverName();
        details.phone = address.phone();
        details.street = address.street();
        details.ward = address.ward();
        details.province = address.province();
        return details;
    }
}
