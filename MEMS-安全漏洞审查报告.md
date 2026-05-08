# MEMS 设备维护管理系统 — 安全漏洞审查报告

## 漏洞1：Mass Assignment 攻击（批量赋值漏洞）

### 问题描述

`MaintenanceController.create()` 方法直接将 `@RequestBody` 绑定到 JPA 实体 `MaintenanceRecord`，攻击者可以在 JSON 请求体中注入任意字段值，包括本不应由用户控制的 `id`、`maintenanceDate`、`status` 等字段。

根据业务规则，`maintenanceDate` 一旦创建后不应被篡改，但当前实现中，攻击者可以在创建时随意指定 `maintenanceDate`，也可以在后续的更新请求中覆盖该字段。此外，攻击者还可以通过注入 `id` 字段覆盖已有记录，或通过 `status` 字段绕过业务状态机。

**核心危害：**
- 篡改维护日期，伪造维护记录的时间戳
- 通过注入 `id` 覆盖已有记录
- 绕过状态流转规则，直接设置非法状态
- 修改 `technician` 字段冒充他人操作

### 攻击场景

攻击者（普通技术员）发送如下请求，篡改维护日期并冒充其他技术员：

```json
POST /api/maintenance
Content-Type: application/json

{
  "id": 5,
  "equipmentId": 100,
  "technician": "高级工程师张某",
  "description": "例行检查",
  "maintenanceDate": "2020-01-01T00:00:00",
  "status": "COMPLETED"
}
```

如果 `id=5` 的记录已存在，JPA 会执行 `UPDATE` 而非 `INSERT`，导致原有记录被覆盖，且 `maintenanceDate` 被篡改为 2020 年的日期。

### 修复代码

**1. 创建请求 DTO，严格限制可输入字段**

```java
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public class MaintenanceRecordCreateRequest {

    @NotNull(message = "设备ID不能为空")
    private Long equipmentId;

    @NotBlank(message = "技术员不能为空")
    private String technician;

    @NotBlank(message = "描述不能为空")
    private String description;

    public Long getEquipmentId() { return equipmentId; }
    public void setEquipmentId(Long equipmentId) { this.equipmentId = equipmentId; }
    public String getTechnician() { return technician; }
    public void setTechnician(String technician) { this.technician = technician; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
```

```java
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class MaintenanceRecordUpdateRequest {

    @NotBlank(message = "描述不能为空")
    private String description;

    private String status;

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
```

**2. 修改 Controller，使用 DTO 替代直接绑定实体**

```java
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/maintenance")
public class MaintenanceController {

    private final MaintenanceRecordRepository repository;
    private final MaintenanceAuditLogRepository auditLogRepository;

    public MaintenanceController(MaintenanceRecordRepository repository,
                                  MaintenanceAuditLogRepository auditLogRepository) {
        this.repository = repository;
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping
    public List<MaintenanceRecord> getAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<MaintenanceRecord> getById(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<MaintenanceRecord> create(
            @Valid @RequestBody MaintenanceRecordCreateRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {

        MaintenanceRecord record = new MaintenanceRecord();
        record.setEquipmentId(request.getEquipmentId());
        record.setTechnician(request.getTechnician());
        record.setDescription(request.getDescription());
        record.setMaintenanceDate(LocalDateTime.now());
        record.setStatus("PENDING");

        MaintenanceRecord saved = repository.save(record);

        MaintenanceAuditLog auditLog = new MaintenanceAuditLog();
        auditLog.setRecordId(saved.getId());
        auditLog.setAction("CREATE");
        auditLog.setOperator(currentUser.getUsername());
        auditLog.setDetail("创建维护记录");
        auditLogRepository.save(auditLog);

        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MaintenanceRecord> update(
            @PathVariable Long id,
            @Valid @RequestBody MaintenanceRecordUpdateRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {

        return repository.findById(id)
                .map(existing -> {
                    existing.setDescription(request.getDescription());
                    if (request.getStatus() != null) {
                        existing.setStatus(request.getStatus());
                    }
                    MaintenanceRecord saved = repository.save(existing);

                    MaintenanceAuditLog auditLog = new MaintenanceAuditLog();
                    auditLog.setRecordId(saved.getId());
                    auditLog.setAction("UPDATE");
                    auditLog.setOperator(currentUser.getUsername());
                    auditLog.setDetail("修改维护记录");
                    auditLogRepository.save(auditLog);

                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails currentUser) {

        return repository.findById(id)
                .map(existing -> {
                    repository.delete(existing);

                    MaintenanceAuditLog auditLog = new MaintenanceAuditLog();
                    auditLog.setRecordId(id);
                    auditLog.setAction("DELETE");
                    auditLog.setOperator(currentUser.getUsername());
                    auditLog.setDetail("删除维护记录");
                    auditLogRepository.save(auditLog);

                    return ResponseEntity.<Void>ok().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
```

