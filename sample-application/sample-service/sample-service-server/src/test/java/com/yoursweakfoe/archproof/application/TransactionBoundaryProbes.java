package com.yoursweakfoe.archproof.application;

import com.yoursweakfoe.common.contract.dto.command.Command;
import com.yoursweakfoe.common.ddd.application.handler.command.CommandHandler;
import org.springframework.transaction.annotation.Transactional;

/**
 * R11「@Transactional ∧ 显式 rollbackFor」负证明夹具（虚构教例，sample 未实现业务形态）。
 *
 * <p>位于 {@code com.yoursweakfoe.archproof.application} 包——两个 {@code @AnalyzeClasses}
 * 扫描根之外，仅由 {@code TransactionBoundaryRuleProofTest} 定向导入，不污染常规架构扫描。
 * 四形态各证一锁：裸标必咬（默认回滚规则的 checked-exception 缝）／漏标必咬（存量语义回归）／
 * 显式标注必放（教义合法路不是死闸）／非 Handler 不咬（主语锚点不外溢）。
 */
public final class TransactionBoundaryProbes {

    private TransactionBoundaryProbes() {}

    /** 探针命令（test-tree 虚构教例载体）。 */
    public record ProbeCommand(String payload) implements Command {}

    /** 裸标 @Transactional——R11 必须咬（未显式声明 rollbackFor）。 */
    public static class BareTransactionalProbe implements CommandHandler<ProbeCommand, String> {
        @Override
        @Transactional
        public String handle(ProbeCommand command) {
            return "bare";
        }
    }

    /** 显式 rollbackFor——教义合规形态，必须放行。 */
    public static class RollbackDeclaredProbe implements CommandHandler<ProbeCommand, String> {
        @Override
        @Transactional(rollbackFor = Exception.class)
        public String handle(ProbeCommand command) {
            return "declared";
        }
    }

    /** 漏标注解——增强前后都必须咬（存量语义回归哨兵）。 */
    public static class NoTransactionalProbe implements CommandHandler<ProbeCommand, String> {
        @Override
        public String handle(ProbeCommand command) {
            return "missing";
        }
    }

    /** 非 CommandHandler 实现、handle 同名且裸标——主语不罩它，必须放行（锚点不外溢证明）。 */
    public static class NonHandlerProbe {
        @Transactional
        public String handle(ProbeCommand command) {
            return "not-a-handler";
        }
    }
}
