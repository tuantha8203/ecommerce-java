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

    // Hai method dưới chỉ dành cho CustomerService (cùng package), phải gọi đúng thứ tự kèm flush
    void clearDefaultAddress() {
        addresses.forEach(Address::unmarkDefault);
    }

    void markDefault(long addressId) {
        addresses.stream()
                .filter(a -> a.getId() == addressId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Địa chỉ " + addressId + " không thuộc khách này"))
                .markDefault();
    }
}
