package com.yoursweakfoe.common.ddd.application.assembler;

import static org.assertj.core.api.Assertions.assertThat;

import com.yoursweakfoe.common.ddd.application.presenter.BasicPresenter;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * BasicAssembler / BasicPresenter default 集合方法单元测试 + 装配方向回潮守卫。
 */
class BasicAssemblerPresenterTest {

    // 测试用简单 Assembler（Integer → String 映射；接口为单向最小契约，仅需实现 toDTO）
    static class TestAssembler implements BasicAssembler<Integer, String> {
        @Override public String toDTO(Integer domain) { return domain.toString(); }
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

    /**
     * 回潮守卫 —— BasicAssembler 公开契约永久单向（Domain → DTO）。
     *
     * <p>教义全貌见 {@link BasicAssembler} 类 javadoc：聚合构造只有 Factory 与 reconstitute
     * 两扇门，DTO → Domain 是死方向，曾以 {@code toDomain}/{@code toDomainList}/
     * {@code toDomainSet} 存在于接口、令全部实现者写 throw 样板，已切除。本断言是该切除的
     * 机器锁：任何人把 toDomain 系方法加回接口，此处立即红——防止「先加回接口再补实现」的
     * 回流路径绕过教义讨论。反射断言（而非编译依赖）保证守卫自身不随接口签名演化而腐烂。
     */
    @Nested
    class AssemblerDirectionGuard {

        @Test
        void basicAssembler_declaresOnlyDomainToDtoDirection() {
            assertThat(Arrays.stream(BasicAssembler.class.getDeclaredMethods())
                    .map(Method::getName))
                    .as("BasicAssembler 不得声明任何 toDomain 系方法（DTO→Domain 死方向，教义见接口 javadoc）")
                    .noneMatch(name -> name.startsWith("toDomain"));

            assertThat(Arrays.stream(BasicAssembler.class.getDeclaredMethods())
                    .map(Method::getName))
                    .as("toDTO 唯一抽象方向必须在位（防空接口使上方断言虚假成立）")
                    .contains("toDTO");
        }
    }
}
