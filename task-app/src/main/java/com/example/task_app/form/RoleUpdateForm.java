package com.example.task_app.form;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class RoleUpdateForm {
    @NotBlank
    private String roleName;
    private Boolean isExpanded;
}
