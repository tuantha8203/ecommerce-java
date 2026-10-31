package com.tuantha8203.shop.customer.internal;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(schema = "customer", name = "customers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false, length = 150)
    private String fullName;

    @Column(length = 20)
    private String phone;

    @Version
    private long version;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<Address> addresses = new ArrayList<>();

    @Column(length = 64)
    private String keycloakId;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    public Customer(String email, String fullName, String phone) {
        this.email = email.trim().toLowerCase();
        this.fullName = fullName;
        this.phone = phone;
    }

    /** Địa chỉ đầu tiên tự động là mặc định. */
    public Address addAddress(String receiverName, String phone, String street, String ward, String province) {
        Address address = new Address(this, receiverName, phone, street, ward, province);
        if (addresses.isEmpty()) {
            address.markDefault();
        }
        addresses.add(address);
        return address;
    }

    public Optional<Address> defaultAddress() {
        return addresses.stream().filter(Address::isDefault).findFirst();
    }

    public void updateProfile(String fullName, String phone) {
        this.fullName = fullName;
        this.phone = phone;
    }

    // The methods below are only for CustomerService (same package).
    // Each change to the default address needs a flush in between; see CustomerService.

    void clearDefaultAddress() {
        addresses.forEach(Address::unmarkDefault);
    }

    void markDefault(long addressId) {
        findAddress(addressId).markDefault();
    }

    /** Removes the address and reports whether it was the default one. */
    boolean removeAddress(long addressId) {
        Address address = findAddress(addressId);
        addresses.remove(address); // orphanRemoval = true turns this into a DELETE
        return address.isDefault();
    }

    void promoteFirstAddressToDefault() {
        addresses.stream().findFirst().ifPresent(Address::markDefault);
    }

    /** Looking up through this customer's own collection is what prevents touching someone else's address. */
    private Address findAddress(long addressId) {
        return addresses.stream()
                .filter(address -> Long.valueOf(addressId).equals(address.getId()))
                .findFirst()
                .orElseThrow(() -> new AddressNotFoundException(addressId));
    }

    /** A profile created from a logged-in account the first time it calls the API. */
    static Customer fromIdentity(String keycloakId, String email, String fullName) {
        Customer customer = new Customer(email, fullName, null);
        customer.keycloakId = keycloakId;
        return customer;
    }

    void linkIdentity(String keycloakId) {
        this.keycloakId = keycloakId;
    }
}