**3. 在实体层增加 `maintenanceDate` 不可变保护（乐观锁 + 字段保护）**

```java
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "maintenance_record")
public class MaintenanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "equipment_id")
    private Long equipmentId;

    private String technician;

    private String description;

    @Column(name = "maintenance_date", updatable = false)
    private LocalDateTime maintenanceDate;

    private String status;

    @Version
    private Long version;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getEquipmentId() { return equipmentId; }
    public void setEquipmentId(Long equipmentId) { this.equipmentId = equipmentId; }
    public String getTechnician() { return technician; }
    public void setTechnician(String technician) { this.technician = technician; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getMaintenanceDate() { return maintenanceDate; }
    public void setMaintenanceDate(LocalDateTime maintenanceDate) { this.maintenanceDate = maintenanceDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
```

> 关键点：`@Column(name = "maintenance_date", updatable = false)` 确保 JPA 在 UPDATE 语句中不包含 `maintenance_date` 字段；`@Version` 提供乐观锁，防止并发覆盖。

### 验证方法

1. **DTO 防护验证：** 发送包含 `id`、`maintenanceDate`、`status` 等额外字段的 POST 请求，确认返回记录中这些字段值由服务端控制，而非请求体中的值
2. **不可变验证：** 创建记录后，尝试通过 PUT 请求修改 `maintenanceDate`，确认该字段未被更新
3. **乐观锁验证：** 使用两个并发请求同时修改同一条记录，确认后提交的请求因版本冲突抛出 `OptimisticLockingFailureException`

---

## 漏洞2：权限粒度控制不足

### 问题描述

当前 `SecurityConfig` 仅做了 URL 级别的认证检查（`anyRequest().authenticated()`），完全没有基于角色的访问控制（RBAC）。根据业务规则：
- 普通技术员（`ROLE_TECHNICIAN`）只能查看和创建记录
- 管理员（`ROLE_ADMIN`）可以修改和删除记录

但当前实现中，任何已认证用户都可以调用所有接口，包括修改和删除操作。同时，缺少 PUT 和 DELETE 端点本身也是问题——虽然功能不完整，但更关键的是没有权限边界。

**核心危害：**
- 普通技术员可以修改或删除任意维护记录
- 无法区分不同角色的操作权限
- 违反最小权限原则，扩大了攻击面

### 攻击场景

普通技术员登录后，直接调用删除接口删除重要维护记录：

```
DELETE /api/maintenance/5
Authorization: Bearer <technician_token>
```

由于没有角色检查，请求会成功执行，重要维护记录被删除。

### 修复代码

**1. 完善 SecurityConfig，增加方法级安全支持与 URL 级角色控制**

```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**", "/api/public/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/maintenance/**").hasAnyRole("TECHNICIAN", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/maintenance/**").hasAnyRole("TECHNICIAN", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/maintenance/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/maintenance/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .exceptionHandling(e -> e
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setContentType("application/json;charset=UTF-8");
                    response.setStatus(401);
                    response.getWriter().write("{\"message\": \"未登录或登录已过期\"}");
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setContentType("application/json;charset=UTF-8");
                    response.setStatus(403);
                    response.getWriter().write("{\"message\": \"权限不足，无法执行此操作\"}");
                })
            );
        return http.build();
    }
}
```

**2. 使用方法级 `@PreAuthorize` 进行双重防护（参见漏洞1修复代码中的 Controller）**

Controller 中的关键注解：

```java
@PutMapping("/{id}")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<MaintenanceRecord> update(...) { ... }

@DeleteMapping("/{id}")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<Void> delete(...) { ... }
```

**3. 创建记录时，强制使用当前认证用户作为技术员（防止冒充）**

```java
@PostMapping
public ResponseEntity<MaintenanceRecord> create(
        @Valid @RequestBody MaintenanceRecordCreateRequest request,
        @AuthenticationPrincipal UserDetails currentUser) {

    MaintenanceRecord record = new MaintenanceRecord();
    record.setEquipmentId(request.getEquipmentId());
    record.setTechnician(currentUser.getUsername());
    record.setDescription(request.getDescription());
    record.setMaintenanceDate(LocalDateTime.now());
    record.setStatus("PENDING");

    MaintenanceRecord saved = repository.save(record);
    return ResponseEntity.status(HttpStatus.CREATED).body(saved);
}
```

