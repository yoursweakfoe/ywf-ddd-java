package com.yoursweakfoe.common.ddd.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.yoursweakfoe.common.ddd.application.assembler.BasicAssembler;
import com.yoursweakfoe.common.ddd.application.presenter.BasicPresenter;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * BasicAssembler / BasicPresenter default 集合方法单元测试。
 */
class BasicAssemblerPresenterTest {

    // 测试用简单 Assembler（String ↔ Integer 映射）
    static class TestAssembler implements BasicAssembler<Integer, String> {
        @Override public Integer toDomain(String dto) { return Integer.parseInt(dto); }
        @Override public String toDTO(Integer domain) { return domain.toString(); }
        @Override public void updateDomain(String dto, Integer domain) { throw new UnsupportedOperationException(); }
        @Override public void updateDTO(Integer domain, String dto) { throw new UnsupportedOperationException(); }
    }

    // 测试用简单 Presenter（Integer → Long 映射）
    static class TestPresenter implements BasicPresenter<Integer, Long> {
        @Override public Long present(Integer dto) { return dto.longValue(); }
    }

    private final TestAssembler assembler = new TestAssembler();
    private final TestPresenter presenter = new TestPresenter();

    @Nested
    class AssemblerListMethods {

        @Test
        void toDomainList_shouldMapAllElements() {
            List<Integer> result = assembler.toDomainList(List.of("1", "2", "3"));
            assertThat(result).containsExactly(1, 2, 3);
        }

        @Test
        void toDomainList_emptyList_shouldReturnEmpty() {
            assertThat(assembler.toDomainList(List.of())).isEmpty();
        }

        @Test
        void toDTOList_shouldMapAllElements() {
            List<String> result = assembler.toDTOList(List.of(10, 20));
            assertThat(result).containsExactly("10", "20");
        }

        @Test
        void toDTOList_emptyList_shouldReturnEmpty() {
            assertThat(assembler.toDTOList(List.of())).isEmpty();
        }
    }

    @Nested
    class AssemblerSetMethods {

        @Test
        void toDomainSet_shouldMapAllElements() {
            Set<Integer> result = assembler.toDomainSet(Set.of("5", "6"));
            assertThat(result).containsExactlyInAnyOrder(5, 6);
        }

        @Test
        void toDTOSet_shouldMapAllElements() {
            Set<String> result = assembler.toDTOSet(Set.of(7, 8));
            assertThat(result).containsExactlyInAnyOrder("7", "8");
        }
    }

    @Nested
    class PresenterMethods {

        @Test
        void presentList_shouldMapAllElements() {
            List<Long> result = presenter.presentList(List.of(1, 2, 3));
            assertThat(result).containsExactly(1L, 2L, 3L);
        }

        @Test
        void presentList_emptyList_shouldReturnEmpty() {
            assertThat(presenter.presentList(List.of())).isEmpty();
        }

        @Test
        void present_singleElement() {
            assertThat(presenter.present(42)).isEqualTo(42L);
        }
    }
}
