// R15 / AC-1「正确放置必通过」姊妹件 —— 对币种入对槽位 + 非 ID 调用位不受误伤，javac 必过。
//
// 本件住 src/test/resources/compile-proofs/（Maven 永不编译，纯取证件），自包含。
// 取证命令（构建 common-ddd 后，仓库根执行；期望退出码 0；与 Probe 件须分开目录编译——
// 两件各带同名币种教例，同包同出参目录会互撞）：
//   javac -encoding UTF-8 -J-Duser.language=en ^
//         -cp ywf-ddd-common/common-ddd/target/classes ^
//         -d <空目录> sample-application/sample-service/sample-service-server/src/test/resources/compile-proofs/IdMixingCompilePass.java
// 实录回填 → 案卷 implement §3（AC-1 第二、三断言）。
import com.yoursweakfoe.common.ddd.domain.id.Identifier;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/** 订单聚合币种（与 Probe 件同形，两文件各自独立编译）。 */
record OrderId(UUID value) implements Identifier<UUID>, Serializable {}

/** 商品聚合币种。 */
record ProductId(UUID value) implements Identifier<UUID>, Serializable {}

/** 写端口形状参考：ID 槽期待 OrderId，另有非 ID 业务参数位。 */
interface OrderPortLike {
    void findById(OrderId id);

    int countByOrderNo(String orderNo);
}

final class IdMixingCompilePass {

    private IdMixingCompilePass() {}

    static void correctUse(OrderPortLike orderPort, OrderId rightCurrency) {
        // AC-1 第二断言：对币种入对槽位必须编译通过（防线不是死闸）。
        orderPort.findById(rightCurrency);
    }

    static void nonIdSlotsUntouched(OrderPortLike orderPort, String orderNo) {
        // AC-1 第三断言：非 ID 调用位（String 业务参数）与币种无涉，不受误伤。
        Objects.requireNonNull(orderNo);
        orderPort.countByOrderNo(orderNo);
    }
}
