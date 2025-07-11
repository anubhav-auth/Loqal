package com.Loqal.UserService.services;

import com.Loqal.UserService.entity.Address;
import com.Loqal.UserService.entity.User;
import com.Loqal.UserService.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

public class UserService {
    @Autowired
    private UserRepository userRepository;

    public User createUser(User user) {
        return userRepository.save(user);
    }

    public User getUserById(String id) {
        return userRepository.findById(id).orElse(null);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User updateRoles(String userId, List<String> newRoles) {
        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            user.setRoles(newRoles);
            return userRepository.save(user);
        }
        return null;
    }

    public User addAddress(String userId, Address address) {
        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            if (user.getSavedAddresses() == null) user.setSavedAddresses(new ArrayList<>());
            user.getSavedAddresses();
            return userRepository.save(user);
        }
        return null;
    }


}
