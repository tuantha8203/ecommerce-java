package com.tuantha8203.shop;

import static com.tuantha8203.shop.TestAuth.admin;
import static com.tuantha8203.shop.TestAuth.customer;
import static com.tuantha8203.shop.TestAuth.newSubject;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Who may call what: 401 without a token, 403 without the role, 2xx with it. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SecurityIntegrationTest {

    @Autowired
    MockMvc mvc;

    private static String newCategory() {
        return """
                {"name": "Sec", "slug": "sec-%s"}
                """.formatted(UUID.randomUUID().toString().substring(0, 8));
    }

    @Test
    void theCatalogIsPublic() throws Exception {
        mvc.perform(get("/api/v1/products")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/categories")).andExpect(status().isOk());
    }

    @Test
    void shoppingNeedsAToken() throws Exception {
        mvc.perform(get("/api/v1/cart")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/orders")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/cart").with(customer(newSubject()))).andExpect(status().isOk());
    }

    @Test
    void adminEndpointsNeedTheAdminRole() throws Exception {
        mvc.perform(post("/api/v1/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newCategory()))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/v1/admin/categories")
                        .with(customer(newSubject()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newCategory()))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/v1/admin/categories")
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newCategory()))
                .andExpect(status().isCreated());
    }

    @Test
    void healthCheckIsOpenForTheLoadBalancer() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }
}
