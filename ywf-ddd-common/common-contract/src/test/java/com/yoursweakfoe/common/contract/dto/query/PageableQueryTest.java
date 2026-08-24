package com.yoursweakfoe.common.contract.dto.query;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * PageableQuery 契约测试 —— 验证 record 天然实现（零覆写样板）与防御性钳制行为。
 *
 * <p>本测试是「record 组件访问器可满足同签名抽象接口方法」的编译期 + 运行期双重证明：
 * {@link SamplePageQuery} 未显式实现任何方法，若接口抽象方法与 record 访问器签名不匹配，
 * 本测试类将无法编译。
 */
class PageableQueryTest {

    /** 样例分页 record：组件名 pageNum/pageSize 与契约抽象方法天然匹配，零覆写。 */
    private record SamplePageQuery(String keyword, int pageNum, int pageSize)
            implements PageableQuery {}

    /** 非 record 实现类：显式覆写访问器同样满足契约（逃生门验证）。 */
    @SuppressWarnings("ClassCanBeRecord")
    private static final class ClassicPageQuery implements PageableQuery {
        private final int pageNum;
        private final int pageSize;

        ClassicPageQuery(int pageNum, int pageSize) {
            this.pageNum = pageNum;
            this.pageSize = pageSize;
        }

        @Override
        public int pageNum() {
            return pageNum;
        }

        @Override
        public int pageSize() {
            return pageSize;
        }
    }

    // ==================== record 天然实现 ====================

    @Test
    void record_withMatchingComponents_implementsContractWithoutOverrides() {
        PageableQuery query = new SamplePageQuery("kw", 3, 50);

        assertThat(query.pageNum()).isEqualTo(3);
        assertThat(query.pageSize()).isEqualTo(50);
    }

    // ==================== safePageNum 钳制 ====================

    @Test
    void safePageNum_zeroOrNegative_clampedToOne() {
        assertThat(new SamplePageQuery("kw", 0, 20).safePageNum()).isEqualTo(1);
        assertThat(new SamplePageQuery("kw", -5, 20).safePageNum()).isEqualTo(1);
    }

    @Test
    void safePageNum_positive_passthrough() {
        assertThat(new SamplePageQuery("kw", 1, 20).safePageNum()).isEqualTo(1);
        assertThat(new SamplePageQuery("kw", 42, 20).safePageNum()).isEqualTo(42);
    }

    // ==================== safePageSize 钳制 ====================

    @Test
    void safePageSize_nonPositive_clampedToOne() {
        assertThat(new SamplePageQuery("kw", 1, 0).safePageSize()).isEqualTo(1);
        assertThat(new SamplePageQuery("kw", 1, -100).safePageSize()).isEqualTo(1);
    }

    @Test
    void safePageSize_withinBounds_passthrough() {
        assertThat(new SamplePageQuery("kw", 1, 1).safePageSize()).isEqualTo(1);
        assertThat(new SamplePageQuery("kw", 1, PageableQuery.DEFAULT_PAGE_SIZE).safePageSize())
                .isEqualTo(PageableQuery.DEFAULT_PAGE_SIZE);
    }

    @Test
    void safePageSize_overLimit_clampedToMaxPageSize() {
        assertThat(new SamplePageQuery("kw", 1, 5000).safePageSize())
                .isEqualTo(PageableQuery.MAX_PAGE_SIZE);
    }

    // ==================== 原始值逃生门 + 非 record 实现 ====================

    @Test
    void rawPageSize_overLimit_notClamped_escapeHatchForExport() {
        SamplePageQuery exportQuery = new SamplePageQuery("kw", 1, 5000);

        assertThat(exportQuery.pageSize()).isEqualTo(5000);     // 原始值不钳制（批量导出通道）
        assertThat(exportQuery.safePageSize()).isEqualTo(1000); // 安全值钳制（常规读侧通道）
    }

    @Test
    void classicClass_explicitOverrides_alsoSatisfiesContract() {
        PageableQuery query = new ClassicPageQuery(2, 30);

        assertThat(query.safePageNum()).isEqualTo(2);
        assertThat(query.safePageSize()).isEqualTo(30);
    }
}