> 注意：`record.setTechnician(currentUser.getUsername())` 而非 `request.getTechnician()`，从安全令牌中获取身份，防止请求体伪造。

### 验证方法

1. **角色隔离验证：** 使用 `ROLE_TECHNICIAN` 角色的令牌调用 `PUT /api/maintenance/1` 和 `DELETE /api/maintenance/1`，确认返回 403 Forbidden
2. **合法操作验证：** 使用 `ROLE_TECHNICIAN` 角色调用 `GET` 和 `POST`，确认返回 200/201；使用 `ROLE_ADMIN` 角色调用所有接口，确认全部成功
3. **冒充防护验证：** 在 POST 请求体中设置 `technician` 为其他用户名，确认返回记录中的 `technician` 字段为当前登录用户名

---

## 漏洞3：审计追踪缺失

### 问题描述

当前系统没有任何操作审计日志。根据业务规则，系统需要记录"谁在什么时间创建/修改了哪条记录"，但现有代码中：
- 创建记录时没有记录操作人和操作时间
- 修改和删除操作完全没有审计记录
- 无法追溯数据变更历史
- 无法满足合规审计要求

**核心危害：**
- 数据被篡改后无法追溯责任人和变更历史
- 无法满足行业合规审计要求
- 内部人员恶意操作无法被追踪
- 发生安全事件后无法进行取证分析

### 攻击场景

管理员恶意修改某条维护记录的描述内容，将"设备未检修"改为"设备已检修"。由于没有审计日志，后续调查时无法证明该记录被篡改过，也无法确定是谁在什么时间做了什么修改。

### 修复代码

**1. 创建审计日志实体**

```java
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "maintenance_audit_log")
public class MaintenanceAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "record_id", nullable = false)
    private Long recordId;

    @Column(nullable = false, length = 20)
    private String action;

    @Column(nullable = false)
    private String operator;

    @Column(name = "operated_at", nullable = false, updatable = false)
    private LocalDateTime operatedAt;

    private String detail;

    @Column(name = "before_snapshot", columnDefinition = "TEXT")
    private String beforeSnapshot;

    @Column(name = "after_snapshot", columnDefinition = "TEXT")
    private String afterSnapshot;

    @PrePersist
    protected void onCreate() {
        if (operatedAt == null) {
            operatedAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getRecordId() { return recordId; }
    public void setRecordId(Long recordId) { this.recordId = recordId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public LocalDateTime getOperatedAt() { return operatedAt; }
    public void setOperatedAt(LocalDateTime operatedAt) { this.operatedAt = operatedAt; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public String getBeforeSnapshot() { return beforeSnapshot; }
    public void setBeforeSnapshot(String beforeSnapshot) { this.beforeSnapshot = beforeSnapshot; }
    public String getAfterSnapshot() { return afterSnapshot; }
    public void setAfterSnapshot(String afterSnapshot) { this.afterSnapshot = afterSnapshot; }
}
```

**2. 创建审计日志 Repository**

```java
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MaintenanceAuditLogRepository extends JpaRepository<MaintenanceAuditLog, Long> {
    List<MaintenanceAuditLog> findByRecordIdOrderByOperatedAtDesc(Long recordId);
    List<MaintenanceAuditLog> findByOperatorOrderByOperatedAtDesc(String operator);
}
```

**3. 使用 AOP 实现声明式审计（推荐，避免业务代码与审计代码耦合）**

