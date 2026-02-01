package com.example.task_app.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.task_app.dto.RoleDto;
import com.example.task_app.mapper.RoleMapper;
import com.example.task_app.model.Role;

@Service
public class RoleService {

    private final RoleMapper roleMapper;

    @Autowired
    public RoleService(RoleMapper roleMapper) {
        this.roleMapper = roleMapper;
    }

    public void createRole(RoleDto roleDto) {
        Role role = new Role();
        role.setRoleId(roleDto.getRoleId());
        role.setRoleName(roleDto.getRoleName());

        if (role.getRoleId() != null) {
            // IDが存在する場合は更新
            roleMapper.update(role);
        } else {
            // IDが存在しない場合は新規登録
            roleMapper.insert(role);
        }
    }
}