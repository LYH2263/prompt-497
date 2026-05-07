package com.mems.controller;

import com.mems.entity.User;
import com.mems.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SecurityContextRepository securityContextRepository;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginRequest, HttpServletRequest request, HttpServletResponse response) {
        String username = loginRequest.get("username");
        String password = loginRequest.get("password");

        Map<String, Object> errorResponse = new HashMap<>();

        // 简单明文密码验证
        return userRepository.findByUsername(username)
            .filter(user -> user.getPassword().equals(password))
            .map(user -> {
                // 手动创建 Authentication 对象并设置到 SecurityContext 中
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                    user.getUsername(),
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
                );
                
                SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
                securityContext.setAuthentication(authentication);
                SecurityContextHolder.setContext(securityContext);
                                
                // 使用 SecurityContextRepository 保存 Context
                securityContextRepository.saveContext(securityContext, request, response);
                
                Map<String, Object> responseData = new HashMap<>();
                responseData.put("success", true);
                responseData.put("message", "登录成功");
                responseData.put("username", user.getUsername());
                responseData.put("role", user.getRole());
                return ResponseEntity.ok(responseData);
            })
            .orElseGet(() -> {
                errorResponse.put("success", false);
                errorResponse.put("message", "用户名或密码错误");
                return ResponseEntity.status(401).body(errorResponse);
            });
    }
}
