package tuantha8203.inventoryservice.infrastructure;

import tuantha8203.inventoryservice.domain.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface StockRepository extends JpaRepository<Stock, UUID> {

    // Giữ (reserve) qty tồn kho cho 1 sản phẩm.
    // Giữ được (1 row) khi: availableQty >= qty (đủ hàng) VÀ lastFencingToken < token (token còn mới).
    // Giữ thất bại (0 row) khi: hết hàng, hoặc token đã lỗi thời (có process khác ghi bằng token cao hơn).
    @Modifying
    @Query("""
        UPDATE Stock s
        SET s.availableQty = s.availableQty - :qty,
            s.reservedQty = s.reservedQty + :qty,
            s.lastFencingToken = :token
        WHERE s.productId = :productId
          AND s.availableQty >= :qty
          AND s.lastFencingToken < :token
        """)
    int reserveIfAvailable(@Param("productId") UUID productId,
                            @Param("qty") int qty,
                            @Param("token") long token);

    // Trả (release) qty đã reserve về lại tồn kho khả dụng.
    // Nhả được (1 row) khi: lastFencingToken < token (token còn mới) — không check availableQty vì đang cộng.
    // Nhả thất bại (0 row) khi: token đã lỗi thời.
    @Modifying
    @Query("""
        UPDATE Stock s
        SET s.availableQty = s.availableQty + :qty,
            s.reservedQty = s.reservedQty - :qty,
            s.lastFencingToken = :token
        WHERE s.productId = :productId
          AND s.lastFencingToken < :token
        """)
    int releaseIfNewerToken(@Param("productId") UUID productId,
                             @Param("qty") int qty,
                             @Param("token") long token);
}