```java
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Aspect
@Component
public class MaintenanceAuditAspect {

    private final MaintenanceAuditLogRepository auditLogRepository;
    private final MaintenanceRecordRepository recordRepository;
    private final ObjectMapper objectMapper;

    public MaintenanceAuditAspect(MaintenanceAuditLogRepository auditLogRepository,
                                   MaintenanceRecordRepository recordRepository,
                                   ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.recordRepository = recordRepository;
        this.objectMapper = objectMapper;
    }

    @Around("execution(* com.mems.controller.MaintenanceController.create(..))")
    @Transactional
    public Object auditCreate(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = joinPoint.proceed();

        if (result instanceof ResponseEntity<?> responseEntity && responseEntity.getBody() instanceof MaintenanceRecord saved) {
            MaintenanceAuditLog log = new MaintenanceAuditLog();
            log.setRecordId(saved.getId());
            log.setAction("CREATE");
            log.setOperator(getCurrentUser());
            log.setDetail("创建维护记录");
            log.setAfterSnapshot(toJson(saved));
            auditLogRepository.save(log);
        }

        return result;
    }

    @Around("execution(* com.mems.controller.MaintenanceController.update(..))")
    @Transactional
    public Object auditUpdate(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        Long id = (Long) args[0];

        String beforeSnapshot = recordRepository.findById(id)
                .map(this::toJson)
                .orElse(null);

        Object result = joinPoint.proceed();

        if (result instanceof ResponseEntity<?> responseEntity && responseEntity.getBody() instanceof MaintenanceRecord saved) {
            MaintenanceAuditLog log = new MaintenanceAuditLog();
            log.setRecordId(saved.getId());
            log.setAction("UPDATE");
            log.setOperator(getCurrentUser());
            log.setDetail("修改维护记录");
            log.setBeforeSnapshot(beforeSnapshot);
            log.setAfterSnapshot(toJson(saved));
            auditLogRepository.save(log);
        }

        return result;
    }

    @Around("execution(* com.mems.controller.MaintenanceController.delete(..))")
    @Transactional
    public Object auditDelete(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        Long id = (Long) args[0];

        String beforeSnapshot = recordRepository.findById(id)
                .map(this::toJson)
                .orElse(null);

        Object result = joinPoint.proceed();

        MaintenanceAuditLog log = new MaintenanceAuditLog();
        log.setRecordId(id);
        log.setAction("DELETE");
        log.setOperator(getCurrentUser());
        log.setDetail("删除维护记录");
        log.setBeforeSnapshot(beforeSnapshot);
        auditLogRepository.save(log);

        return result;
    }

    private String getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "SYSTEM";
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }
}
```

**4. 审计日志查询接口（仅管理员可访问）**

```java
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/maintenance-audit")
@PreAuthorize("hasRole('ADMIN')")
public class MaintenanceAuditController {

    private final MaintenanceAuditLogRepository auditLogRepository;

    public MaintenanceAuditController(MaintenanceAuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping("/record/{recordId}")
    public ResponseEntity<List<MaintenanceAuditLog>> getByRecordId(@PathVariable Long recordId) {
        return ResponseEntity.ok(auditLogRepository.findByRecordIdOrderByOperatedAtDesc(recordId));
    }

    @GetMapping("/operator/{operator}")
    public ResponseEntity<List<MaintenanceAuditLog>> getByOperator(@PathVariable String operator) {
        return ResponseEntity.ok(auditLogRepository.findByOperatorOrderByOperatedAtDesc(operator));
    }
}
```

### 验证方法

1. **创建审计验证：** 调用 POST 创建一条维护记录，然后以管理员身份调用 `GET /api/maintenance-audit/record/{id}`，确认存在 `action=CREATE` 的审计记录，且 `operator` 和 `operatedAt` 正确
2. **修改审计验证：** 以管理员身份修改记录后，查询审计日志，确认存在 `action=UPDATE` 的记录，且 `beforeSnapshot` 和 `afterSnapshot` 正确记录了变更前后的数据
3. **删除审计验证：** 删除记录后，确认审计日志中仍保留 `action=DELETE` 的记录（审计日志不受主记录删除影响）
4. **防篡改验证：** 确认审计日志表没有 UPDATE 和 DELETE 权限（通过数据库层面限制），审计记录只能追加不能修改

---

## 漏洞关联性与修复优先级

| 优先级 | 漏洞 | 影响范围 | 修复复杂度 |
|--------|------|----------|------------|
| P0 | Mass Assignment | 数据完整性被破坏，可直接覆盖任意记录 | 中 |
| P0 | 权限粒度控制不足 | 任何认证用户可执行所有操作 | 低 |
| P1 | 审计追踪缺失 | 无法追溯操作，不满足合规要求 | 中高 |

**三个漏洞的协同效应：** 攻击者可以先用低权限技术员账户（漏洞2），通过 Mass Assignment（漏洞1）篡改关键数据，而由于没有审计追踪（漏洞3），事后完全无法发现和追责。三个漏洞组合使用时，风险远大于单个漏洞的简单叠加。

## 数据库层面的补充防护建议

```sql
-- 审计日志表设置为只追加（Appender-Only）
REVOKE UPDATE, DELETE ON maintenance_audit_log FROM PUBLIC;

-- maintenance_date 列设置不可更新
ALTER TABLE maintenance_record ADD COLUMN version BIGINT DEFAULT 0;
```
