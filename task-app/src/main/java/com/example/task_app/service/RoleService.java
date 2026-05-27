package com.example.task_app.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.task_app.dto.RoleDto;
import com.example.task_app.form.ReorderItem;
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
        role.setColor(roleDto.getColor());

        if (role.getRoleId() != null) {
            if (roleMapper.selectById(role.getRoleId()) == null) {
                throw new NoSuchElementException("Role not found: " + role.getRoleId());
            }
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
                    return new RoleResponse(role.getRoleId(), role.getRoleName(), role.getIsExpanded(), role.getColor(), tasks);
                })
                .collect(Collectors.toList());
    }

    public RoleResponse updateRole(Integer roleId, String roleName, Boolean isExpanded, String color) {
        Role existingRole = roleMapper.selectById(roleId);
        if (existingRole == null) {
            throw new NoSuchElementException("Role not found: " + roleId);
        }
        existingRole.setRoleName(roleName);
        if (isExpanded != null) {
            existingRole.setIsExpanded(isExpanded);
        }
        if (color != null) {
            existingRole.setColor(color);
        }
        roleMapper.updateWithExpanded(existingRole);

        List<TaskResponse> tasks = taskMapper.findByRoleId(roleId)
                .stream()
                .map(t -> new TaskResponse(t.getTaskId(), t.getRoleId(), t.getTitle(), t.isPermanent()))
                .collect(Collectors.toList());
        return new RoleResponse(existingRole.getRoleId(), existingRole.getRoleName(), existingRole.getIsExpanded(), existingRole.getColor(), tasks);
    }

    public void reorderRoles(List<ReorderItem> items) {
        items.forEach(item -> roleMapper.updateSortOrder(item.getId(), item.getSortOrder()));
    }

    public void deleteRole(Integer roleId) {
        Role existingRole = roleMapper.selectById(roleId);
        if (existingRole == null) {
            throw new NoSuchElementException("Role not found: " + roleId);
        }
        roleMapper.deleteById(roleId);
    }
}
