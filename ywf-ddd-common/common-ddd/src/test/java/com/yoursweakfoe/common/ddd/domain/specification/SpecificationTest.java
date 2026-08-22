package com.yoursweakfoe.common.ddd.domain.specification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Specification — 可组合领域谓词")
class SpecificationTest {

    private final Specification<Integer> isPositive = n -> n > 0;
    private final Specification<Integer> isEven = n -> n % 2 == 0;

    @Test
    void and_bothSatisfied_true() {
        assertThat(isPositive.and(isEven).isSatisfiedBy(4)).isTrue();
    }

    @Test
    void and_oneViolated_false() {
        assertThat(isPositive.and(isEven).isSatisfiedBy(3)).isFalse(); // positive but odd
        assertThat(isPositive.and(isEven).isSatisfiedBy(-2)).isFalse(); // even but negative
    }

    @Test
    void or_eitherSatisfied_true() {
        assertThat(isPositive.or(isEven).isSatisfiedBy(3)).isTrue();    // positive
        assertThat(isPositive.or(isEven).isSatisfiedBy(-4)).isTrue();   // even
    }

    @Test
    void or_neither_false() {
        assertThat(isPositive.or(isEven).isSatisfiedBy(-3)).isFalse();
    }

    @Test
    void not_inverts() {
        assertThat(isPositive.not().isSatisfiedBy(-1)).isTrue();
        assertThat(isPositive.not().isSatisfiedBy(1)).isFalse();
    }

    @Test
    void composition_isChainable() {
        // isEven && !isPositive → 非正偶数（0, -2, -4 ...）
        Specification<Integer> nonPositiveEven = isEven.and(isPositive.not());
        assertThat(nonPositiveEven.isSatisfiedBy(-2)).isTrue();
        assertThat(nonPositiveEven.isSatisfiedBy(2)).isFalse();
        assertThat(nonPositiveEven.isSatisfiedBy(-3)).isFalse();
    }

    @Test
    void nullCandidate_shortCircuits_false() {
        assertThat(isPositive.and(isEven).isSatisfiedBy(null)).isFalse();
        assertThat(isPositive.or(isEven).isSatisfiedBy(null)).isFalse();
        assertThat(isPositive.not().isSatisfiedBy(null)).isFalse();
    }

    @Test
    void nullSpec_operator_throws() {
        assertThatThrownBy(() -> isPositive.and(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void composition_appliesToCollection() {
        // 纯校验语义：规格作为集合成员的判定谓词（不涉及读侧 LambdaQueryWrapper）
        Specification<Integer> spec = isPositive.and(isEven).not();
        List<Integer> result = List.of(1, 2, 3, 4, -2, 5).stream()
                .filter(spec::isSatisfiedBy)
                .toList();
        assertThat(result).containsExactly(1, 3, -2, 5);
    }
}