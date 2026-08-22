package com.yoursweakfoe.common.ddd.application.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PageResult — 分页结果容器测试")
class PageResultTest {

    @Test
    void totalPages_calculation() {
        PageResult<String> page = new PageResult<>(List.of("a", "b"), 5, 1, 2);
        assertThat(page.totalPages()).isEqualTo(3);
    }

    @Test
    void totalPages_exactDivision() {
        PageResult<String> page = new PageResult<>(List.of("a", "b"), 4, 1, 2);
        assertThat(page.totalPages()).isEqualTo(2);
    }

    @Test
    void totalPages_zeroPageSize() {
        PageResult<String> page = new PageResult<>(List.of(), 0, 1, 0);
        assertThat(page.totalPages()).isEqualTo(0);
    }

    @Test
    void hasNext_true() {
        PageResult<String> page = new PageResult<>(List.of("a"), 3, 1, 1);
        assertThat(page.hasNext()).isTrue();
    }

    @Test
    void hasNext_false_lastPage() {
        PageResult<String> page = new PageResult<>(List.of("a"), 3, 3, 1);
        assertThat(page.hasNext()).isFalse();
    }

    @Test
    void hasPrevious_false_firstPage() {
        PageResult<String> page = new PageResult<>(List.of("a"), 3, 1, 1);
        assertThat(page.hasPrevious()).isFalse();
    }

    @Test
    void hasPrevious_true() {
        PageResult<String> page = new PageResult<>(List.of("a"), 3, 2, 1);
        assertThat(page.hasPrevious()).isTrue();
    }

    @Test
    void map_transformsRecords_preservesMetadata() {
        PageResult<Integer> page = new PageResult<>(List.of(1, 2, 3), 10, 2, 3);

        PageResult<String> mapped = page.map(i -> "item-" + i);

        assertThat(mapped.records()).containsExactly("item-1", "item-2", "item-3");
        assertThat(mapped.total()).isEqualTo(10);
        assertThat(mapped.pageNum()).isEqualTo(2);
        assertThat(mapped.pageSize()).isEqualTo(3);
    }

    @Test
    void map_emptyRecords() {
        PageResult<Integer> page = new PageResult<>(List.of(), 0, 1, 20);

        PageResult<String> mapped = page.map(Object::toString);

        assertThat(mapped.records()).isEmpty();
        assertThat(mapped.total()).isEqualTo(0);
    }
}
