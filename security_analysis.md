# MEMS 系统安全漏洞分析报告

## 漏洞1：Mass Assignment 攻击（批量赋值漏洞）

### 问题描述

`MaintenanceController` 直接将请求体反序列化为 `MaintenanceRecord` 实体对象并直接保存，这导致了典型的 **Mass Assignment** 漏洞。攻击者可以通过在请求中包含额外的字段来绕过业务规则，特别是：

1. **`maintenanceDate` 字段保护失效**：业务规则要求维护记录一旦创建后，`maintenanceDate` 不应被篡改。但当前代码没有任何字段级别的验证或保护机制。
2. **实体直接暴露给请求**：使用 `@RequestBody MaintenanceRecord record` 并配合 Lombok 的 `@Data` 注解，意味着请求体中的所有字段都会被绑定到实体对象。
3. **缺少 PUT/PATCH 端点但更严重**：当前代码虽然只实现了 POST，但如果后续添加了更新接口，同样的问题会导致攻击者可以修改 `maintenanceDate`。

**危害**：
- 攻击者可以在创建记录时设置任意的 `maintenanceDate`，绕过时间戳的真实性
- 攻击者可以设置敏感字段值（如 `id`、`status` 等）
- 破坏数据完整性和审计可信度

### 攻击场景

**场景1：创建时设置虚假日期**

攻击者发送以下请求：

```http
POST /api/maintenance
Content-Type: application/json

{
  "equipmentId": 1,
  "technician": "张三",
  "description": "设备维护",
  "maintenanceDate": "2024-01-01T00:00:00",
  "status": "已完成"
}
```

攻击结果：
- 系统接受虚假的 `maintenanceDate`
- 维护记录的实际执行时间可以被任意伪造
- 无法追溯真实的操作时间

**场景2：设置不应该由用户控制的字段**

```http
POST /api/maintenance
Content-Type: application/json

{
  "id": 999,
  "equipmentId": 1,
  "technician": "张三",
  "description": "设备维护",
  "status": "已完成"
}
```

攻击结果：
- 虽然 `@GeneratedValue` 会覆盖 `id`，但这种模式本身就是不安全的
- 暴露了内部数据库结构

### 修复代码

**步骤1：创建 DTO 类隔离请求与实体**

```java
package com.mems.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MaintenanceRecordCreateRequest {
    
    @NotNull(message = "设备ID不能为空")
    private Long equipmentId;
    
    @NotBlank(message = "技术员姓名不能为空")
    private String technician;
    
    @NotBlank(message = "描述不能为空")
    private String description;
    
    // 注意：不包含 maintenanceDate 字段，由服务端自动设置
    // 注意：不包含 id 字段，由数据库自动生成
    // 注意：不包含 status 字段，由系统初始化设置
}

@Data
public class MaintenanceRecordUpdateRequest {
    
    @NotBlank(message = "技术员姓名不能为空")
    private String technician;
    
    @NotBlank(message = "描述不能为空")
    private String description;
    
    private String status;
    
    // 注意：不包含 maintenanceDate 字段，该字段创建后不可修改
}
```

**步骤2：使用 `@Column(updatable = false)` 保护字段**

```java
package com.mems.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "maintenance_record")
public class MaintenanceRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "equipment_id", nullable = false)
    private Long equipmentId;
    
    @Column(nullable = false)
    private String technician;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "maintenance_date", nullable = false, updatable = false)
    private LocalDateTime maintenanceDate;
    
    @Column(nullable = false)
    private String status;
    
    @PrePersist
    protected void onCreate() {
        if (maintenanceDate == null) {
            maintenanceDate = LocalDateTime.now();
        }
        if (status == null) {
            status = "进行中";
        }
    }
}
```

**步骤3：修改 Controller 使用 DTO**

