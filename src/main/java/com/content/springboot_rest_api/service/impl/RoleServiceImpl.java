package com.content.springboot_rest_api.service.impl;

import com.content.springboot_rest_api.dto.RoleDto;
import com.content.springboot_rest_api.dto.UserSummaryDto;
import com.content.springboot_rest_api.entity.Role;
import com.content.springboot_rest_api.entity.User;
import com.content.springboot_rest_api.exception.GlobalAPIException;
import com.content.springboot_rest_api.repository.RoleRepository;
import com.content.springboot_rest_api.repository.UserRepository;
import com.content.springboot_rest_api.service.RoleService;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    private RoleDto convertToDto(Role role) {
        RoleDto dto = new RoleDto();
        dto.setId(role.getId());
        dto.setName(role.getName());

        Set<UserSummaryDto> userDtos = role.getUsers()
                .stream()
                .map(user -> new UserSummaryDto(
                        user.getId(),
                        user.getUsername(),
                        user.getEmail(),
                        user.getFullName()
                ))
                .collect(Collectors.toSet());

        dto.setUsers(userDtos);
        return dto;
    }

    @Override
    public RoleDto createRole(RoleDto roleDto) {
        Role role = new Role();
        role.setName(roleDto.getName());
        Role saved = roleRepository.save(role);
        return convertToDto(saved);
    }

    @Override
    public RoleDto updateRole(Long id, RoleDto roleDto) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new GlobalAPIException(HttpStatus.NOT_FOUND, "Role not found"));

        role.setName(roleDto.getName());
        Role updated = roleRepository.save(role);

        return convertToDto(updated);
    }

    @Override
    public void deleteRole(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new GlobalAPIException(HttpStatus.NOT_FOUND, "Role not found"));
        roleRepository.delete(role);
    }

    @Override
    public RoleDto getRoleById(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new GlobalAPIException(HttpStatus.NOT_FOUND, "Role not found"));
        return convertToDto(role);
    }

    @Override
    public List<RoleDto> getAllRoles() {
        return roleRepository.findAll()
                .stream()
                .map(this::convertToDto)
                .toList();
    }

    @Override
    public void assignRoleToUser(Long userId, Long roleId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GlobalAPIException(HttpStatus.NOT_FOUND, "User not found"));
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new GlobalAPIException(HttpStatus.NOT_FOUND, "Role not found"));

        user.getRoles().add(role);
        userRepository.save(user);
    }

    @Override
    public void removeRoleFromUser(Long userId, Long roleId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GlobalAPIException(HttpStatus.NOT_FOUND, "User not found"));
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new GlobalAPIException(HttpStatus.NOT_FOUND, "Role not found"));

        user.getRoles().remove(role);
        userRepository.save(user);
    }
}
