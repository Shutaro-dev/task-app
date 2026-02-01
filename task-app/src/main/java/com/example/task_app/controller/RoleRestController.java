package com.example.task_app.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import com.example.task_app.form.RoleForm;
import com.example.task_app.service.RoleService;
import com.example.task_app.dto.RoleDto;

@RestController
@RequestMapping("/api/roles")
public class RoleRestController {

    private final RoleService roleService;

    @Autowired
    public RoleRestController(RoleService roleService) {
        this.roleService = roleService;
    }

    @PostMapping
    public ResponseEntity<Void> createRole(@RequestBody @Valid RoleForm roleForm) {
        RoleDto roleDto = new RoleDto(roleForm.getRoleId(), roleForm.getRoleName());
        roleService.createRole(roleDto);
        return ResponseEntity.ok().build();
    }
}
