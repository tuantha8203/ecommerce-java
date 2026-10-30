package com.tuantha8203.shop.catalog.internal;

import com.tuantha8203.shop.catalog.ProductStatus;
import com.tuantha8203.shop.catalog.ProductSummary;
import com.tuantha8203.shop.shared.PageResponse;
import com.tuantha8203.shop.shared.PreconditionFailedException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/admin/products")
@RequiredArgsConstructor
class AdminProductController {

    private final ProductService products;

    @PostMapping
    ResponseEntity<ProductDetails> create(
            @Valid @RequestBody CreateProductRequest request, UriComponentsBuilder uriBuilder) {
        ProductDetails created = products.create(request);
        URI location = uriBuilder
                .path("/api/v1/admin/products/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location)
                .eTag(String.valueOf(created.version()))
                .body(created);
    }

    @GetMapping("/{id}")
    ResponseEntity<ProductDetails> get(@PathVariable long id) {
        ProductDetails product = products.getById(id);
        return ResponseEntity.ok().eTag(String.valueOf(product.version())).body(product);
    }

    @PutMapping("/{id}")
    ResponseEntity<ProductDetails> update(
            @PathVariable long id,
            @RequestHeader(HttpHeaders.IF_MATCH) String ifMatch,
            @Valid @RequestBody UpdateProductRequest request) {
        ProductDetails updated = products.update(id, versionFrom(ifMatch), request);
        return ResponseEntity.ok().eTag(String.valueOf(updated.version())).body(updated);
    }

    @PostMapping("/{id}/activation")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void activate(@PathVariable long id) {
        products.activate(id);
    }

    @PostMapping("/{id}/archival")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void archive(@PathVariable long id) {
        products.archive(id);
    }

    @GetMapping
    PageResponse<ProductSummary> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) ProductStatus status) {
        return products.listForAdmin(status, page, size);
    }

    /** Accepts "3", "\"3\"" and W/"3". */
    private static long versionFrom(String ifMatch) {
        String value = ifMatch.replace("W/", "").replace("\"", "").trim();
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            throw new PreconditionFailedException("Invalid If-Match header: " + ifMatch);
        }
    }
}
