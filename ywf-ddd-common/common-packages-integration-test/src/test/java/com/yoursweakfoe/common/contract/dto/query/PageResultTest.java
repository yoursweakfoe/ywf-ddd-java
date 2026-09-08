package com.yoursweakfoe.common.contract.dto.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PageResult — 分页结果容器测试")
class PageResultTest {

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

    @Test
    @DisplayName("防御性拷贝：外部可变 List 构造后再修改，容器不受污染")
    void defensiveCopy_externalMutableList_isolated() {
        var mutable = new ArrayList<>(List.of("a", "b"));
        PageResult<String> page = new PageResult<>(mutable, 2, 1, 2);

        mutable.add("c"); // 构造后外部再改

        assertThat(page.records()).containsExactly("a", "b");
    }

    @Test
    @DisplayName("records() 返回不可变视图：add 抛 UnsupportedOperationException")
    void recordsView_unmodifiable() {
        PageResult<String> page = new PageResult<>(List.of("a"), 1, 1, 1);

        assertThatThrownBy(() -> page.records().add("x"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("null records 归一为空列表")
    void nullRecords_normalizedToEmpty() {
        PageResult<String> page = new PageResult<>(null, 0, 1, 20);

        assertThat(page.records()).isEmpty();
        assertThat(page.total()).isZero();
    }
}
