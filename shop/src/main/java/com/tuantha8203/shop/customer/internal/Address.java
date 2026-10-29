package com.tuantha8203.shop.customer.internal;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(schema = "customer", name = "addresses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(nullable = false, length = 150)
    private String receiverName;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(nullable = false)
    private String street;

    @Column(nullable = false, length = 100)
    private String ward;

    @Column(nullable = false, length = 100)
    private String province;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    Address(Customer customer, String receiverName, String phone, String street, String ward, String province) {
        this.customer = customer;
        this.receiverName = receiverName;
        this.phone = phone;
        this.street = street;
        this.ward = ward;
        this.province = province;
    }

    void markDefault() {
        this.isDefault = true;
    }

    void unmarkDefault() {
        this.isDefault = false;
    }
}
