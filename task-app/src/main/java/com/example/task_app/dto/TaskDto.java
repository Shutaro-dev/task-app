package com.example.task_app.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TaskDto {
    private Integer taskId;
    private Integer roleId;
    private String title;
    private boolean isPermanent;
}
