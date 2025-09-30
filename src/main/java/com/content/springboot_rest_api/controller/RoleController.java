package com.content.springboot_rest_api.controller;

import com.content.springboot_rest_api.dto.CommentDto;
import com.content.springboot_rest_api.dto.RoleDto;
import com.content.springboot_rest_api.dto.TagDto;
import com.content.springboot_rest_api.exception.ErrorDetails;
import com.content.springboot_rest_api.exception.GlobalAPIException;
import com.content.springboot_rest_api.service.RoleService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("api/categories")
@AllArgsConstructor
@Slf4j //-- tambah logger untuk tangkap error
public class RoleController {

    private final RoleService roleService;

    @PostMapping
    public ResponseEntity<?> createRole(@RequestBody RoleDto roleDto) {
        try {
            RoleDto saved = roleService.createRole(roleDto);
            return new ResponseEntity<>(saved, HttpStatus.CREATED);
        } catch (GlobalAPIException apiEx) {
            log.error("Error while creating Role", apiEx);
            return buildErrorResponse(apiEx.getMessage(), "Business error", apiEx.getStatus());
        } catch (Exception e) {
            log.error("Unexpected error while creating role", e);
            return buildErrorResponse("Unexpected error occurred", e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllRoles() {
        try {
            List<RoleDto> roleDto = roleService.getAllRoles();
            return ResponseEntity.ok(roleDto);
        } catch (GlobalAPIException apiEx) {
            log.error("Error while fetching all roles", apiEx);
            return buildErrorResponse(apiEx.getMessage(), "Custom business error", apiEx.getStatus());
        } catch (Exception e) {
            log.error("Unexpected error while fetching all roles", e);
            return buildErrorResponse("Unexpected error occurred", e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("{id}")
    public ResponseEntity<?> updateRole(
            @PathVariable("id") Long id,
            @RequestBody RoleDto roleDto
    ) {
        try {
            RoleDto updated = roleService.updateRole(id, roleDto);
            return ResponseEntity.ok(updated);
        } catch (GlobalAPIException apiEx) {
            log.error("Error while updating role {}", id, apiEx);
            return buildErrorResponse(apiEx.getMessage(), "Business error", apiEx.getStatus());
        } catch (Exception e) {
            log.error("Unexpected error while updating role {}", id, e);
            return buildErrorResponse("Unexpected error occurred", e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("{id}")
    public ResponseEntity<?> deleteRole(@PathVariable("id") Long id) {
        try {
            roleService.deleteRole(id);
            return ResponseEntity.ok("Role deleted successfully!");
        } catch (GlobalAPIException apiEx) {
            log.error("Error while deleting role {}", id, apiEx);
            return buildErrorResponse(apiEx.getMessage(), "Business error", apiEx.getStatus());
        } catch (Exception e) {
            log.error("Unexpected error while deleting role {}", id, e);
            return buildErrorResponse("Unexpected error occurred", e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/{roleId}/assign/{userId}")
    public ResponseEntity<?> assignRoleToUser(@PathVariable Long roleId, @PathVariable Long userId) {
        try {
            roleService.assignRoleToUser(userId, roleId);
            return ResponseEntity.ok("Role assign successfully!");
        } catch (GlobalAPIException apiEx) {
            log.error("Error while assign role", apiEx);
            return buildErrorResponse(apiEx.getMessage(), "Business error", apiEx.getStatus());
        } catch (Exception e) {
            log.error("Unexpected error while assign role", e);
            return buildErrorResponse("Unexpected error occurred", e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/{roleId}/remove/{userId}")
    public ResponseEntity<?> removeRoleFromUser(@PathVariable Long roleId, @PathVariable Long userId) {
        try {
            roleService.removeRoleFromUser(userId, roleId);
            return ResponseEntity.ok("Role remove successfully!");
        } catch (GlobalAPIException apiEx) {
            log.error("Error while remove role", apiEx);
            return buildErrorResponse(apiEx.getMessage(), "Business error", apiEx.getStatus());
        } catch (Exception e) {
            log.error("Unexpected error while remove role", e);
            return buildErrorResponse("Unexpected error occurred", e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("{id}")
    public ResponseEntity<?> getRoleById(@PathVariable("id") Long id) {
        try {
            RoleDto dto = roleService.getRoleById(id);
            return ResponseEntity.ok(dto);
        } catch (GlobalAPIException apiEx) {
            log.error("Error while fetching role {}", id, apiEx);
            return buildErrorResponse(apiEx.getMessage(), "Business error", apiEx.getStatus());
        } catch (Exception e) {
            log.error("Unexpected error while fetching role {}", id, e);
            return buildErrorResponse("Unexpected error occurred", e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    //  Helper method biar engga copy-paste error response
    private ResponseEntity<ErrorDetails> buildErrorResponse(String message, String details, HttpStatus status) {
        ErrorDetails errorDetails = new ErrorDetails(
                LocalDateTime.now(),
                message,
                details
        );
        return ResponseEntity.status(status).body(errorDetails);
    }
}
