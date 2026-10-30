package com.tuantha8203.shop.customer.internal;

public record AddressView(
        Long id, String receiverName, String phone, String street, String ward, String province, boolean isDefault) {

    static AddressView from(Address address) {
        return new AddressView(
                address.getId(),
                address.getReceiverName(),
                address.getPhone(),
                address.getStreet(),
                address.getWard(),
                address.getProvince(),
                address.isDefault());
    }
}
