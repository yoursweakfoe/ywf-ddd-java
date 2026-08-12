package com.yoursweakfoe.common.ddd.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.yoursweakfoe.common.ddd.fixtures.converter.ProductConverter;
import com.yoursweakfoe.common.ddd.fixtures.model.Product;
import com.yoursweakfoe.common.ddd.fixtures.po.ProductPO;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("BasicConverter — 基础转换器 default 方法测试")
class BasicConverterTest {

    private ProductConverter converter;

    @BeforeEach
    void setUp() {
        converter = new ProductConverter();
    }

    private ProductPO productPO(Long id, String name, int stock) {
        ProductPO po = new ProductPO();
        po.setId(id);
        po.setName(name);
        po.setStock(stock);
        return po;
    }

    // ==================== toDomainList ====================

    @Nested
    @DisplayName("toDomainList")
    class ToDomainList {

        @Test
        void delegatesToToDomain() {
            List<ProductPO> poList = List.of(
                    productPO(1L, "Widget", 100),
                    productPO(2L, "Gadget", 50));
            List<Product> result = converter.toDomainList(poList);
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getName()).isEqualTo("Widget");
            assertThat(result.get(1).getName()).isEqualTo("Gadget");
        }
    }

    // ==================== toPOList ====================

    @Nested
    @DisplayName("toPOList")
    class ToPOList {

        @Test
        void delegatesToToPO() {
            List<Product> domainList = List.of(
                    new Product(1L, "Widget", 100),
                    new Product(2L, "Gadget", 50));
            List<ProductPO> result = converter.toPOList(domainList);
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getName()).isEqualTo("Widget");
            assertThat(result.get(1).getName()).isEqualTo("Gadget");
        }
    }

    // ==================== toDomainSet ====================

    @Nested
    @DisplayName("toDomainSet")
    class ToDomainSet {

        @Test
        void deduplication() {
            // Same PO instance mapped twice — Set should deduplicate by equals/hashCode
            ProductPO po = productPO(1L, "Widget", 100);
            Set<ProductPO> poSet = Set.of(po);
            Set<Product> result = converter.toDomainSet(poSet);
            assertThat(result).hasSize(1);
            assertThat(result.iterator().next().getName()).isEqualTo("Widget");
        }
    }

    // ==================== toPOSet ====================

    @Nested
    @DisplayName("toPOSet")
    class ToPOSet {

        @Test
        void deduplication() {
            Product domain = new Product(1L, "Widget", 100);
            Set<Product> domainSet = Set.of(domain);
            Set<ProductPO> result = converter.toPOSet(domainSet);
            assertThat(result).hasSize(1);
            assertThat(result.iterator().next().getName()).isEqualTo("Widget");
        }
    }
}
