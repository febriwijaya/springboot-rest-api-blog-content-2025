package com.content.springboot_rest_api.service;

import com.content.springboot_rest_api.dto.RoleDto;

import java.util.List;

public interface RoleService {
    RoleDto createRole(RoleDto roleDto);
    RoleDto updateRole(Long id, RoleDto roleDto);
    void deleteRole(Long id);
    RoleDto getRoleById(Long id);
    List<RoleDto> getAllRoles();

    void assignRoleToUser(Long userId, Long roleId);
    void removeRoleFromUser(Long userId, Long roleId);
}
