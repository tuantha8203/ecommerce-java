package com.tuantha8203.shop.payment.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.tuantha8203.shop.TestcontainersConfiguration;
import com.tuantha8203.shop.cart.internal.CartService;
import com.tuantha8203.shop.catalog.internal.CategoryService;
import com.tuantha8203.shop.catalog.internal.CreateCategoryRequest;
import com.tuantha8203.shop.catalog.internal.CreateProductRequest;
import com.tuantha8203.shop.catalog.internal.ProductService;
import com.tuantha8203.shop.customer.internal.AddAddressRequest;
import com.tuantha8203.shop.customer.internal.CustomerService;
import com.tuantha8203.shop.customer.internal.RegisterCustomerRequest;
import com.tuantha8203.shop.inventory.internal.InventoryService;
import com.tuantha8203.shop.shared.CallerHeaders;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.client.ResourceAccessException;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class PaymentFlowIntegrationTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    GatewayClient gateway; // the remote gateway is simulated; everything else is real

    @Autowired
    PaymentService paymentService;

    @Autowired
    PaymentRepository payments;

    @Autowired
    WebhookSignatures signatures;

    @Autowired
    CustomerService customerService;

    @Autowired
    CategoryService categoryService;

    @Autowired
    ProductService productService;

    @Autowired
    InventoryService inventoryService;

    @Autowired
    CartService cartService;

    @Autowired
    Clock clock;

    private long customerId;
    private long productId;

    // --- helpers ---

    private void buyerWithTwoShirtsInCart() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        customerId = customerService
                .register(new RegisterCustomerRequest("pay-" + suffix + "@mail.com", "Payer", null))
                .id();
        customerService.addAddress(
                customerId, new AddAddressRequest("Payer", "0900000001", "1 Le Loi", "Ben Thanh Ward", "HCMC"));
        long categoryId = categoryService
                .create(new CreateCategoryRequest("Pay", "pay-" + suffix, null))
                .id();
        productId = productService
                .create(new CreateProductRequest(
                        categoryId, "PAY-" + suffix.toUpperCase(), "Shirt", "pay-shirt-" + suffix, null, 100_000))
                .id();
        productService.activate(productId);
        inventoryService.setOnHand(productId, 10);
        cartService.setQuantity(customerId, productId, 2);
    }

    private int placeOrder(String expectedStatus) throws Exception {
        String json = mvc.perform(post("/api/v1/orders")
                        .header(CallerHeaders.CUSTOMER_ID, customerId)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentToken\": \"tok_visa\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(expectedStatus))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(json, "$.id");
    }

    private void gatewayAnswers(String status) {
        when(gateway.charge(any(), any(), anyLong(), any()))
                .thenAnswer(call -> new GatewayClient.ChargeResponse(
                        "ch_" + UUID.randomUUID(),
                        call.getArgument(1),
                        call.getArgument(2),
                        status,
                        "DECLINED".equals(status) ? "card_declined" : null));
    }

    private void gatewayTimesOut() {
        when(gateway.charge(any(), any(), anyLong(), any())).thenThrow(new ResourceAccessException("Read timed out"));
    }

    private ResultActions sendWebhook(
            String eventId, String orderRef, String chargeId, String status, boolean validSignature) throws Exception {
        String body = """
                {"eventId":"%s","type":"charge.%s","chargeId":"%s","reference":"%s","amount":200000,"currency":"VND","status":"%s","occurredAt":"2026-01-01T00:00:00Z"}""".formatted(eventId, status.toLowerCase(), chargeId, orderRef, status);
        return mvc.perform(post("/api/v1/payments/webhook")
                .header("X-Fake-Payment-Signature", validSignature ? signatures.sign(body) : "forged")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private void expectOrderStatus(int orderId, String expected) throws Exception {
        mvc.perform(get("/api/v1/orders/" + orderId).header(CallerHeaders.CUSTOMER_ID, customerId))
                .andExpect(jsonPath("$.status").value(expected));
    }

    private PaymentStatus paymentStatus(int orderId) {
        return payments.findByOrderRef("order-" + orderId).orElseThrow().getStatus();
    }

    // --- tests ---

    @Test
    void successfulPaymentMarksOrderPaidAndStockNeverExpires() throws Exception {
        buyerWithTwoShirtsInCart();
        gatewayAnswers("SUCCEEDED");

        placeOrder("PAID");

        // even one hour later the expiry job must not touch a paid order's stock
        inventoryService.releaseExpired(clock.instant().plus(Duration.ofHours(1)), 1000);
        assertThat(inventoryService.getStock(productId).reserved()).isEqualTo(2);
    }

    @Test
    void declinedPaymentCancelsTheOrderAndReleasesStock() throws Exception {
        buyerWithTwoShirtsInCart();
        gatewayAnswers("DECLINED");

        placeOrder("CANCELLED");

        assertThat(inventoryService.getStock(productId).reserved()).isZero();
    }

    @Test
    void timeoutLeavesTheOrderPendingUntilTheWebhookSettlesIt() throws Exception {
        buyerWithTwoShirtsInCart();
        gatewayTimesOut();

        int orderId = placeOrder("PENDING_PAYMENT");
        assertThat(paymentStatus(orderId)).isEqualTo(PaymentStatus.UNKNOWN);

        sendWebhook("evt_" + UUID.randomUUID(), "order-" + orderId, "ch_1", "SUCCEEDED", true)
                .andExpect(status().isOk());

        expectOrderStatus(orderId, "PAID");
    }

    @Test
    void forgedWebhookIsRejected() throws Exception {
        buyerWithTwoShirtsInCart();
        gatewayTimesOut();
        int orderId = placeOrder("PENDING_PAYMENT");

        sendWebhook("evt_" + UUID.randomUUID(), "order-" + orderId, "ch_1", "SUCCEEDED", false)
                .andExpect(status().isUnauthorized());

        expectOrderStatus(orderId, "PENDING_PAYMENT");
    }

    @Test
    void duplicateWebhookIsAcceptedButAppliedOnce() throws Exception {
        buyerWithTwoShirtsInCart();
        gatewayTimesOut();
        int orderId = placeOrder("PENDING_PAYMENT");
        String eventId = "evt_" + UUID.randomUUID();

        sendWebhook(eventId, "order-" + orderId, "ch_1", "SUCCEEDED", true).andExpect(status().isOk());
        sendWebhook(eventId, "order-" + orderId, "ch_1", "SUCCEEDED", true).andExpect(status().isOk());

        expectOrderStatus(orderId, "PAID");
    }

    @Test
    void reconciliationSettlesAPaymentWhoseResponseWasLost() throws Exception {
        buyerWithTwoShirtsInCart();
        gatewayTimesOut();
        int orderId = placeOrder("PENDING_PAYMENT");
        String orderRef = "order-" + orderId;
        when(gateway.findByReference(orderRef))
                .thenReturn(List.of(new GatewayClient.ChargeResponse("ch_lost", orderRef, 200_000, "SUCCEEDED", null)));

        // run the job as if five minutes had passed
        paymentService.reconcile(clock.instant().plus(Duration.ofMinutes(5)));

        expectOrderStatus(orderId, "PAID");
    }

    @Test
    void moneyArrivingAfterCancellationIsRefunded() throws Exception {
        buyerWithTwoShirtsInCart();
        gatewayTimesOut();
        int orderId = placeOrder("PENDING_PAYMENT");
        String orderRef = "order-" + orderId;

        mvc.perform(post("/api/v1/orders/" + orderId + "/cancellation").header(CallerHeaders.CUSTOMER_ID, customerId))
                .andExpect(status().isNoContent());

        // the lost charge turns out to have succeeded after all
        sendWebhook("evt_" + UUID.randomUUID(), orderRef, "ch_late", "SUCCEEDED", true)
                .andExpect(status().isOk());
        assertThat(paymentStatus(orderId)).isEqualTo(PaymentStatus.REFUND_REQUESTED);
        expectOrderStatus(orderId, "CANCELLED");

        when(gateway.refund("ch_late"))
                .thenReturn(new GatewayClient.ChargeResponse("ch_late", orderRef, 200_000, "REFUNDED", null));
        paymentService.processRefunds();

        verify(gateway).refund("ch_late");
        assertThat(paymentStatus(orderId)).isEqualTo(PaymentStatus.REFUNDED);
    }
}
