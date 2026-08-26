package tuantha8203.inventoryservice.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "stock")
@Getter
@NoArgsConstructor
public class Stock {

    @Id
    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "available_qty", nullable = false)
    private int availableQty;

    @Column(name = "reserved_qty", nullable = false)
    private int reservedQty;

    @Column(name = "last_fencing_token", nullable = false)
    private long lastFencingToken;

    public Stock(UUID productId, int availableQty) {
        this.productId = productId;
        this.availableQty = availableQty;
    }
}