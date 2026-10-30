package com.tuantha8203.shop.customer.internal;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.tuantha8203.shop.TestcontainersConfiguration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CustomerApiIntegrationTest {

    private static final String HEADER = CustomerController.CUSTOMER_ID_HEADER;

    @Autowired
    MockMvc mvc;

    private int register(String email) throws Exception {
        String json = mvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "fullName": "An Nguyen", "phone": "0900000001"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(json, "$.id");
    }

    private int addAddress(int customerId, String street) throws Exception {
        String json = mvc.perform(post("/api/v1/customers/me/addresses")
                        .header(HEADER, customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"receiverName": "An Nguyen", "phone": "0900000001",
                                 "street": "%s", "ward": "Ben Thanh Ward", "province": "Ho Chi Minh City"}
                                """.formatted(street)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(json, "$.id");
    }

    private static String uniqueEmail() {
        return "user-" + UUID.randomUUID().toString().substring(0, 8) + "@mail.com";
    }

    @Test
    void registersAndRejectsSameEmailWithDifferentCase() throws Exception {
        String email = uniqueEmail();
        register(email);

        mvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "fullName": "Someone Else"}
                                """.formatted(email.toUpperCase())))
                .andExpect(status().isConflict());
    }

    @Test
    void updatesProfileButNeverTheEmail() throws Exception {
        String email = uniqueEmail();
        int id = register(email);

        mvc.perform(put("/api/v1/customers/me")
                        .header(HEADER, id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName": "An Tran", "phone": "0911111111", "email": "hacker@mail.com"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("An Tran"))
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void removingDefaultAddressPromotesTheNextOne() throws Exception {
        int id = register(uniqueEmail());
        int first = addAddress(id, "1 Le Loi");
        int second = addAddress(id, "2 Hai Ba Trung");

        mvc.perform(get("/api/v1/customers/me/addresses").header(HEADER, id))
                .andExpect(jsonPath("$[?(@.isDefault == true)].id").value(first));

        mvc.perform(post("/api/v1/customers/me/addresses/" + second + "/default")
                        .header(HEADER, id))
                .andExpect(status().isNoContent());

        mvc.perform(delete("/api/v1/customers/me/addresses/" + second).header(HEADER, id))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/customers/me/addresses").header(HEADER, id))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(first))
                .andExpect(jsonPath("$[0].isDefault").value(true));
    }

    @Test
    void cannotTouchAnotherCustomersAddress() throws Exception {
        int alice = register(uniqueEmail());
        int bob = register(uniqueEmail());
        int bobsAddress = addAddress(bob, "9 Bob Street");

        mvc.perform(delete("/api/v1/customers/me/addresses/" + bobsAddress).header(HEADER, alice))
                .andExpect(status().isNotFound());

        mvc.perform(get("/api/v1/customers/me/addresses").header(HEADER, bob)).andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void requestWithoutCustomerHeaderIsRejected() throws Exception {
        mvc.perform(get("/api/v1/customers/me")).andExpect(status().isBadRequest());
    }
}