```java
package com.mems.controller;

import com.mems.dto.MaintenanceRecordCreateRequest;
import com.mems.dto.MaintenanceRecordUpdateRequest;
import com.mems.entity.MaintenanceRecord;
import com.mems.repository.MaintenanceRecordRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/maintenance")
public class MaintenanceController {

    @Autowired
    private MaintenanceRecordRepository repository;

    @GetMapping
    public List<MaintenanceRecord> getAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<MaintenanceRecord> getById(@PathVariable Long id) {
        return repository.findById(id)
            .map(ResponseEntity::ok)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "记录不存在"));
    }

    @PostMapping
    public ResponseEntity<MaintenanceRecord> create(@Valid @RequestBody MaintenanceRecordCreateRequest request) {
        MaintenanceRecord record = new MaintenanceRecord();
        record.setEquipmentId(request.getEquipmentId());
        record.setTechnician(request.getTechnician());
        record.setDescription(request.getDescription());
        
        MaintenanceRecord saved = repository.save(record);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MaintenanceRecord> update(
            @PathVariable Long id,
            @Valid @RequestBody MaintenanceRecordUpdateRequest request) {
        
        return repository.findById(id)
            .map(existing -> {
                existing.setTechnician(request.getTechnician());
                existing.setDescription(request.getDescription());
                existing.setStatus(request.getStatus());
                
                MaintenanceRecord saved = repository.save(existing);
                return ResponseEntity.ok(saved);
            })
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "记录不存在"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "记录不存在");
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
```

**步骤4：添加 `@Version` 实现乐观锁（高并发一致性）**

```java
package com.mems.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "maintenance_record")
public class MaintenanceRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Version
    private Long version;
    
    @Column(name = "equipment_id", nullable = false)
    private Long equipmentId;
    
    @Column(nullable = false)
    private String technician;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "maintenance_date", nullable = false, updatable = false)
    private LocalDateTime maintenanceDate;
    
    @Column(nullable = false)
    private String status;
    
    @PrePersist
    protected void onCreate() {
        if (maintenanceDate == null) {
            maintenanceDate = LocalDateTime.now();
        }
        if (status == null) {
            status = "进行中";
        }
    }
}
```

### 验证方法

1. **验证 `maintenanceDate` 不可修改**：
   - 发送 POST 请求创建记录，记录返回的 `maintenanceDate`
   - 尝试发送 PUT 请求，在请求体中包含 `maintenanceDate` 字段
   - 验证更新后的记录的 `maintenanceDate` 保持不变

2. **验证字段隔离**：
   - 发送 POST 请求，在请求体中包含 `id`、`maintenanceDate`、`version` 等字段
   - 验证这些字段被忽略，只有 DTO 中定义的字段被接受

3. **验证乐观锁**：
   - 并发发送两个 PUT 请求修改同一条记录
   - 验证第二个请求返回 409 或抛出 `ObjectOptimisticLockingFailureException`

---

## 漏洞2：权限粒度控制不足（水平越权/垂直越权风险）

### 问题描述

