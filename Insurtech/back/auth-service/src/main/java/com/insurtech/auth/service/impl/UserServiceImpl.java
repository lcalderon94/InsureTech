package com.insurtech.auth.service.impl;

import com.insurtech.auth.model.dto.UserDto;
import com.insurtech.auth.model.entity.Role;
import com.insurtech.auth.model.entity.User;
import com.insurtech.auth.repository.RoleRepository;
import com.insurtech.auth.repository.UserRepository;
import com.insurtech.auth.service.UserService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import org.slf4j.Logger;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService, UserDetailsService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);



    @Override
    @Transactional
    public User createUser(UserDto userDto) {
        // Validaciones
        if (existsByUsername(userDto.getUsername()) || existsByEmail(userDto.getEmail())) {
            throw new IllegalArgumentException("El nombre de usuario o email ya existe");
        }

        // Obtener el siguiente ID de secuencia
        Long nextId = userRepository.getNextUserId();

        // Crear usuario
        User user = new User();
        user.setId(nextId);
        user.setUsername(userDto.getUsername());
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));
        user.setEmail(userDto.getEmail());
        user.setFirstName(userDto.getFirstName());
        user.setLastName(userDto.getLastName());

        // Procesar roles desde el DTO
        Set<Role> userRoles = new HashSet<>();

        if (userDto.getRoles() != null && !userDto.getRoles().isEmpty()) {
            // Buscar roles por nombre y agregarlos
            for (String roleName : userDto.getRoles()) {
                roleRepository.findByName(roleName)
                        .ifPresent(userRoles::add);
            }
        }

        // Si no se encontró ningún rol válido, usar rol predeterminado
        if (userRoles.isEmpty()) {
            Role defaultRole = roleRepository.findById(2L)
                    .orElseThrow(() -> new RuntimeException("Rol predeterminado no encontrado"));
            userRoles.add(defaultRole);
        }

        user.setRoles(userRoles);

        return userRepository.save(user);
    }

    @Override
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    @Transactional
    public User updateUser(Long id, UserDto userDto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (userDto.getEmail() != null &&
                !user.getEmail().equals(userDto.getEmail()) &&
                existsByEmail(userDto.getEmail())) {
            throw new IllegalArgumentException("El email ya está registrado");
        }

        if (userDto.getUsername() != null) user.setUsername(userDto.getUsername());
        if (userDto.getEmail() != null) user.setEmail(userDto.getEmail());
        if (userDto.getFirstName() != null) user.setFirstName(userDto.getFirstName());
        if (userDto.getLastName() != null) user.setLastName(userDto.getLastName());

        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));
    }
}