# 模块用法法卷：common-security（框架法 · 严格件）

> **身份**：本卷是用法规范与规范代码形状的**唯一权威**（严格件）——消费代码必须遵循，违反本卷=修代码；修卷只走 `../../changes/` 程序。docs 同题节（`reference/api/common-security.md` §3）为宽松件：语感与指针，冲突以本卷为准（宽严双份，2026-09-06）。
> **机器对账**：本卷在 check-docs C1（`{agg}` 模板实例化）/ C3（符号解析）/ C4（教学中立）扫描面内。开册法案：2026-09 框架成典案。

---

```xml
<dependency>
    <groupId>com.yoursweakfoe</groupId>
    <artifactId>common-security</artifactId>
</dependency>
```

### 配置：JwtDecoder（必配）

- **非对称（JWKS）**：
  ```yaml
  spring:
    security:
      oauth2:
        resourceserver:
          jwt:
            jwk-set-uri: https://${IDP_HOST}/.well-known/jwks.json
  ```
- **对称密钥（HMAC，公司现状）**：自定义 Bean
  ```java
  @Bean
  JwtDecoder jwtDecoder() {
      SecretKey key = new SecretKeySpec(Base64.getDecoder().decode(secret), "HmacSHA256");
      return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
  }
  ```
- **多方案**：见 §2 的 `DelegatingJwtDecoder`。

### 配置：角色 claim 名 + 权限前缀（可选，唯一字段缝）

```yaml
ywf:
  security:
    enabled: true            # 安全链总开关，默认 true（缺省启用）
    roles-claim: roles       # 角色列表所在的 claim 名，默认 roles
    authority-prefix: ROLE_  # 角色 → 权限前缀，默认 ROLE_
```

其余身份字段不配置、不写死——各服务按名字自取。

> **opt-out 门控**：`ywf.security.enabled=false` 时整条安全链不注册（无 `SecurityFilterChain`、
> 不启用 `@EnableWebSecurity`/`@EnableMethodSecurity`）。面向「不想要安全链、只想复用 `SecurityUtil`
> 读 JWT」的消费方（如纯内部服务）。`SecurityUtil` 是静态工具类，与安全链无耦合，关闭后仍可用。

### 场景 1：获取当前用户身份（字段自取）

```java
// 契约接口（真实 OrderController）：身份不进方法签名——principal 由安全链承载，Controller 纯透传
@GetMapping("/orders/{orderId}")
OrderCO getOrder(@PathVariable("orderId") UUID orderId);

// Application / Adapter 层：SecurityUtil 按名字自取（domain 层禁止——R6 领域不感知认证上下文）
String userId = SecurityUtil.getString("uid");   // 或 "sub" / "user_id" / 任意你们的名字
{Agg} agg = new {Agg}(command, userId);          // 教学占位例（{Agg} = 聚合根类名占位）：审计字段、数据归属
```

### 场景 2：方法级鉴权

```java
@PreAuthorize("hasRole('ADMIN')")
public void approve(OrderCommand command) { ... }

// 数据归属判断（域数据依赖，无法上浮网关）——agg 为场景 1 声明的 {Agg} 占位聚合实例
boolean isOwner = agg.getCustomerId().equals(SecurityUtil.getString("uid"));
boolean isAdmin = SecurityUtil.getStringList("roles").contains("ADMIN");
```

### 场景 3：自定义安全链

```java
@Configuration
@EnableWebSecurity
class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationConverter converter) {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
            .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(converter)))
            .authorizeHttpRequests(a -> a
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().permitAll());
        return http.build();
    }
}
```
