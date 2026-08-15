package com.yoursweakfoe.common.ddd.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * PageResult 边界情况测试。
 */
class PageResultBoundaryTest {

    @Test
    void totalPages_withZeroTotal_shouldReturnZero() {
        PageResult<String> result = new PageResult<>(List.of(), 0, 1, 20);
        assertThat(result.totalPages()).isEqualTo(0);
    }

    @Test
    void totalPages_withZeroPageSize_shouldReturnZero() {
        PageResult<String> result = new PageResult<>(List.of(), 100, 1, 0);
        assertThat(result.totalPages()).isEqualTo(0);
    }

    @Test
    void totalPages_exactDivision() {
        PageResult<String> result = new PageResult<>(List.of(), 100, 1, 20);
        assertThat(result.totalPages()).isEqualTo(5);
    }

    @Test
    void totalPages_withRemainder() {
        PageResult<String> result = new PageResult<>(List.of(), 101, 1, 20);
        assertThat(result.totalPages()).isEqualTo(6);
    }

    @Test
    void hasNext_shouldBeTrueWhenNotLastPage() {
        PageResult<String> result = new PageResult<>(List.of("a"), 100, 1, 20);
        assertThat(result.hasNext()).isTrue();
    }

    @Test
    void hasNext_shouldBeFalseOnLastPage() {
        PageResult<String> result = new PageResult<>(List.of("a"), 100, 5, 20);
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    void hasPrevious_shouldBeFalseOnFirstPage() {
        PageResult<String> result = new PageResult<>(List.of("a"), 100, 1, 20);
        assertThat(result.hasPrevious()).isFalse();
    }

    @Test
    void hasPrevious_shouldBeTrueOnSecondPage() {
        PageResult<String> result = new PageResult<>(List.of("a"), 100, 2, 20);
        assertThat(result.hasPrevious()).isTrue();
    }

    @Test
    void map_shouldTransformRecords() {
        PageResult<Integer> result = new PageResult<>(List.of(1, 2, 3), 3, 1, 20);
        PageResult<String> mapped = result.map(String::valueOf);

        assertThat(mapped.records()).containsExactly("1", "2", "3");
        assertThat(mapped.total()).isEqualTo(3);
        assertThat(mapped.pageNum()).isEqualTo(1);
        assertThat(mapped.pageSize()).isEqualTo(20);
    }

    @Test
    void map_emptyRecords() {
        PageResult<Integer> result = new PageResult<>(List.of(), 0, 1, 20);
        PageResult<String> mapped = result.map(String::valueOf);

        assertThat(mapped.records()).isEmpty();
        assertThat(mapped.total()).isEqualTo(0);
    }
}
