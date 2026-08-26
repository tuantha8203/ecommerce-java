package tuantha8203.productservice.api;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import tuantha8203.common.api.ApiResponse;
import tuantha8203.common.api.CommonErrorCode;
import tuantha8203.common.api.FieldError;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@AutoConfigureTestRestTemplate
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductControllerIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16");

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate rest;

    private static final ParameterizedTypeReference<ApiResponse<ProductResponse>> PRODUCT_RESPONSE =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<ApiResponse<List<ProductResponse>>> PRODUCT_LIST_RESPONSE =
            new ParameterizedTypeReference<>() {};

    private String baseUri() {
        return "http://localhost:" + port;
    }

    private ProductRequest newRequest(String sku) {
        return new ProductRequest("Integration product", "desc", sku, BigDecimal.TEN);
    }

    @Test
    void createAndFetchProduct() {
        var createResponse = rest.exchange(
                RequestEntity.post(URI.create(baseUri() + "/api/v1/products")).body(newRequest("SKU-IT-1")),
                PRODUCT_RESPONSE);
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResponse.getBody().success()).isTrue();
        assertThat(createResponse.getHeaders().getLocation()).isNull();

        var id = createResponse.getBody().data().id();
        var getResponse = rest.exchange(
                RequestEntity.method(HttpMethod.GET, URI.create(baseUri() + "/api/v1/products/" + id)).build(),
                PRODUCT_RESPONSE);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody().data().sku()).isEqualTo("SKU-IT-1");
    }

    @Test
    void getById_returnsWrappedErrorWhenMissing() {
        var missingId = "00000000-0000-0000-0000-000000000000";
        var response = rest.exchange(
                RequestEntity.method(HttpMethod.GET, URI.create(baseUri() + "/api/v1/products/" + missingId)).build(),
                PRODUCT_RESPONSE);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().errorCode()).isEqualTo(ProductErrorCode.PRODUCT_NOT_FOUND.name());
        assertThat(response.getBody().data()).isNull();
    }

    @Test
    void getById_returnsWrappedBadRequestForInvalidUuid() {
        var response = rest.exchange(
                RequestEntity.method(HttpMethod.GET, URI.create(baseUri() + "/api/v1/products/not-a-uuid")).build(),
                PRODUCT_RESPONSE);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().errorCode()).isEqualTo(CommonErrorCode.INVALID_PARAMETER.name());
    }

    @Test
    void create_returnsWrappedValidationErrorWhenSkuMissing() {
        var invalidRequest = new ProductRequest("No sku product", null, "  ", BigDecimal.TEN);

        var response = rest.exchange(
                RequestEntity.post(URI.create(baseUri() + "/api/v1/products")).body(invalidRequest),
                PRODUCT_RESPONSE);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().errors()).extracting(FieldError::field).contains("sku");
    }

    @Test
    void create_returnsWrappedConflictWhenSkuAlreadyExists() {
        rest.exchange(
                RequestEntity.post(URI.create(baseUri() + "/api/v1/products")).body(newRequest("SKU-DUP")),
                PRODUCT_RESPONSE);

        var duplicateResponse = rest.exchange(
                RequestEntity.post(URI.create(baseUri() + "/api/v1/products")).body(newRequest("SKU-DUP")),
                PRODUCT_RESPONSE);

        assertThat(duplicateResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(duplicateResponse.getBody().success()).isFalse();
    }

    @Test
    void update_replacesAllFieldsAndReturnsWrappedResponse() {
        var created = rest.exchange(
                RequestEntity.post(URI.create(baseUri() + "/api/v1/products")).body(newRequest("SKU-UPD-1")),
                PRODUCT_RESPONSE).getBody().data();

        var updateRequest = new ProductRequest("Updated name", "updated desc", "SKU-UPD-2", BigDecimal.ONE);
        var updateResponse = rest.exchange(
                RequestEntity.put(URI.create(baseUri() + "/api/v1/products/" + created.id())).body(updateRequest),
                PRODUCT_RESPONSE);

        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updateResponse.getBody().data().name()).isEqualTo("Updated name");
        assertThat(updateResponse.getBody().data().sku()).isEqualTo("SKU-UPD-2");
    }

    @Test
    void delete_returnsWrappedResponseWithHttp200NotNoContent() {
        var created = rest.exchange(
                RequestEntity.post(URI.create(baseUri() + "/api/v1/products")).body(newRequest("SKU-DEL-1")),
                PRODUCT_RESPONSE).getBody().data();

        var deleteResponse = rest.exchange(
                RequestEntity.method(HttpMethod.DELETE, URI.create(baseUri() + "/api/v1/products/" + created.id())).build(),
                PRODUCT_RESPONSE);

        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(deleteResponse.getBody().success()).isTrue();

        var afterDelete = rest.exchange(
                RequestEntity.method(HttpMethod.GET, URI.create(baseUri() + "/api/v1/products/" + created.id())).build(),
                PRODUCT_RESPONSE);
        assertThat(afterDelete.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void listAll_returnsPaginationMetadata() {
        rest.exchange(
                RequestEntity.post(URI.create(baseUri() + "/api/v1/products")).body(newRequest("SKU-LIST-1")),
                PRODUCT_RESPONSE);
        rest.exchange(
                RequestEntity.post(URI.create(baseUri() + "/api/v1/products")).body(newRequest("SKU-LIST-2")),
                PRODUCT_RESPONSE);

        var response = rest.exchange(
                RequestEntity.method(HttpMethod.GET,
                        URI.create(baseUri() + "/api/v1/products?page=0&size=1&sort=createdAt,desc")).build(),
                PRODUCT_LIST_RESPONSE);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().data()).hasSize(1);
        assertThat(response.getBody().pagination()).isNotNull();
        assertThat(response.getBody().pagination().size()).isEqualTo(1);
        assertThat(response.getBody().pagination().totalElements()).isGreaterThanOrEqualTo(2);
    }
}
