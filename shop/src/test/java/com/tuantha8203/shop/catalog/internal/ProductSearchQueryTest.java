package com.tuantha8203.shop.catalog.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.text.Normalizer;
import org.junit.jupiter.api.Test;

class ProductSearchQueryTest {

    @Test
    void everyWordBecomesAPrefixAndAllMustMatch() {
        assertThat(ProductService.toPrefixQuery("Áo  Thun")).isEqualTo("áo:* & thun:*");
    }

    @Test
    void tsquerySyntaxTypedByTheUserIsRemoved() {
        assertThat(ProductService.toPrefixQuery("ao & (thun | !jean)")).isEqualTo("ao:* & thun:* & jean:*");
        assertThat(ProductService.toPrefixQuery("!!! ???")).isEmpty();
    }

    @Test
    void decomposedVietnameseIsComposedFirst() {
        String decomposed = Normalizer.normalize("áo", Normalizer.Form.NFD); // 'a' + combining acute accent
        assertThat(decomposed).hasSize(3);

        assertThat(ProductService.toPrefixQuery(decomposed)).isEqualTo("áo:*");
    }

    @Test
    void longInputIsCappedAtEightWords() {
        assertThat(ProductService.toPrefixQuery("a b c d e f g h i j").split("&"))
                .hasSize(8);
    }

    @Test
    void theExactQueryIsTheSameWordsWithoutPrefixes() {
        assertThat(ProductService.toExactQuery("Áo  Thun!!")).isEqualTo("áo & thun");
    }
}
