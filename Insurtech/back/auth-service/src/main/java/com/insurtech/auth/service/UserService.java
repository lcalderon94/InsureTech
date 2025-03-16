package com.insurtech.auth.service;

import com.insurtech.auth.model.dto.UserDto;
import com.insurtech.auth.model.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserService {
    User createUser(UserDto userDto);
    Optional<User> getUserById(Long id);
    Optional<User> getUserByUsername(String username);
    List<User> getAllUsers();
    User updateUser(Long id, UserDto userDto);
    void deleteUser(Long id);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}