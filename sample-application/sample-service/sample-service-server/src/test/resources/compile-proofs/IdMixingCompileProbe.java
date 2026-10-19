// R15 / AC-1「混放编译锁」取证件 —— 他聚合 ID 传入本聚合 ID 调用位，javac 必拒。
//
// 本件住 src/test/resources/compile-proofs/（Maven 永不编译，纯取证件），自包含、
// 仅依赖 common-ddd 身份词汇（法卷锚 BP-13／案卷 2026-09-typed-identifier）。
// 取证命令（构建 common-ddd 后，仓库根执行；期望退出码非 0）：
//   javac -encoding UTF-8 -J-Duser.language=en ^
//         -cp ywf-ddd-common/common-ddd/target/classes ^
//         -d <空目录> sample-application/sample-service/sample-service-server/src/test/resources/compile-proofs/IdMixingCompileProbe.java
// 实录回填 → 案卷 implement §3（AC-1）。
import com.yoursweakfoe.common.ddd.domain.id.Identifier;
import java.io.Serializable;
import java.util.UUID;

/** 订单聚合币种（本件虚构教例，与 sample 真实迁移无关）。 */
record OrderId(UUID value) implements Identifier<UUID>, Serializable {}

/** 商品聚合币种（本件虚构教例）。 */
record ProductId(UUID value) implements Identifier<UUID>, Serializable {}

/** 写端口形状参考：ID 槽期待 OrderId。 */
interface OrderPortLike {
    void findById(OrderId id);
}

final class IdMixingCompileProbe {

    private IdMixingCompileProbe() {}

    static void misuse(OrderPortLike orderPort, ProductId wrongCurrency) {
        // 期望编译错误：incompatible types: ProductId cannot be converted to OrderId
        // ——混放从运行时提前到编译期，即 AC-1 第一断言。
        orderPort.findById(wrongCurrency);
    }
}
