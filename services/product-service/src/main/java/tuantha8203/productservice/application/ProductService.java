package tuantha8203.productservice.application;

import tuantha8203.productservice.api.ProductRequest;
import tuantha8203.productservice.api.ProductResponse;
import tuantha8203.productservice.domain.Product;
import tuantha8203.productservice.domain.ProductRepositoryPort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class ProductService {

    private final ProductRepositoryPort repository;
    private final ProductMapper mapper;
    private final ProductEventPublisher eventPublisher;

    public ProductService(ProductRepositoryPort repository, ProductMapper mapper, ProductEventPublisher eventPublisher) {
        this.repository = repository;
        this.mapper = mapper;
        this.eventPublisher = eventPublisher;
    }

    public ProductResponse create(ProductRequest request) {
        Product saved = repository.save(mapper.toEntity(request));
        eventPublisher.publishUpdated(saved);
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ProductResponse getById(UUID id) {
        return mapper.toResponse(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> listAll(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toResponse);
    }

    public ProductResponse update(UUID id, ProductRequest request) {
        Product product = findOrThrow(id);
        mapper.updateEntity(product, request);
        eventPublisher.publishUpdated(product);
        return mapper.toResponse(product);
    }

    public void delete(UUID id) {
        repository.delete(findOrThrow(id));
        eventPublisher.publishDeleted(id);
    }

    private Product findOrThrow(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }
}