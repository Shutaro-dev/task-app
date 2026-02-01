package com.example.task_app.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class TaskResponse {
    private Integer taskId;
    private Integer roleId;
    private String title;
    private boolean isPermanent;
}