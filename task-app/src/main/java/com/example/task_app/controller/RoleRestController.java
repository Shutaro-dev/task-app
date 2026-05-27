package com.example.task_app.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import jakarta.validation.Valid;
import com.example.task_app.form.ReorderItem;
import com.example.task_app.form.RoleForm;
import com.example.task_app.form.RoleUpdateForm;
import com.example.task_app.service.RoleService;
import com.example.task_app.dto.RoleDto;
import com.example.task_app.response.RoleResponse;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/roles")
public class RoleRestController {

    private final RoleService roleService;

    @Autowired
    public RoleRestController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    public ResponseEntity<List<RoleResponse>> getAllRoles() {
        return ResponseEntity.ok(roleService.getAllRoles());
    }

    @PostMapping
    public ResponseEntity<Void> createRole(@RequestBody @Valid RoleForm roleForm) {
        RoleDto roleDto = new RoleDto(roleForm.getRoleId(), roleForm.getRoleName(), roleForm.getColor());
        try {
            roleService.createRole(roleDto);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/reorder")
    public ResponseEntity<Void> reorderRoles(@RequestBody List<ReorderItem> items) {
        roleService.reorderRoles(items);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<RoleResponse> updateRole(
            @PathVariable Integer id,
            @RequestBody @Valid RoleUpdateForm form) {
        try {
            RoleResponse response = roleService.updateRole(id, form.getRoleName(), form.getIsExpanded(), form.getColor());
            return ResponseEntity.ok(response);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRole(@PathVariable Integer id) {
        try {
            roleService.deleteRole(id);
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }
}
