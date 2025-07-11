package com.Loqal.UserService.controller;

import com.Loqal.UserService.entity.Address;
import com.Loqal.UserService.entity.User;
import com.Loqal.UserService.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {
    @Autowired
    private UserService userService;

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        return ResponseEntity.ok(userService.createUser(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable String id) {
        User user = userService.getUserById(id);
        return user != null ? ResponseEntity.ok(user) : ResponseEntity.notFound().build();
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PutMapping("/{id}/roles")
    public ResponseEntity<User> updateRoles(@PathVariable String id, @RequestBody List<String> roles) {
        return ResponseEntity.ok(userService.updateRoles(id, roles));
    }

    @PutMapping("/{id}/addresses")
    public ResponseEntity<User> addAddress(@PathVariable String id, @RequestBody Address address) {
        return ResponseEntity.ok(userService.addAddress(id, address));
    }
}

