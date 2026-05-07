package com.mems.controller;

import com.mems.entity.User;
import com.mems.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class UserController {

    @Autowired
    private UserRepository userRepository;



    @GetMapping("/users")
    public ResponseEntity<?> getUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    @PostMapping("/users")
    public com.mems.entity.User createUser(@RequestBody com.mems.entity.User user) {
        return userRepository.save(user);
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        userRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
