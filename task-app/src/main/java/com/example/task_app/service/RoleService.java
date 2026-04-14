package com.example.task_app.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.task_app.dto.RoleDto;
import com.example.task_app.mapper.RoleMapper;
import com.example.task_app.mapper.TaskMapper;
import com.example.task_app.model.Role;
import com.example.task_app.response.RoleResponse;
import com.example.task_app.response.TaskResponse;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
public class RoleService {

    private final RoleMapper roleMapper;
    private final TaskMapper taskMapper;

    @Autowired
    public RoleService(RoleMapper roleMapper, TaskMapper taskMapper) {
        this.roleMapper = roleMapper;
        this.taskMapper = taskMapper;
    }

    public void createRole(RoleDto roleDto) {
        Role role = new Role();
        role.setRoleId(roleDto.getRoleId());
        role.setRoleName(roleDto.getRoleName());

        if (role.getRoleId() != null) {
            roleMapper.update(role);
        } else {
            roleMapper.insert(role);
        }
    }

    public List<RoleResponse> getAllRoles() {
        List<Role> roles = roleMapper.selectAll();
        return roles.stream()
                .map(role -> {
                    List<TaskResponse> tasks = taskMapper.findByRoleId(role.getRoleId())
                            .stream()
                            .map(t -> new TaskResponse(t.getTaskId(), t.getRoleId(), t.getTitle(), t.isPermanent()))
                            .collect(Collectors.toList());
                    return new RoleResponse(role.getRoleId(), role.getRoleName(), role.getIsExpanded(), tasks);
                })
                .collect(Collectors.toList());
    }

    public RoleResponse updateRole(Integer roleId, String roleName, Boolean isExpanded) {
        Role existingRole = roleMapper.selectById(roleId);
        if (existingRole == null) {
            throw new NoSuchElementException("Role not found: " + roleId);
        }
        existingRole.setRoleName(roleName);
        if (isExpanded != null) {
            existingRole.setIsExpanded(isExpanded);
        }
        roleMapper.updateWithExpanded(existingRole);

        List<TaskResponse> tasks = taskMapper.findByRoleId(roleId)
                .stream()
                .map(t -> new TaskResponse(t.getTaskId(), t.getRoleId(), t.getTitle(), t.isPermanent()))
                .collect(Collectors.toList());
        return new RoleResponse(existingRole.getRoleId(), existingRole.getRoleName(), existingRole.getIsExpanded(), tasks);
    }

    public void deleteRole(Integer roleId) {
        Role existingRole = roleMapper.selectById(roleId);
        if (existingRole == null) {
            throw new NoSuchElementException("Role not found: " + roleId);
        }
        roleMapper.deleteById(roleId);
    }
}
