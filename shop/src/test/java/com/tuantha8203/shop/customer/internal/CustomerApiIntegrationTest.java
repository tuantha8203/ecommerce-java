package com.tuantha8203.shop.customer.internal;

import static com.tuantha8203.shop.TestAuth.customer;
import static com.tuantha8203.shop.TestAuth.newSubject;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CustomerApiIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    CustomerService customerService;

    /** A token with an email chosen by the test, verified or not. */
    private static RequestPostProcessor tokenWithEmail(String subject, String email, boolean verified) {
        return jwt().jwt(token -> token.subject(subject)
                        .claim("email", email)
                        .claim("email_verified", verified)
                        .claim("name", "Someone"))
                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
    }

    private int addAddress(String subject, String street) throws Exception {
        String json = mvc.perform(post("/api/v1/customers/me/addresses")
                        .with(customer(subject))
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

    @Test
    void firstCallCreatesTheProfileFromTheToken() throws Exception {
        String subject = newSubject();

        mvc.perform(get("/api/v1/customers/me").with(customer(subject)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(subject + "@test.local"));
    }

    @Test
    void verifiedEmailLinksTheAccountToAnExistingProfile() throws Exception {
        // a profile created before accounts existed (phase 1-2), with no Keycloak id yet
        String email = "legacy-" + UUID.randomUUID().toString().substring(0, 8) + "@mail.com";
        long legacyId = customerService
                .register(new RegisterCustomerRequest(email, "Legacy Customer", null))
                .id();

        mvc.perform(get("/api/v1/customers/me").with(tokenWithEmail(newSubject(), email, true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(legacyId));
    }

    @Test
    void unverifiedEmailCannotTakeOverAnExistingProfile() throws Exception {
        String victim = newSubject();
        mvc.perform(get("/api/v1/customers/me").with(customer(victim))).andExpect(status().isOk());

        // an attacker registers in Keycloak with the victim's address but never verifies it
        mvc.perform(get("/api/v1/customers/me").with(tokenWithEmail(newSubject(), victim + "@test.local", false)))
                .andExpect(status().isConflict());
    }

    @Test
    void updatesProfileButNeverTheEmail() throws Exception {
        String subject = newSubject();

        mvc.perform(put("/api/v1/customers/me")
                        .with(customer(subject))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName": "An Tran", "phone": "0911111111", "email": "hacker@mail.com"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("An Tran"))
                .andExpect(jsonPath("$.email").value(subject + "@test.local"));
    }

    @Test
    void removingDefaultAddressPromotesTheNextOne() throws Exception {
        String subject = newSubject();
        int first = addAddress(subject, "1 Le Loi");
        int second = addAddress(subject, "2 Hai Ba Trung");

        mvc.perform(get("/api/v1/customers/me/addresses").with(customer(subject)))
                .andExpect(jsonPath("$[?(@.isDefault == true)].id").value(first));

        mvc.perform(post("/api/v1/customers/me/addresses/" + second + "/default")
                        .with(customer(subject)))
                .andExpect(status().isNoContent());

        mvc.perform(delete("/api/v1/customers/me/addresses/" + second).with(customer(subject)))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/customers/me/addresses").with(customer(subject)))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(first))
                .andExpect(jsonPath("$[0].isDefault").value(true));
    }

    @Test
    void cannotTouchAnotherCustomersAddress() throws Exception {
        String alice = newSubject();
        String bob = newSubject();
        int bobsAddress = addAddress(bob, "9 Bob Street");

        mvc.perform(delete("/api/v1/customers/me/addresses/" + bobsAddress).with(customer(alice)))
                .andExpect(status().isNotFound());

        mvc.perform(get("/api/v1/customers/me/addresses").with(customer(bob))).andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void requestWithoutTokenIsRejected() throws Exception {
        mvc.perform(get("/api/v1/customers/me")).andExpect(status().isUnauthorized());
    }
}
