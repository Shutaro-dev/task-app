package com.example.task_app.form;

import lombok.Getter;
import jakarta.validation.constraints.NotBlank;

@Getter
public class RoleForm {
    private Integer roleId;
    @NotBlank
    private String roleName;
    private String color;
}