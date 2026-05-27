package com.example.task_app.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class RoleResponse {
    private Integer roleId;
    private String roleName;
    private Boolean isExpanded;
    private String color;
    private List<TaskResponse> tasks;
}
