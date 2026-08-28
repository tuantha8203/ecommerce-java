package tuantha8203.searchservice.api;

import tuantha8203.searchservice.domain.ProductDocument;
import tuantha8203.searchservice.infrastructure.ProductSearchRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class SearchController {

    private final ProductSearchRepository repository;

    public SearchController(ProductSearchRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/api/v1/search/products")
    public List<ProductDocument> search(@RequestParam String q) {
        return repository.findByNameContainingIgnoreCase(q);
    }
}