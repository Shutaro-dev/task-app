package com.example.task_app.model;

import lombok.Data;

@Data
public class Role {
    private Integer roleId;
    private String roleName;
    private Boolean isExpanded;
    private String color;
    private Integer sortOrder;
}