package com.tuantha8203.shop.inventory.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Only read through JPA; every change goes through conditional UPDATE statements in the repository. */
@Entity
@Table(schema = "inventory", name = "stock_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockItem {

    @Id
    private Long productId;

    @Column(nullable = false)
    private int onHand;

    @Column(nullable = false)
    private int reserved;

    public int available() {
        return onHand - reserved;
    }
}