当前 `SecurityConfig` 只实现了最基本的认证（Authentication），完全没有实现基于角色的授权（Authorization）：

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/auth/**", "/api/public/**").permitAll()
    .anyRequest().authenticated()  // 所有已认证用户都可以访问所有API
)
```

**存在的问题**：

1. **没有方法级别的权限控制**：业务规则明确要求：
   - 普通技术员（ROLE_TECHNICIAN）只能查看和创建记录
   - 管理员（ROLE_ADMIN）可以修改和删除记录
   - 但当前代码中，任何已登录用户都可以执行所有操作

2. **角色配置缺失**：虽然 `AuthController` 中设置了 `ROLE_` 前缀的权限，但没有在任何地方使用

3. **缺少 PUT/DELETE 端点保护**：即使后续添加这些端点，也没有权限控制

**危害**：
- 普通技术员可以修改或删除任意维护记录
- 违反最小权限原则
- 无法实现业务规则要求的职责分离

### 攻击场景

**场景1：普通技术员删除记录**

假设存在一个普通技术员用户（ROLE_TECHNICIAN），登录后发送：

```http
DELETE /api/maintenance/1
```

攻击结果：
- 当前代码没有 DELETE 端点，但如果添加了，由于没有权限控制，技术员可以成功删除记录
- 即使只有 GET/POST，未来扩展时也容易引入漏洞

**场景2：普通技术员修改记录**

```http
PUT /api/maintenance/1
Content-Type: application/json

{
  "status": "已完成",
  "description": "被篡改的描述"
}
```

攻击结果：
- 如果有 PUT 端点，技术员可以任意修改维护记录
- 破坏了管理员对记录的最终控制权

### 修复代码

**步骤1：启用方法级安全注解**

```java
package com.mems.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpMethod;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .securityContext(context -> context.securityContextRepository(securityContextRepository()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**", "/api/public/**", "/v3/api-docs/**", "/swagger-ui/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/maintenance/**").hasAnyRole("TECHNICIAN", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/maintenance/**").hasAnyRole("TECHNICIAN", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/maintenance/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/maintenance/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .exceptionHandling(e -> {
                e.authenticationEntryPoint((request, response, authException) -> {
                    response.setContentType("application/json;charset=UTF-8");
                    response.setStatus(401);
                    response.getWriter().write("{\"message\": \"未登录或登录已过期\"}");
                });
                e.accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setContentType("application/json;charset=UTF-8");
                    response.setStatus(403);
                    response.getWriter().write("{\"message\": \"权限不足，禁止访问\"}");
                });
            });

        return http.build();
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
```

**步骤2：在 Controller 中使用方法级权限控制**

```java
package com.mems.controller;

import com.mems.dto.MaintenanceRecordCreateRequest;
import com.mems.dto.MaintenanceRecordUpdateRequest;
import com.mems.entity.MaintenanceRecord;
import com.mems.repository.MaintenanceRecordRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/maintenance")
public class MaintenanceController {

    @Autowired
    private MaintenanceRecordRepository repository;

    @GetMapping
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'ADMIN')")
    public List<MaintenanceRecord> getAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'ADMIN')")
    public ResponseEntity<MaintenanceRecord> getById(@PathVariable Long id) {
        return repository.findById(id)
            .map(ResponseEntity::ok)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "记录不存在"));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'ADMIN')")
    public ResponseEntity<MaintenanceRecord> create(@Valid @RequestBody MaintenanceRecordCreateRequest request) {
        MaintenanceRecord record = new MaintenanceRecord();
        record.setEquipmentId(request.getEquipmentId());
        record.setTechnician(request.getTechnician());
        record.setDescription(request.getDescription());
        
        MaintenanceRecord saved = repository.save(record);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MaintenanceRecord> update(
            @PathVariable Long id,
            @Valid @RequestBody MaintenanceRecordUpdateRequest request) {
        
        return repository.findById(id)
            .map(existing -> {
                existing.setTechnician(request.getTechnician());
                existing.setDescription(request.getDescription());
                existing.setStatus(request.getStatus());
                
                MaintenanceRecord saved = repository.save(existing);
                return ResponseEntity.ok(saved);
            })
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "记录不存在"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "记录不存在");
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
```

**步骤3：创建自定义 `UserDetailsService` 确保权限正确加载**

```java
package com.mems.service;

import com.mems.entity.User;
import com.mems.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("用户不存在: " + username));

        return new org.springframework.security.core.userdetails.User(
            user.getUsername(),
            user.getPassword(),
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
        );
    }
}
```

### 验证方法

1. **验证技术员权限**：
   - 使用 ROLE_TECHNICIAN 用户登录
   - 发送 GET/POST 请求，验证返回 200/201
   - 发送 PUT/DELETE 请求，验证返回 403 禁止访问

2. **验证管理员权限**：
   - 使用 ROLE_ADMIN 用户登录
   - 发送 GET/POST/PUT/DELETE 请求，验证所有操作都成功

3. **双重保险验证**：
   - 临时修改 `SecurityConfig`，移除 URL 级别的权限控制
   - 验证 `@PreAuthorize` 注解仍然有效（方法级权限控制独立生效）

---

## 漏洞3：审计追踪缺失（操作日志记录）

### 问题描述

系统完全没有实现操作审计日志功能。业务规则明确要求：

> 系统需要记录完整的操作审计日志（谁在什么时间创建/修改了哪条记录）

但当前代码中：
- 没有任何审计日志实体
- 没有任何日志记录逻辑
- 无法追溯操作历史
- 无法进行合规性审查

**危害**：
- 数据被篡改后无法追踪责任人
- 无法进行安全事件溯源
- 不符合企业级系统的合规要求
- 无法排查生产问题

### 攻击场景

**场景：恶意篡改数据后无法追溯**

假设某个维护记录被错误修改或删除，由于没有审计日志：

```http
PUT /api/maintenance/1
Content-Type: application/json

{
  "status": "已完成",
  "description": "这个修改是谁做的？"
}
```

**后果**：
- 管理员无法知道是谁修改了记录
- 无法知道修改发生的具体时间
- 无法恢复到修改前的状态
- 无法进行责任认定

### 修复代码

**步骤1：创建审计日志实体**

```java
package com.mems.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "audit_log")
public class AuditLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "entity_type", nullable = false)
    private String entityType;
    
    @Column(name = "entity_id", nullable = false)
    private Long entityId;
    
    @Column(name = "operation_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private OperationType operationType;
    
    @Column(name = "operator", nullable = false)
    private String operator;
    
    @Column(name = "operation_time", nullable = false)
    private LocalDateTime operationTime;
    
    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;
    
    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;
    
    @Column(name = "ip_address")
    private String ipAddress;
    
    @Column(name = "user_agent")
    private String userAgent;
    
    @PrePersist
    protected void onCreate() {
        if (operationTime == null) {
            operationTime = LocalDateTime.now();
        }
    }
    
    public enum OperationType {
        CREATE,
        UPDATE,
        DELETE,
        READ
    }
}
```

**步骤2：创建审计日志 Repository**

```java
package com.mems.repository;

import com.mems.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    
    List<AuditLog> findByEntityTypeAndEntityIdOrderByOperationTimeDesc(String entityType, Long entityId);
    
    List<AuditLog> findByOperatorOrderByOperationTimeDesc(String operator);
    
    List<AuditLog> findByOperationTimeBetweenOrderByOperationTimeDesc(
        LocalDateTime startTime, LocalDateTime endTime);
    
    List<AuditLog> findByEntityTypeOrderByOperationTimeDesc(String entityType);
}
```

**步骤3：创建审计服务**

```java
package com.mems.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mems.entity.AuditLog;
import com.mems.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

@Service
public class AuditService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logCreate(String entityType, Long entityId, Object newValue) {
        saveAuditLog(entityType, entityId, AuditLog.OperationType.CREATE, null, newValue);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logUpdate(String entityType, Long entityId, Object oldValue, Object newValue) {
        saveAuditLog(entityType, entityId, AuditLog.OperationType.UPDATE, oldValue, newValue);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logDelete(String entityType, Long entityId, Object oldValue) {
        saveAuditLog(entityType, entityId, AuditLog.OperationType.DELETE, oldValue, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logRead(String entityType, Long entityId, Object value) {
        saveAuditLog(entityType, entityId, AuditLog.OperationType.READ, null, value);
    }

    private void saveAuditLog(String entityType, Long entityId, AuditLog.OperationType operationType,
                               Object oldValue, Object newValue) {
        AuditLog log = new AuditLog();
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setOperationType(operationType);
        log.setOperator(getCurrentUsername());
        log.setOldValue(toJson(oldValue));
        log.setNewValue(toJson(newValue));
        log.setIpAddress(getClientIp());
        log.setUserAgent(getUserAgent());

        auditLogRepository.save(log);
    }

    private String getCurrentUsername() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
            .map(Authentication::getName)
            .orElse("SYSTEM");
    }

    private String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return obj.toString();
        }
    }

    private String getClientIp() {
        return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
            .filter(ServletRequestAttributes.class::isInstance)
            .map(ServletRequestAttributes.class::cast)
            .map(ServletRequestAttributes::getRequest)
            .map(this::extractClientIp)
            .orElse(null);
    }

    private String extractClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private String getUserAgent() {
        return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
            .filter(ServletRequestAttributes.class::isInstance)
            .map(ServletRequestAttributes.class::cast)
            .map(ServletRequestAttributes::getRequest)
            .map(req -> req.getHeader("User-Agent"))
            .orElse(null);
    }
}
```

**步骤4：修改 Controller 集成审计日志**

```java
package com.mems.controller;

import com.mems.dto.MaintenanceRecordCreateRequest;
import com.mems.dto.MaintenanceRecordUpdateRequest;
import com.mems.entity.MaintenanceRecord;
import com.mems.repository.MaintenanceRecordRepository;
import com.mems.service.AuditService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/maintenance")
public class MaintenanceController {

    @Autowired
    private MaintenanceRecordRepository repository;

    @Autowired
    private AuditService auditService;

    @GetMapping
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'ADMIN')")
    public List<MaintenanceRecord> getAll() {
        List<MaintenanceRecord> records = repository.findAll();
        records.forEach(record -> 
            auditService.logRead("MaintenanceRecord", record.getId(), record)
        );
        return records;
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'ADMIN')")
    public ResponseEntity<MaintenanceRecord> getById(@PathVariable Long id) {
        return repository.findById(id)
            .map(record -> {
                auditService.logRead("MaintenanceRecord", record.getId(), record);
                return ResponseEntity.ok(record);
            })
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "记录不存在"));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'ADMIN')")
    @Transactional
    public ResponseEntity<MaintenanceRecord> create(@Valid @RequestBody MaintenanceRecordCreateRequest request) {
        MaintenanceRecord record = new MaintenanceRecord();
        record.setEquipmentId(request.getEquipmentId());
        record.setTechnician(request.getTechnician());
        record.setDescription(request.getDescription());
        
        MaintenanceRecord saved = repository.save(record);
        auditService.logCreate("MaintenanceRecord", saved.getId(), saved);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<MaintenanceRecord> update(
            @PathVariable Long id,
            @Valid @RequestBody MaintenanceRecordUpdateRequest request) {
        
        return repository.findById(id)
            .map(existing -> {
                MaintenanceRecord oldValue = cloneRecord(existing);
                
                existing.setTechnician(request.getTechnician());
                existing.setDescription(request.getDescription());
                existing.setStatus(request.getStatus());
                
                MaintenanceRecord saved = repository.save(existing);
                auditService.logUpdate("MaintenanceRecord", saved.getId(), oldValue, saved);
                
                return ResponseEntity.ok(saved);
            })
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "记录不存在"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return repository.findById(id)
            .map(existing -> {
                repository.deleteById(id);
                auditService.logDelete("MaintenanceRecord", id, existing);
                return ResponseEntity.noContent().<Void>build();
            })
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "记录不存在"));
    }

    private MaintenanceRecord cloneRecord(MaintenanceRecord source) {
        MaintenanceRecord clone = new MaintenanceRecord();
        clone.setId(source.getId());
        clone.setVersion(source.getVersion());
        clone.setEquipmentId(source.getEquipmentId());
        clone.setTechnician(source.getTechnician());
        clone.setDescription(source.getDescription());
        clone.setMaintenanceDate(source.getMaintenanceDate());
        clone.setStatus(source.getStatus());
        return clone;
    }
}
```

**步骤5：创建审计日志查询接口（供管理员使用）**

```java
package com.mems.controller;

import com.mems.entity.AuditLog;
import com.mems.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @GetMapping("/entity/{entityType}/{entityId}")
    @PreAuthorize("hasRole('ADMIN')")
    public List<AuditLog> getEntityHistory(
            @PathVariable String entityType,
            @PathVariable Long entityId) {
        return auditLogRepository.findByEntityTypeAndEntityIdOrderByOperationTimeDesc(entityType, entityId);
    }

    @GetMapping("/user/{username}")
    @PreAuthorize("hasRole('ADMIN')")
    public List<AuditLog> getUserOperations(@PathVariable String username) {
        return auditLogRepository.findByOperatorOrderByOperationTimeDesc(username);
    }

    @GetMapping("/time-range")
    @PreAuthorize("hasRole('ADMIN')")
    public List<AuditLog> getOperationsByTimeRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return auditLogRepository.findByOperationTimeBetweenOrderByOperationTimeDesc(startTime, endTime);
    }

    @GetMapping("/entity-type/{entityType}")
    @PreAuthorize("hasRole('ADMIN')")
    public List<AuditLog> getByEntityType(@PathVariable String entityType) {
        return auditLogRepository.findByEntityTypeOrderByOperationTimeDesc(entityType);
    }
}
```

**步骤6：创建数据库初始化脚本（如果使用 SQL 初始化）**

```sql
-- audit_log 表
CREATE TABLE IF NOT EXISTS audit_log (
    id BIGSERIAL PRIMARY KEY,
    entity_type VARCHAR(100) NOT NULL,
    entity_id BIGINT NOT NULL,
    operation_type VARCHAR(20) NOT NULL,
    operator VARCHAR(100) NOT NULL,
    operation_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    old_value TEXT,
    new_value TEXT,
    ip_address VARCHAR(100),
    user_agent VARCHAR(500)
);

CREATE INDEX idx_audit_entity ON audit_log(entity_type, entity_id);
CREATE INDEX idx_audit_operator ON audit_log(operator);
CREATE INDEX idx_audit_time ON audit_log(operation_time);
CREATE INDEX idx_audit_type ON audit_log(operation_type);
```

### 验证方法

1. **验证创建日志**：
   - 创建一条维护记录
   - 查询 `audit_log` 表，验证存在一条 `CREATE` 类型的记录
   - 确认 `operator` 字段为当前登录用户
   - 确认 `new_value` 包含记录内容

2. **验证更新日志**：
   - 修改一条维护记录
   - 查询 `audit_log` 表，验证存在一条 `UPDATE` 类型的记录
   - 确认 `old_value` 和 `new_value` 分别记录修改前后的状态

3. **验证删除日志**：
   - 删除一条维护记录
   - 查询 `audit_log` 表，验证存在一条 `DELETE` 类型的记录
   - 确认 `old_value` 记录了被删除的内容

4. **验证独立事务**：
   - 在一个方法中故意抛出异常（在审计日志之后）
   - 验证审计日志仍然被写入（`REQUIRES_NEW` 确保独立提交）

5. **验证权限控制**：
   - 使用普通技术员登录
   - 尝试访问 `/api/audit/**` 接口
   - 验证返回 403 禁止访问

---

## 修复总结

### 修复前后对比

| 问题 | 修复前 | 修复后 |
|------|--------|--------|
| 字段保护 | 所有字段可修改 | DTO 隔离 + `@Column(updatable = false)` |
| 权限控制 | 任意登录用户可操作所有API | URL级 + 方法级双重权限控制 |
| 审计追踪 | 无 | 完整的操作审计日志系统 |
| 并发控制 | 无 | `@Version` 乐观锁 |

### 技术要点总结

1. **Mass Assignment 防护**：
   - 使用 DTO 模式隔离请求与实体
   - `@Column(updatable = false)` 数据库层保护
   - `@PrePersist` 自动设置不可变字段
   - `@Version` 乐观锁防止并发修改冲突

2. **权限控制**：
   - `@EnableMethodSecurity` 启用方法级安全
   - `@PreAuthorize` 注解实现细粒度权限控制
   - URL 级别的 `hasRole` / `hasAnyRole` 配置
   - 自定义 403 处理器提供友好错误信息

3. **审计日志**：
   - 独立的 `AuditLog` 实体和 Repository
   - `@Transactional(propagation = Propagation.REQUIRES_NEW)` 确保日志独立提交
   - 记录操作人、时间、IP、User-Agent、新旧值
   - 提供审计日志查询接口（仅管理员可访问）
